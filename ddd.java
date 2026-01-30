apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "common-library.fullname" . }}-vault-agent-config
  annotations:
    "helm.sh/hook": pre-install, pre-upgrade
    "helm.sh/hook-weight": "-4"
    "helm.sh/hook-delete-policy": before-hook-creation
data:
  # ⚠️ IMPORTANT: la clé doit s'appeler EXACTEMENT comme ça
  vault-agent-config.hcl: |
{{ index .Values.liquibase.hashicorp "template" | nindent 4 }}



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

      # ⚠️ le service account bound au role Vault
      serviceAccountName: local-ap11236-java-application-liquibase

      volumes:
        - name: config
          configMap:
            name: {{ include "common-library.fullname" . }}-vault-agent-config
            items:
              - key: vault-agent-config.hcl
                path: vault-agent-config.hcl

        - name: vault-shared-data
          emptyDir: {}

        - name: vault-home-volume
          emptyDir: {}

      initContainers:
        - name: vault-agent
          image: bnpp-pf/vault:1.18.1
          command: ["vault","agent","-config=/etc/vault/vault-agent-config.hcl","-log-level=info"]
          env:
            - name: VAULT_ADDR
              value: "{{ .Values.liquibase.hashicorp.addr }}"
            - name: VAULT_NAMESPACE
              value: "{{ .Values.liquibase.hashicorp.ns | default "" }}"
          volumeMounts:
            - name: config
              mountPath: /etc/vault
              readOnly: true
            - name: vault-shared-data
              mountPath: /applis/vault
            - name: vault-home-volume
              mountPath: /home/vault

      containers:
        - name: create-liquibase-schema
          image: postgres:15-alpine
          command: ["sh","-c"]
          args:
            - |
              set -euo pipefail
              echo "Loading creds from vault..."
              . /applis/vault/db.env

              export PGPASSWORD="$DB_PASSWORD"

              echo "Creating schema liquibase if not exists..."
              psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 \
                -c 'CREATE SCHEMA IF NOT EXISTS liquibase;'
          volumeMounts:
            - name: vault-shared-data
              mountPath: /applis/vault
