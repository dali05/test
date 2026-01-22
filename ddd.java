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
      imagePullSecrets:
        {{- with .Values.liquibase.job.imagePullSecrets }}
        {{- toYaml . | nindent 8 }}
        {{- end }}
      containers:
        - name: bootstrap
          image: postgres:15-alpine
          env:
            # On réutilise les mêmes variables Vault que Liquibase
            {{- /* Injection Vaultenv (même mécanique que le job Liquibase) */}}
            {{- include "common-library.hashicorp.vaultenv" . | nindent 12 }}

            # On reprend les mêmes valeurs que le job Liquibase (URL/username/password)
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

              # Utilise les creds récupérés via Vaultenv
              export PGUSER="$PF_LIQUIBASE_COMMAND_USERNAME"
              export PGPASSWORD="$PF_LIQUIBASE_COMMAND_PASSWORD"

              # Crée le schéma requis avant Liquibase
              psql "$PSQL_URL" -v ON_ERROR_STOP=1 -c 'CREATE SCHEMA IF NOT EXISTS admin;'

              echo "OK: schema admin exists."
