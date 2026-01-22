{{- if .Values.liquibase.job.enabled }}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ printf "%s-db-bootstrap" .Release.Name | trunc 63 | trimSuffix "-" }}
  labels:
    app.kubernetes.io/name: {{ .Chart.Name }}
    app.kubernetes.io/instance: {{ .Release.Name }}
    app.kubernetes.io/component: db-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "0"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: {{ .Values.liquibase.job.backoffLimit | default 1 }}
  ttlSecondsAfterFinished: {{ .Values.liquibase.job.ttlSecondsAfterFinished | default 300 }}
  template:
    metadata:
      labels:
        app.kubernetes.io/name: {{ .Chart.Name }}
        app.kubernetes.io/instance: {{ .Release.Name }}
        app.kubernetes.io/component: db-bootstrap
    spec:
      restartPolicy: Never

      {{- if .Values.liquibase.job.serviceAccountName }}
      serviceAccountName: {{ .Values.liquibase.job.serviceAccountName | quote }}
      {{- else if .Values.serviceAccount.name }}
      serviceAccountName: {{ .Values.serviceAccount.name | quote }}
      {{- else }}
      serviceAccountName: {{ printf "%s-sa" .Release.Name | trunc 63 | trimSuffix "-" | quote }}
      {{- end }}

      {{- if .Values.liquibase.job.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml .Values.liquibase.job.imagePullSecrets | nindent 8 }}
      {{- else if .Values.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml .Values.imagePullSecrets | nindent 8 }}
      {{- end }}

      containers:
        - name: bootstrap
          image: "{{ required "liquibase.job.image.fullName is required" .Values.liquibase.job.image.fullName }}"
          imagePullPolicy: {{ .Values.liquibase.job.image.pullPolicy | default "IfNotPresent" }}

          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: false
            runAsNonRoot: true

          env:
            # ---- Vaultenv configuration ----
            - name: VAULT_ADDR
              value: {{ required "hashicorp.addr is required" .Values.hashicorp.addr | quote }}
            - name: VAULT_PATH
              value: {{ required "hashicorp.path is required" .Values.hashicorp.path | quote }}
            - name: VAULT_NAMESPACE
              value: {{ .Values.hashicorp.ns | default "" | quote }}
            - name: VAULT_SKIP_VERIFY
              value: {{ .Values.hashicorp.skipVerify | default "true" | quote }}
            - name: VAULT_CLIENT_TIMEOUT
              value: {{ .Values.hashicorp.timeout | default "10s" | quote }}

            # ---- Liquibase / DB parameters (these will be resolved by vaultenv if they contain vault paths) ----
            - name: PF_LIQUIBASE_COMMAND_URL
              value: {{ required "liquibase.job.extraEnv must contain PF_LIQUIBASE_COMMAND_URL" (pluck "value" (first (where .Values.liquibase.job.extraEnv "name" "PF_LIQUIBASE_COMMAND_URL")) | first | default "") | quote }}
            - name: PF_LIQUIBASE_COMMAND_USERNAME
              value: {{ required "liquibase.job.extraEnv must contain PF_LIQUIBASE_COMMAND_USERNAME" (pluck "value" (first (where .Values.liquibase.job.extraEnv "name" "PF_LIQUIBASE_COMMAND_USERNAME")) | first | default "") | quote }}
            - name: PF_LIQUIBASE_COMMAND_PASSWORD
              value: {{ required "liquibase.job.extraEnv must contain PF_LIQUIBASE_COMMAND_PASSWORD" (pluck "value" (first (where .Values.liquibase.job.extraEnv "name" "PF_LIQUIBASE_COMMAND_PASSWORD")) | first | default "") | quote }}
            - name: PF_LIQUIBASE_COMMAND_CHANGELOG_FILE
              value: {{ required "liquibase.job.extraEnv must contain PF_LIQUIBASE_COMMAND_CHANGELOG_FILE" (pluck "value" (first (where .Values.liquibase.job.extraEnv "name" "PF_LIQUIBASE_COMMAND_CHANGELOG_FILE")) | first | default "") | quote }}

            - name: LIQUIBASE_DEFAULT_SCHEMA_NAME
              value: {{ .Values.liquibase.job.defaultSchema | default "admin" | quote }}
            - name: LIQUIBASE_LIQUIBASE_SCHEMA_NAME
              value: {{ .Values.liquibase.job.liquibaseSchema | default "admin" | quote }}

            # ---- Any additional env from values ----
            {{- with .Values.liquibase.job.extraEnv }}
            {{- toYaml . | nindent 12 }}
            {{- end }}

          command: ["sh","-c"]
          args:
            - |
              set -euo pipefail

              echo "Bootstrap: starting..."
              echo "Bootstrap: schema=${LIQUIBASE_DEFAULT_SCHEMA_NAME}"

              # PF_LIQUIBASE_COMMAND_URL is JDBC like: jdbc:postgresql://host:5432/db
              if [ -z "${PF_LIQUIBASE_COMMAND_URL:-}" ]; then
                echo "ERROR: PF_LIQUIBASE_COMMAND_URL is not set"
                exit 1
              fi

              # Convert JDBC url to psql url (remove jdbc:)
              PSQL_URL="$(echo "$PF_LIQUIBASE_COMMAND_URL" | sed 's/^jdbc://')"

              # vaultenv should have resolved these to real values by now
              if [ -z "${PF_LIQUIBASE_COMMAND_USERNAME:-}" ] || [ -z "${PF_LIQUIBASE_COMMAND_PASSWORD:-}" ]; then
                echo "ERROR: PF_LIQUIBASE_COMMAND_USERNAME/PASSWORD not set (vaultenv did not resolve?)"
                exit 1
              fi

              export PGUSER="$PF_LIQUIBASE_COMMAND_USERNAME"
              export PGPASSWORD="$PF_LIQUIBASE_COMMAND_PASSWORD"

              echo "Bootstrap: creating schema '${LIQUIBASE_DEFAULT_SCHEMA_NAME}' if not exists..."
              psql "$PSQL_URL" -v ON_ERROR_STOP=1 -c "CREATE SCHEMA IF NOT EXISTS ${LIQUIBASE_DEFAULT_SCHEMA_NAME};"

              echo "Bootstrap: running liquibase update..."
              liquibase \
                --url="$PF_LIQUIBASE_COMMAND_URL" \
                --username="$PF_LIQUIBASE_COMMAND_USERNAME" \
                --password="$PF_LIQUIBASE_COMMAND_PASSWORD" \
                --changeLogFile="$PF_LIQUIBASE_COMMAND_CHANGELOG_FILE" \
                --defaultSchemaName="$LIQUIBASE_DEFAULT_SCHEMA_NAME" \
                --liquibaseSchemaName="$LIQUIBASE_LIQUIBASE_SCHEMA_NAME" \
                update

              echo "Bootstrap: done."
{{- end }}