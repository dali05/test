apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-db-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-10"
    "helm.sh/hook-delete-policy": hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: bootstrap
          image: postgres:15-alpine
          env:
            {{- /* Contexte "root" attendu par la common-library */ -}}
            {{- $vals := merge (deepCopy .Values) (dict
                "hashicorp" .Values.liquibase.hashicorp
                "serviceAccount" .Values.liquibase.serviceAccount
              ) -}}
            {{- $ctx := dict
                "Values" $vals
                "Release" .Release
                "Chart" .Chart
                "Capabilities" .Capabilities
                "Template" .Template
              -}}
            {{- include "common-library.hashicorp.vaultenv" $ctx | nindent 12 }}

            {{- with .Values.liquibase.job.extraEnv }}
            {{- toYaml . | nindent 12 }}
            {{- end }}

          command: ["sh","-c"]
          args:
            - |
              set -euo pipefail

              # Convertit jdbc:postgresql://... en postgresql://... pour psql
              PSQL_URL="$(echo "$PF_LIQUIBASE_COMMAND_URL" | sed 's/^jdbc://')"

              echo "Connecting to DB with psql..."
              echo "Creating schema admin if not exists..."

              export PGUSER="$PF_LIQUIBASE_COMMAND_USERNAME"
              export PGPASSWORD="$PF_LIQUIBASE_COMMAND_PASSWORD"

              psql "$PSQL_URL" -v ON_ERROR_STOP=1 -c 'CREATE SCHEMA IF NOT EXISTS admin;'

              echo "OK: schema admin exists."
