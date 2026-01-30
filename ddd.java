{{- /*
Job hook qui crée le schema Liquibase avant l'exécution du job Liquibase
*/ -}}
{{- $ctx := dict "Values" .Values.liquibase "Chart" .Chart "Release" .Release "Capabilities" .Capabilities -}}

apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include "common-library.fullname" . }}-liquibase-schema
  annotations:
    "helm.sh/hook": pre-install, pre-upgrade
    "helm.sh/hook-weight": "-2"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never
      serviceAccountName: {{ include "common-library.serviceAccountName" . }}
      containers:
        - name: init-liquibase-schema
          image: "{{ .Values.liquibase.job.image.fullName }}"
          imagePullPolicy: {{ .Values.liquibase.job.image.pullPolicy | default "IfNotPresent" }}
          command: ["sh","-c"]
          args:
            - |
              set -euo pipefail
              SCHEMA="${LIQUIBASE_LIQUIBASE_SCHEMA_NAME:-liquibase}"
              JDBC_URL="${PF_LIQUIBASE_COMMAND_URL}"
              PGURL="$(echo "$JDBC_URL" | sed -E 's|^jdbc:||')"
              export PGPASSWORD="${PF_LIQUIBASE_COMMAND_PASSWORD}"
              psql "${PGURL}" -U "${PF_LIQUIBASE_COMMAND_USERNAME}" -v ON_ERROR_STOP=1 \
                -c "CREATE SCHEMA IF NOT EXISTS \"${SCHEMA}\";"

          env:
{{ include "common-library.hashicorp.vaultenv" $ctx | nindent 12 }}
{{- with .Values.liquibase.job.extraEnv }}
{{ toYaml . | nindent 12 }}
{{- end }}
