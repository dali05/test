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

        # ✅ role Kubernetes auth Vault (ton écran "role")
        vault.hashicorp.com/role: "ns-wall-e-springboot-local-ap11236-java-application-liquibase"

        # ✅ chemin autorisé par la policy (ton écran "policy")
        vault.hashicorp.com/agent-inject-secret-dbcreds: "database/postgres/pg0000000/creds/app_pg0000000_ibmclouddb"

        # On écrit username + password dans /vault/secrets/dbcreds (2 lignes)
        vault.hashicorp.com/agent-inject-template-dbcreds: |
          {{`{{- with secret "database/postgres/pg0000000/creds/app_pg0000000_ibmclouddb" -}}
          {{ .Data.username }}
          {{ .Data.password }}
          {{- end -}}`}}

    spec:
      # ✅ doit matcher EXACTEMENT bound_service_account_names
      serviceAccountName: local-ap11236-java-application-liquibase
      restartPolicy: Never

      containers:
        - name: db-bootstrap
          image: postgres:15
          imagePullPolicy: IfNotPresent
          command: ["sh","-c"]
          args:
            - |
              set -euo pipefail

              CREDS_FILE="/vault/secrets/dbcreds"
              if [ ! -f "$CREDS_FILE" ]; then
                echo "ERROR: Vault did not inject $CREDS_FILE"
                echo "Check: serviceAccountName + namespace + vault role name + secret path"
                echo "serviceAccountName=$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace 2>/dev/null || true)"
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