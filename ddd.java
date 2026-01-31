liquibase:
  hashicorp:
    enabled: true
    method: "vault-agent-initcontainer"
    addr: "http://vault.ns-vault.svc.cluster.local:8200"
    ns: ""

    template: |
      exit_after_auth = true
      pid_file = "/home/vault/pidfile"

      auto_auth {
        method "kubernetes" {
          mount_path = "auth/kubernetes"
          config = {
            role = "ns-wall-e-springboot-local-ap11236-java-application-liquibase"
          }
        }
      }

      sink "file" {
        config = {
          path = "/home/vault/.token"
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
      serviceAccountName: {{ include "common-library.serviceAccountName" . }}

      containers:
        - name: create-liquibase-schema
          image: "{{ .Values.liquibase.job.image.fullName }}"
          imagePullPolicy: {{ .Values.liquibase.job.image.pullPolicy | default "IfNotPresent" }}
          command: ["sh","-c"]
          args:
            - |
              set -euo pipefail

              echo "Sourcing Vault DB env"
              . /applis/vault/db.env

              echo "Creating schema liquibase..."
              psql \
                -h "$DB_HOST" \
                -p "$DB_PORT" \
                -U "$DB_USER" \
                -d "$DB_NAME" \
                -v ON_ERROR_STOP=1 \
                -c "CREATE SCHEMA IF NOT EXISTS liquibase;"
          volumeMounts:
            - name: vault-shared-data
              mountPath: /applis/vault
