apiVersion: v1
kind: ServiceAccount
metadata:
  name: local-ap11236-java-application-liquibase
  namespace: ns-wall-e-springboot

apiVersion: batch/v1
kind: Job
metadata:
  name: vault-pg-debug
  namespace: ns-wall-e-springboot

spec:
  backoffLimit: 0

  template:
    spec:
      serviceAccountName: default   # ← IMPORTANT FIX

      restartPolicy: Never

      volumes:
        - name: vault-shared-data
          emptyDir: {}

        - name: config
          configMap:
            name: wall-e-vault-agent-config

      initContainers:
        - name: vault-agent
          image: hashicorp/vault:1.21
          args:
            - "agent"
            - "-config=/etc/vault/vault-agent-config.hcl"

          env:
            - name: VAULT_ADDR
              value: "http://vault.ns-vault.svc.cluster.local:8200"

            - name: VAULT_NAMESPACE
              value: "ns-wall-e-springboot"

          volumeMounts:
            - name: vault-shared-data
              mountPath: /etc/secrets

            - name: config
              mountPath: /etc/vault
              readOnly: true

      containers:
        - name: show-secrets
          image: busybox:1.36
          command: ["/bin/sh", "-c"]
          args:
            - |
              echo "Waiting for vault secrets..."

              for i in $(seq 1 20); do
                if [ -f /etc/secrets/pg.env ]; then
                  break
                fi
                sleep 1
              done

              if [ ! -f /etc/secrets/pg.env ]; then
                echo "❌ pg.env not found"
                exit 1
              fi

              echo "=== RAW FILE ==="
              cat /etc/secrets/pg.env

              echo ""
              echo "=== EXTRACTED VALUES ==="
              . /etc/secrets/pg.env
              echo "PGUSER=$PGUSER"
              echo "PGPASSWORD=$PGPASSWORD"

              echo "Sleeping for debug..."
              sleep 3600

          volumeMounts:
            - name: vault-shared-data
              mountPath: /etc/secrets
