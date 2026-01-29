{{- if .Values.liquibase.job.enabled }}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-db-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-10"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    metadata:
      annotations:
        vault.hashicorp.com/agent-inject: "true"
        vault.hashicorp.com/role: {{ .Values.vault.role | quote }}
        vault.hashicorp.com/agent-inject-secret-dbcreds: {{ .Values.vault.dbCredsPath | quote }}
        vault.hashicorp.com/agent-inject-template-dbcreds: |
          {{`{{- with secret "`}}{{ .Values.vault.dbCredsPath }}{{`" -}}
          {{ .Data.username }}
          {{ .Data.password }}
          {{- end -}}`}}
        {{- if .Values.vault.namespace }}
        vault.hashicorp.com/namespace: {{ .Values.vault.namespace | quote }}
        {{- end }}
    spec:
      serviceAccountName: dali
      restartPolicy: Never
      containers:
        - name: db-bootstrap
          image: postgres:15
          command: ["sh","-c"]
          args:
            - |
              set -euo pipefail

              CREDS_FILE="/vault/secrets/dbcreds"
              if [ ! -f "$CREDS_FILE" ]; then
                echo "ERROR: Vault did not inject $CREDS_FILE"
                echo "Check: injector enabled for namespace + role binding to serviceAccount 'dali'"
                ls -la /vault || true
                exit 1
              fi

              USER="$(sed -n '1p' "$CREDS_FILE")"
              PASS="$(sed -n '2p' "$CREDS_FILE")"

              export PGHOST="{{ .Values.db.host }}"
              export PGPORT="{{ .Values.db.port }}"
              export PGDATABASE="{{ .Values.db.database }}"
              export PGUSER="$USER"
              export PGPASSWORD="$PASS"

              psql -v ON_ERROR_STOP=1 -c "CREATE SCHEMA IF NOT EXISTS admin; CREATE SCHEMA IF NOT EXISTS liquibase;"
{{- end }}