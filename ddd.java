

liquibase:
  job:
    extraEnv:
      - name: POSTGRES_HOST
        value: "postgresql.ns-postgresql.svc.cluster.local"

      - name: POSTGRES_PORT
        value: "5432"

      - name: POSTGRES_DATABASE
        value: "ibmclouddb"

      - name: PF_LIQUIBASE_COMMAND_USERNAME
        value: "vault:database/postgres/pg000000/creds/own_pg000000_ibmclouddb#username"

      - name: PF_LIQUIBASE_COMMAND_PASSWORD
        value: "vault:database/postgres/pg000000/creds/own_pg000000_ibmclouddb#password"

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

      containers:
        - name: bootstrap
          image: postgres:15-alpine

          env:
            {{- /*
              Remap root values for common-library
            */ -}}
            {{- $vals := merge (deepCopy .Values) (dict
                "hashicorp" .Values.liquibase.hashicorp
                "serviceAccount" .Values.liquibase.serviceAccount
                "selectors" .Values.liquibase.selectors
              ) -}}
            {{- $ctx := dict
                "Values" $vals
                "Release" .Release
                "Chart" .Chart
                "Capabilities" .Capabilities
                "Template" .Template
              -}}

            {{- /* Vault env */ -}}
            {{- include "common-library.hashicorp.vaultenv" $ctx | nindent 12 }}

            {{- /* DB + creds from liquibase.job.extraEnv */ -}}
            {{- with .Values.liquibase.job.extraEnv }}
            {{- toYaml . | nindent 12 }}
            {{- end }}

          command: ["sh", "-c"]
          args:
            - |
              set -euo pipefail

              echo "Bootstrap: creating schema 'admin' if not exists..."

              : "${POSTGRES_HOST:?POSTGRES_HOST is required}"
              : "${POSTGRES_PORT:?POSTGRES_PORT is required}"
              : "${POSTGRES_DATABASE:?POSTGRES_DATABASE is required}"
              : "${PF_LIQUIBASE_COMMAND_USERNAME:?USERNAME is required}"
              : "${PF_LIQUIBASE_COMMAND_PASSWORD:?PASSWORD is required}"

              export PGHOST="$POSTGRES_HOST"
              export PGPORT="$POSTGRES_PORT"
              export PGDATABASE="$POSTGRES_DATABASE"
              export PGUSER="$PF_LIQUIBASE_COMMAND_USERNAME"
              export PGPASSWORD="$PF_LIQUIBASE_COMMAND_PASSWORD"

              psql -v ON_ERROR_STOP=1 -c 'CREATE SCHEMA IF NOT EXISTS admin;'

              echo "OK: schema admin exists."