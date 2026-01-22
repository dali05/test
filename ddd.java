{{- if .Values.liquibase.job.enabled }}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-db-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never

      serviceAccountName: {{ .Values.serviceAccount.name | default (printf "%s-sa" .Release.Name) }}

      imagePullSecrets:
        - name: {{ .Values.imagePullSecret }}

      containers:
        - name: liquibase
          image: "{{ .Values.liquibase.job.image }}"
          imagePullPolicy: IfNotPresent

          command: ["sh", "-c"]
          args:
            - |
              set -e

              echo "=== DB Bootstrap start ==="

              if [ -z "$PF_LIQUIBASE_COMMAND_URL" ]; then
                echo "PF_LIQUIBASE_COMMAND_URL is missing"
                exit 1
              fi

              export PGUSER="$PF_LIQUIBASE_COMMAND_USERNAME"
              export PGPASSWORD="$PF_LIQUIBASE_COMMAND_PASSWORD"

              PSQL_URL=$(echo "$PF_LIQUIBASE_COMMAND_URL" | sed 's/^jdbc://')

              echo "Creating schema admin if not exists"
              psql "$PSQL_URL" -c "CREATE SCHEMA IF NOT EXISTS admin;"

              echo "Running Liquibase"
              liquibase \
                --url="$PF_LIQUIBASE_COMMAND_URL" \
                --username="$PF_LIQUIBASE_COMMAND_USERNAME" \
                --password="$PF_LIQUIBASE_COMMAND_PASSWORD" \
                --changeLogFile="$PF_LIQUIBASE_CHANGELOG" \
                --defaultSchemaName=admin \
                update

              echo "=== DB Bootstrap done ==="

          env:
            # ---- Vaultenv ----
            - name: VAULT_ADDR
              value: {{ .Values.vault.addr | quote }}
            - name: VAULT_PATH
              value: {{ .Values.vault.path | quote }}
            - name: VAULT_SKIP_VERIFY
              value: "true"

            # ---- Liquibase ----
            - name: PF_LIQUIBASE_COMMAND_URL
              value: {{ .Values.liquibase.url | quote }}

            - name: PF_LIQUIBASE_COMMAND_USERNAME
              value: {{ .Values.liquibase.username | quote }}

            - name: PF_LIQUIBASE_COMMAND_PASSWORD
              value: {{ .Values.liquibase.password | quote }}

            - name: PF_LIQUIBASE_CHANGELOG
              value: {{ .Values.liquibase.changelog | quote }}
{{- end }}