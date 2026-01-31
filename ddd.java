apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "common-library.fullname" . }}-vault-agent-config
  annotations:
    "helm.sh/hook": pre-install, pre-upgrade
    "helm.sh/hook-weight": "-4"
    "helm.sh/hook-delete-policy": before-hook-creation
data:
  vault-agent-config.hcl: |
    exit_after_auth = true
    pid_file = "/home/vault/pidfile"

    auto_auth {
      method "kubernetes" {
        mount_path = "auth/kubernetes"
        config = {
          role = "ns-wall-e-springboot-local-ap11236-java-application-liquibase"
        }
      }
      sink "file" {
        config = {
          path = "/home/vault/.token"
        }
      }
    }

    template {
      contents = <<EOT
DB_USER={{ with secret "database/postgres/pg000000/creds/app_pg000000_ibmclouddb" }}{{ .Data.username }}{{ end }}
DB_PASSWORD={{ with secret "database/postgres/pg000000/creds/app_pg000000_ibmclouddb" }}{{ .Data.password }}{{ end }}
DB_HOST=postgresql.ns-postgresql.svc.cluster.local
DB_PORT=5432
DB_NAME=<TA_DB>
EOT
      destination = "/applis/vault/db.env"
    }


apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include "common-library.fullname" . }}-db-bootstrap
  annotations:
    "helm.sh/hook": pre-install, pre-upgrade
    "helm.sh/hook-weight": "-3"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never

      # IMPORTANT: doit matcher le bound service account du role Vault
      serviceAccountName: local-ap11236-java-application-liquibase

      volumes:
        - name: config
          configMap:
            name: {{ include "common-library.fullname" . }}-vault-agent-config
            items:
              - key: vault-agent-config.hcl
                path: vault-agent-config.hcl
        - name: vault-shared-data
          emptyDir: {}
        - name: vault-home
          emptyDir: {}

      initContainers:
        - name: vault-agent
          image: bnpp-pf/vault:1.20.3-2
          command: ["vault","agent","-config=/etc/vault/vault-agent-config.hcl","-log-level=info"]
          env:
            - name: VAULT_ADDR
              value: "https://vault.ns-vault.svc.cluster.local:8200"
            # DEV/local: ignore cert (sinon bad certificate)
            - name: VAULT_SKIP_VERIFY
              value: "true"
          volumeMounts:
            - name: config
              mountPath: /etc/vault
              readOnly: true
            - name: vault-shared-data
              mountPath: /applis/vault
            - name: vault-home
              mountPath: /home/vault

      containers:
        - name: create-liquibase-schema
          image: wall-e-sql   # ton image qui contient psql
          command: ["sh","-c"]
          args:
            - |
              set -euo pipefail

              echo "== db.env =="
              ls -l /applis/vault || true
              cat /applis/vault/db.env

              . /applis/vault/db.env
              export PGPASSWORD="$DB_PASSWORD"

              echo "Creating schema liquibase..."
              psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 \
                -c 'CREATE SCHEMA IF NOT EXISTS liquibase;'

          volumeMounts:
            - name: vault-shared-data
              mountPath: /applis/vault
