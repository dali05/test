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
              La common-library attend certaines clés à la racine:
              - .Values.hashicorp
              - .Values.serviceAccount
              - .Values.selectors
              Or chez toi elles sont sous .Values.liquibase.*
              On "remonte" ces clés via un contexte artificiel.
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

            {{- /* Injection Vaultenv (résout les vault:... en valeurs réelles) */ -}}
            {{- include "common-library.hashicorp.vaultenv" $ctx | nindent 12 }}

            {{- /* Variables DB host/port/db (tu les as dans values.yaml sous backend.extraEnv) */ -}}
            {{- with .Values.backend.extraEnv }}
            {{- toYaml . | nindent 12 }}
            {{- end }}

            {{- /* Variables liquibase (username/password, url, etc. sous liquibase.job.extraEnv) */ -}}
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
              : "${PF_LIQUIBASE_COMMAND_USERNAME:?PF_LIQUIBASE_COMMAND_USERNAME is required}"
              : "${PF_LIQUIBASE_COMMAND_PASSWORD:?PF_LIQUIBASE_COMMAND_PASSWORD is required}"

              export PGHOST="$POSTGRES_HOST"
              export PGPORT="$POSTGRES_PORT"
              export PGDATABASE="$POSTGRES_DATABASE"
              export PGUSER="$PF_LIQUIBASE_COMMAND_USERNAME"
              export PGPASSWORD="$PF_LIQUIBASE_COMMAND_PASSWORD"

              psql -v ON_ERROR_STOP=1 -c 'CREATE SCHEMA IF NOT EXISTS admin;'

              echo "OK: schema admin exists."
