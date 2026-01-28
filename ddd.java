liquibase:
  hashicorp:
    enabled: true

  job:
    image:
      fullName: walle-backend-admin-liquibase
      pullPolicy: Always

  bootstrap:
    enabled: true
    extraEnv:
      - name: PF_LIQUIBASE_COMMAND
        value: "update"
      - name: PF_LIQUIBASE_COMMAND_CHANGELOG_FILE
        value: "db/changelog/bootstrap-liquibase-meta.yaml"
      - name: PF_LIQUIBASE_COMMAND_DEFAULT_SCHEMA_NAME
        value: "liquibase_meta"
      - name: PF_LIQUIBASE_COMMAND_LIQUIBASE_SCHEMA_NAME
        value: "liquibase_meta"
      # Vault creds "owner" (si tu as un rôle owner)
      - name: PF_LIQUIBASE_COMMAND_USERNAME
        value: "vault:database/postgres/.../creds/owner_role#username"
      - name: PF_LIQUIBASE_COMMAND_PASSWORD
        value: "vault:database/postgres/.../creds/owner_role#password"

  migrate:
    enabled: true
    extraEnv:
      - name: PF_LIQUIBASE_COMMAND
        value: "update"
      - name: PF_LIQUIBASE_COMMAND_CHANGELOG_FILE
        value: "db/changelog/db.changelog-master.yaml"
      - name: PF_LIQUIBASE_COMMAND_DEFAULT_SCHEMA_NAME
        value: "admin"
      - name: PF_LIQUIBASE_COMMAND_LIQUIBASE_SCHEMA_NAME
        value: "liquibase_meta"
      # Vault creds "app"
      - name: PF_LIQUIBASE_COMMAND_USERNAME
        value: "vault:database/postgres/.../creds/app_role#username"
      - name: PF_LIQUIBASE_COMMAND_PASSWORD
        value: "vault:database/postgres/.../creds/app_role#password"


databaseChangeLog:
  - changeSet:
      id: 000-create-liquibase-meta-schema
      author: pipeline
      changes:
        - sql:
            sql: |
              CREATE SCHEMA IF NOT EXISTS liquibase_meta;


databaseChangeLog:
  - changeSet:
      id: 001-create-admin-schema
      author: pipeline
      changes:
        - sql:
            sql: |
              CREATE SCHEMA IF NOT EXISTS admin;
              GRANT USAGE, CREATE ON SCHEMA admin TO admin;


{{- if .Values.liquibase.bootstrap.enabled }}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include "common-library.fullName" . }}-liquibase-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-20"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: liquibase-bootstrap
          image: {{ .Values.liquibase.job.image.fullName }}
          imagePullPolicy: {{ .Values.liquibase.job.image.pullPolicy }}
          env:
{{ toYaml .Values.liquibase.bootstrap.extraEnv | nindent 12 }}
{{- if .Values.liquibase.hashicorp.enabled }}
{{ include "common-library.hashicorp.vaultenv" (dict
    "Values" .Values.liquibase
    "Release" .Release
    "Chart" .Chart
    "Capabilities" .Capabilities
  ) | nindent 12 }}
{{- end }}
{{- end }}

{{- if .Values.liquibase.migrate.enabled }}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include "common-library.fullName" . }}-liquibase-migrate
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "0"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: liquibase-migrate
          image: {{ .Values.liquibase.job.image.fullName }}
          imagePullPolicy: {{ .Values.liquibase.job.image.pullPolicy }}
          env:
{{ toYaml .Values.liquibase.migrate.extraEnv | nindent 12 }}
{{- if .Values.liquibase.hashicorp.enabled }}
{{ include "common-library.hashicorp.vaultenv" (dict
    "Values" .Values.liquibase
    "Release" .Release
    "Chart" .Chart
    "Capabilities" .Capabilities
  ) | nindent 12 }}
{{- end }}
{{- end }}


