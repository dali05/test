apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-db-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  template:
    metadata:
      annotations:
        vault.hashicorp.com/agent-inject: "true"
        vault.hashicorp.com/role: "postgres-role"
        vault.hashicorp.com/agent-inject-secret-dbcreds: "database/creds/own_pg000000_ibmclouddb"
        vault.hashicorp.com/agent-inject-template-dbcreds: |
          {{`{{- with secret "database/creds/own_pg000000_ibmclouddb" -}}
          {{ .Data.username }}
          {{ .Data.password }}
          {{- end -}}`}}
    spec:
      serviceAccountName: <SERVICE_ACCOUNT_AUTORISE_VAULT>
      restartPolicy: Never
      containers:
        - name: db-bootstrap
          image: postgres:15
          command: ["sh","-c"]
          args:
            - |
              set -e
              USER="$(sed -n '1p' /vault/secrets/dbcreds)"
              PASS="$(sed -n '2p' /vault/secrets/dbcreds)"

              export PGUSER="$USER"
              export PGPASSWORD="$PASS"
              export PGHOST="postgresql.ns-postgresql.svc.cluster.local"
              export PGPORT="5432"
              export PGDATABASE="ibmclouddb"

              psql -c "CREATE SCHEMA IF NOT EXISTS admin; CREATE SCHEMA IF NOT EXISTS liquibase;"