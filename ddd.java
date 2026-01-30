apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include "common-library.fullname" . }}-db-bootstrap
  annotations:
    "helm.sh/hook": pre-install, pre-upgrade
    "helm.sh/hook-weight": "-3"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never

      # ⚠️ IMPORTANT : serviceAccount lié au role Vault
      serviceAccountName: local-ap11236-java-application-liquibase

      # 🔐 InitContainer Vault Agent (fourni par la common-library)
      initContainers:
{{ include "common-library.hashicorp.initcontainer" (dict "Values" (mergeOverwrite (deepCopy .Values.liquibase) (dict "hashicorp" (mergeOverwrite (deepCopy .Values.liquibase.hashicorp) (dict "method" "vault-agent-initcontainer")))) "Chart" .Chart "Release" .Release "Capabilities" .Capabilities) | nindent 8 }}

      volumes:
{{ include "common-library.hashicorp.initcontainer.volumes" (dict "Values" (mergeOverwrite (deepCopy .Values.liquibase) (dict "hashicorp" (mergeOverwrite (deepCopy .Values.liquibase.hashicorp) (dict "method" "vault-agent-initcontainer")))) "Chart" .Chart "Release" .Release "Capabilities" .Capabilities) | nindent 8 }}

      containers:
        - name: create-liquibase-schema
          image: postgres:15-alpine
          command: ["sh","-c"]
          args:
            - |
              set -euo pipefail

              echo "Loading DB credentials from Vault..."
              . /applis/vault/db.env

              export PGPASSWORD="$DB_PASSWORD"

              echo "Creating schema liquibase if not exists..."
              psql \
                -h "$DB_HOST" \
                -p "$DB_PORT" \
                -U "$DB_USER" \
                -d "$DB_NAME" \
                -v ON_ERROR_STOP=1 \
                -c 'CREATE SCHEMA IF NOT EXISTS liquibase;'

          volumeMounts:
            - name: vault-shared-data
              mountPath: /applis/vault