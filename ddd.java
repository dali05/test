apiVersion: v1
kind: ServiceAccount
metadata:
  name: local-ap11236-java-application-liquibase
  namespace: ns-wall-e-springboot
---
apiVersion: batch/v1
kind: Job
metadata:
  name: vault-pg-debug
  namespace: ns-wall-e-springboot
spec:
  backoffLimit: 0
  template:
    metadata:
      labels:
        job-name: vault-pg-debug
    spec:
      serviceAccountName: local-ap11236-java-application-liquibase
      restartPolicy: Never

      volumes:
        - name: vault-shared-data
          emptyDir: {}
        - name: config
          configMap:
            name: wall-e-vault-agent-config
            optional: false

      initContainers:
        - name: vault-agent
          image: docker-registry-devops.pf.echonet/hashicorp/vault:1.21
          imagePullPolicy: IfNotPresent
          args:
            - "agent"
            - "-config=/etc/vault/vault-agent-config.hcl"
            - "-log-level=info"
          env:
            - name: VAULT_ADDR
              value: "http://vault.ns-vault.svc.cluster.local:8200"
            - name: VAULT_NAMESPACE
              value: "ns-wall-e-springboot"
            # ⚠️ Si tu avais mis VAULT_CACERT et que ça cassait (no such file),
            # laisse-le supprimé tant que tu es en http.
          volumeMounts:
            - name: vault-shared-data
              mountPath: /etc/secrets
            - name: config
              mountPath: /etc/vault
              readOnly: true

      containers:
        - name: show-pg-creds
          image: busybox:1.36
          imagePullPolicy: IfNotPresent
          command: ["/bin/sh", "-lc"]
          args:
            - |
              echo "=== waiting for /etc/secrets/pg.env (rendered by vault-agent) ==="
              for i in $(seq 1 120); do
                if [ -f /etc/secrets/pg.env ]; then
                  break
                fi
                sleep 1
              done

              if [ ! -f /etc/secrets/pg.env ]; then
                echo "ERROR: /etc/secrets/pg.env not created"
                echo "Tip: check initContainer logs: vault-agent"
                exit 1
              fi

              echo "=== /etc/secrets/pg.env content (masked) ==="
              # Affiche le fichier mais masque la valeur du mot de passe dans le dump
              sed -E 's/(PGPASSWORD=).*/\1***MASKED***/' /etc/secrets/pg.env || true

              echo "=== extracted values ==="
              # charge les exports
              . /etc/secrets/pg.env

              echo "PGUSER=$PGUSER"
              echo "PGPASSWORD=$PGPASSWORD"

              echo "Sleeping for debug (1h)..."
              sleep 3600
          volumeMounts:
            - name: vault-shared-data
              mountPath: /etc/secrets
