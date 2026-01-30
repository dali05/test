apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include "common-library.fullname" . }}-liquibase-schema
  annotations:
    "helm.sh/hook": pre-install, pre-upgrade
    "helm.sh/hook-weight": "-2"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
  labels:
{{ include "common-library.metadata.labels" . | nindent 4 }}
spec:
  backoffLimit: 1
  template:
    metadata:
      labels:
{{ include "common-library.selector.labels" . | nindent 8 }}
{{ include "common-library.deployment.labels" . | nindent 8 }}
      annotations:
{{ include "common-library.illumio.annotations" . | nindent 8 }}
    spec:
      restartPolicy: Never

      # IMPORTANT: utiliser le serviceAccount qui est bound au role Vault
      serviceAccountName: {{ .Values.liquibase.serviceAccount.name | default (include "common-library.serviceAccountName" .) }}

      # --- Vault Agent initContainer (injecte secrets dans un volume partagé) ---
      initContainers:
{{- /* On réutilise la helper de votre lib */ -}}
{{ include "common-library.hashicorp.initcontainer" (dict "Values" (mergeOverwrite (deepCopy .Values.liquibase) (dict "hashicorp" (mergeOverwrite (deepCopy .Values.liquibase.hashicorp) (dict "method" "vault-agent-initcontainer")))) "Chart" .Chart "Release" .Release "Capabilities" .Capabilities) | nindent 8 }}

      volumes:
        - name: mnt-tmp
          emptyDir: {}
        - name: mnt-user
          emptyDir: {}
{{ include "common-library.hashicorp.initcontainer.volumes" (dict "Values" (mergeOverwrite (deepCopy .Values.liquibase) (dict "hashicorp" (mergeOverwrite (deepCopy .Values.liquibase.hashicorp) (dict "method" "vault-agent-initcontainer")))) "Chart" .Chart "Release" .Release "Capabilities" .Capabilities) | nindent 8 }}

      containers:
        - name: create-liquibase-schema
          image: postgres:15-alpine
          command: ["sh","-c"]
          args:
            - |
              set -euo pipefail

              # ⚠️ Adapter ces chemins selon ce que votre vault-agent template écrit.
              # Dans beaucoup de setups: /applis/vault/<fichier>
              # Exemple simple si votre template génère un fichier env:
              if [ -f /applis/vault/db.env ]; then
                . /applis/vault/db.env
              fi

              # Attendu: DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD
              : "${DB_HOST:?missing DB_HOST}"
              : "${DB_PORT:?missing DB_PORT}"
              : "${DB_NAME:?missing DB_NAME}"
              : "${DB_USER:?missing DB_USER}"
              : "${DB_PASSWORD:?missing DB_PASSWORD}"

              export PGPASSWORD="$DB_PASSWORD"
              echo "Creating schema liquibase if not exists..."
              psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 \
                -c 'CREATE SCHEMA IF NOT EXISTS liquibase;'

          volumeMounts:
            - name: vault-shared-data
              mountPath: /applis/vault