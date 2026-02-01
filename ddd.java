apiVersion: v1
kind: ConfigMap
metadata:
  name: vault-pg-debug-vault-agent-config
  namespace: ns-wall-e-springboot
data:
  vault-agent-config.hcl: |
    exit_after_auth = false
    pid_file = "/home/vault/pidfile"

    vault {
      address = "http://vault.ns-vault.svc.cluster.local:8200"
    }

    auto_auth {
      method "kubernetes" {
        # ✅ LE BON MOUNT PATH
        mount_path = "auth/kubernetes_kub00001_local"

        config = {
          # ✅ Ton rôle Vault (tu l'appelles "approle" dans values, mais ici c'est bien role=...)
          role = "ns-wall-e-springboot-local-ap11236-java-application-liquibase"
        }
      }

      sink "file" {
        config = {
          path = "/home/vault/.vault-token"
        }
      }
    }

    template {
      destination = "/etc/secrets/pg.env"
      perms       = "0600"
      contents    = <<EOH
{{- with secret "database/postgres/pg0000000/creds/own_pg0000000_ibmclouddb" -}}
export PGUSER="{{ .Data.username }}"
export PGPASSWORD="{{ .Data.password }}"
{{- end -}}
EOH
    }
---
apiVersion: batch/v1
kind: Job
metadata:
  name: vault-pg-debug
  namespace: ns-wall-e-springboot
spec:
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never

      # ⚠️ Mets ici ton ServiceAccount (celui qui est bound au rôle Vault)
      serviceAccountName: local-ap11236-java-application-liquibase

      volumes:
        - name: vault-shared-data
          emptyDir: {}
        - name: vault-config
          configMap:
            name: vault-pg-debug-vault-agent-config
        - name: vault-home
          emptyDir: {}

      initContainers:
        - name: vault-agent
          image: docker-registry-devops.pf.echonet/hashicorp/vault:1.21
          args:
            - "agent"
            - "-config=/etc/vault/vault-agent-config.hcl"
            - "-log-level=info"
          env:
            # Si tu utilises Vault Enterprise namespace (sinon tu peux enlever VAULT_NAMESPACE)
            - name: VAULT_NAMESPACE
              value: "ns-wall-e-springboot"
            - name: VAULT_ADDR
              value: "http://vault.ns-vault.svc.cluster.local:8200"
          volumeMounts:
            - name: vault-config
              mountPath: /etc/vault
              readOnly: true
            - name: vault-shared-data
              mountPath: /etc/secrets
            - name: vault-home
              mountPath: /home/vault

      containers:
        - name: show-pg-creds
          image: busybox:1.36
          command: ["/bin/sh","-lc"]
          args:
            - |
              echo "==== waiting for /etc/secrets/pg.env (rendered by vault-agent) ===="
              for i in $(seq 1 120); do
                if [ -f /etc/secrets/pg.env ]; then
                  break
                fi
                sleep 1
              done

              if [ ! -f /etc/secrets/pg.env ]; then
                echo "ERROR: /etc/secrets/pg.env not created"
                exit 1
              fi

              echo "==== /etc/secrets/pg.env content ===="
              cat /etc/secrets/pg.env
              echo "==== extracted values ===="

              # source le fichier (il contient export PGUSER=... etc)
              . /etc/secrets/pg.env

              echo "PGUSER=$PGUSER"
              echo "PGPASSWORD=$PGPASSWORD"

              echo "==== END ===="
              # laisse le pod vivant pour que tu puisses le voir / exec si besoin
              sleep 3600
          volumeMounts:
            - name: vault-shared-data
              mountPath: /etc/secrets



kubectl apply -f vault-pg-debug.yaml
kubectl logs -n ns-wall-e-springboot job/vault-pg-debug -c show-pg-creds
kubectl logs -n ns-wall-e-springboot job/vault-pg-debug -c vault-agent

