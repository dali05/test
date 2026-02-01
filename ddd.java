apiVersion: batch/v1
kind: Job
metadata:
  name: vault-pg-debug
  namespace: ns-wall-e-springboot
spec:
  backoffLimit: 0
  ttlSecondsAfterFinished: 300
  template:
    metadata:
      labels:
        app: vault-pg-debug
    spec:
      restartPolicy: Never

      # IMPORTANT:
      # Mets ici le MÊME ServiceAccount que ton job liquibase utilisait
      # (celui qui marche avec ton role Vault "ns-wall-e-springboot-local-ap11236-java-application-liquibase")
      serviceAccountName: default

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
            - name: VAULT_CACERT
              value: "/etc/ssl/certs/ca.crt"
            - name: SKIP_CHOWN
              value: "true"
            - name: SKIP_SETCAP
              value: "true"
          volumeMounts:
            - name: vault-shared-data
              mountPath: /etc/secrets
            - name: config
              mountPath: /etc/vault
              readOnly: true

      containers:
        - name: show-pg-env
          image: busybox:1.36
          imagePullPolicy: IfNotPresent
          command: ["/bin/sh","-lc"]
          args:
            - |
              echo "===== DEBUG: listing /etc/secrets ====="
              ls -la /etc/secrets || true
              echo "===== DEBUG: content of /etc/secrets/pg.env ====="
              cat /etc/secrets/pg.env || true
              echo "===== DEBUG: extracted values ====="
              # source le fichier si c’est un envfile
              . /etc/secrets/pg.env 2>/dev/null || true
              echo "PGUSER=$PGUSER"
              echo "PGPASSWORD=$PGPASSWORD"
              echo "===== END ====="
              # petit sleep pour te laisser le temps de voir le pod si besoin
              sleep 20
          volumeMounts:
            - name: vault-shared-data
              mountPath: /etc/secrets
