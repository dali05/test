{{- /*
templates/db-bootstrap-job.yaml

Hypothèses :
- Vault Agent Injector est installé dans le cluster
- Un role Vault Kubernetes existe et permet de lire database/creds/...
- Le ServiceAccount utilisé par ce Job est autorisé dans Vault (bound_service_account_names / namespaces)
*/ -}}
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
        # --- Vault Agent Injector ---
        vault.hashicorp.com/agent-inject: "true"

        # ⚠️ Mets ici le role Vault utilisé pour l’auth Kubernetes
        # Si tu as déjà un role applicatif, réutilise-le.
        vault.hashicorp.com/role: {{ default (printf "%s-vault" .Release.Name) .Values.vault.role | quote }}

        # Le secret dynamique Postgres (engine database -> creds/<role>)
        # Exemple typique: database/creds/own_pg000000_ibmclouddb
        vault.hashicorp.com/agent-inject-secret-dbcreds: {{ default "database/creds/own_pg000000_ibmclouddb" .Values.vault.dbCredsPath | quote }}

        # On écrit 2 lignes : username puis password dans /vault/secrets/dbcreds
        vault.hashicorp.com/agent-inject-template-dbcreds: |
          {{`{{- with secret "`}}{{ default "database/creds/own_pg000000_ibmclouddb" .Values.vault.dbCredsPath }}{{`" -}}
          {{ .Data.username }}
          {{ .Data.password }}
          {{- end -}}`}}

        # (Optionnel) Namespace Vault si vous l’utilisez. Sinon laisse vide.
        {{- if .Values.vault.namespace }}
        vault.hashicorp.com/namespace: {{ .Values.vault.namespace | quote }}
        {{- end }}

    spec:
      restartPolicy: Never

      # ⚠️ Important : ce SA doit être autorisé dans Vault (auth kubernetes)
      serviceAccountName: {{ default (printf "%s-sa" .Release.Name) .Values.serviceAccount.name | quote }}

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
                echo "Check: Vault injector annotations, serviceAccountName, and Vault role binding."
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

              echo "Running bootstrap on ${PGHOST}:${PGPORT}/${PGDATABASE} as ${PGUSER}"
              psql -v ON_ERROR_STOP=1 -c "CREATE SCHEMA IF NOT EXISTS admin; CREATE SCHEMA IF NOT EXISTS liquibase;"

          env:
            # Ces valeurs viennent de values.yaml (à adapter)
            - name: TZ
              value: "UTC"

{{- end }}