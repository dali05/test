{{- $ctx := dict "Values" .Values.liquibase "Chart" .Chart "Release" .Release "Capabilities" .Capabilities -}}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-db-bootstrap
  annotations:
    helm.sh/hook: pre-install,pre-upgrade
    helm.sh/hook-weight: "-20"   # AVANT liquibase
    helm.sh/hook-delete-policy: before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never
      serviceAccountName: {{ .Release.Name }}-java-application-liquibase
      containers:
        - name: db-bootstrap
          image: wall-e-sql
          imagePullPolicy: IfNotPresent
          command: ["sh", "-c"]
          args:
            - |
              set -euo pipefail

              echo "Parsing JDBC URL..."
              # jdbc:postgresql://HOST:PORT/DB
              PGHOST="$(echo "$PF_LIQUIBASE_COMMAND_URL" | sed -E 's#^jdbc:postgresql://([^:/]+).*$#\1#')"
              PGPORT="$(echo "$PF_LIQUIBASE_COMMAND_URL" | sed -E 's#^jdbc:postgresql://[^:/]+:([0-9]+)/.*$#\1#')"
              PGDATABASE="$(echo "$PF_LIQUIBASE_COMMAND_URL" | sed -E 's#^.*/([^/?]+).*$#\1#')"

              export PGPASSWORD="$PF_LIQUIBASE_COMMAND_PASSWORD"

              echo "Waiting for PostgreSQL at ${PGHOST}:${PGPORT}/${PGDATABASE}..."
              until psql -h "$PGHOST" -p "$PGPORT" -U "$PF_LIQUIBASE_COMMAND_USERNAME" -d "$PGDATABASE" -c "select 1" >/dev/null 2>&1; do
                sleep 2
              done

              echo "Creating schemas..."
              psql -h "$PGHOST" -p "$PGPORT" -U "$PF_LIQUIBASE_COMMAND_USERNAME" -d "$PGDATABASE" \
                -v ON_ERROR_STOP=1 \
                -c "CREATE SCHEMA IF NOT EXISTS admin;" \
                -c "CREATE SCHEMA IF NOT EXISTS liquibase;"

              echo "DB bootstrap done ✔"
          env:
            {{ include "common-library.hashicorp.vaultenv" $ctx | nindent 12 }}
            {{- with .Values.liquibase.job.extraEnv }}
            {{ toYaml . | nindent 12 }}
            {{- end }}
