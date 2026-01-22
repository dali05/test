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
          image: ap27627-docker-pf.artifactory-dogen.group.echonet/bnpp-pf/liquibase-postgres:4.32.0-1

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

            {{- /* Variables liquibase (URL, username, password, changelog, etc.) */ -}}
            {{- with .Values.liquibase.job.extraEnv }}
            {{- toYaml . | nindent 12 }}
            {{- end }}

          command: ["sh", "-c"]
          args:
            - |
              set -euo pipefail

              echo "Bootstrap: creating schema 'admin' if not exists..."

              : "${PF_LIQUIBASE_COMMAND_URL:?PF_LIQUIBASE_COMMAND_URL is required}"
              : "${PF_LIQUIBASE_COMMAND_USERNAME:?PF_LIQUIBASE_COMMAND_USERNAME is required}"
              : "${PF_LIQUIBASE_COMMAND_PASSWORD:?PF_LIQUIBASE_COMMAND_PASSWORD is required}"

              # Exécute un SQL simple AVANT le job Liquibase principal
              liquibase \
                --url="$PF_LIQUIBASE_COMMAND_URL" \
                --username="$PF_LIQUIBASE_COMMAND_USERNAME" \
                --password="$PF_LIQUIBASE_COMMAND_PASSWORD" \
                executeSql \
                --sql="CREATE SCHEMA IF NOT EXISTS admin;"

              echo "OK: schema admin exists."