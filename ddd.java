{{- $ctx := dict "Values" .Values.liquibase "Chart" .Chart "Release" .Release "Capabilities" .Capabilities -}}

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

      # ⚠️ important: même SA que liquibase si Vault est bound à ce SA
      serviceAccountName: {{ include "common-library.serviceAccountName" $ctx }}

      containers:
        - name: create-liquibase-schema
          image: "{{ .Values.liquibase.job.image.fullName }}"
          imagePullPolicy: {{ .Values.liquibase.job.image.pullPolicy | default "IfNotPresent" }}
          command: ["sh","-c"]
          args:
            - |
              set -euo pipefail

              # schema liquibase
              SCHEMA="${LIQUIBASE_LIQUIBASE_SCHEMA_NAME:-liquibase}"

              # JDBC -> postgres URL
              JDBC_URL="${PF_LIQUIBASE_COMMAND_URL}"
              PGURL="$(echo "$JDBC_URL" | sed -E 's|^jdbc:||')"

              export PGPASSWORD="${PF_LIQUIBASE_COMMAND_PASSWORD}"

              echo "Creating schema ${SCHEMA}..."
              psql "${PGURL}" -U "${PF_LIQUIBASE_COMMAND_USERNAME}" -v ON_ERROR_STOP=1 \
                -c "CREATE SCHEMA IF NOT EXISTS \"${SCHEMA}\";"

          env:
{{ include "common-library.hashicorp.vaultenv" $ctx | nindent 12 }}
{{- with .Values.liquibase.job.extraEnv }}
{{ toYaml . | nindent 12 }}
{{- end }}