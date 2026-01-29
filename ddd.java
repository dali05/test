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
    spec:
      restartPolicy: Never
      containers:
        - name: db-bootstrap
          image: postgres:15
          imagePullPolicy: IfNotPresent
          command: ["sh", "-c"]
          args:
            - |
              set -e

              # ✅ mapper les variables vault (PF_*) vers les variables psql (PG*)
              export PGUSER="$PF_LIQUIBASE_COMMAND_USERNAME"
              export PGPASSWORD="$PF_LIQUIBASE_COMMAND_PASSWORD"

              echo "Connected as PGUSER=$PGUSER"
              psql -v ON_ERROR_STOP=1 <<SQL
              CREATE SCHEMA IF NOT EXISTS admin AUTHORIZATION CURRENT_USER;
              CREATE SCHEMA IF NOT EXISTS liquibase AUTHORIZATION CURRENT_USER;
              SQL

              echo "Bootstrap OK"
          env:
            # ✅ récupérées dynamiquement via vaultenv (exactement comme ton job liquibase)
            {{- toYaml .Values.liquibase.job.extraEnv | nindent 12 }}

            # ✅ paramètres postgres
            - name: PGHOST
              value: "postgresql.ns-postgresql.svc.cluster.local"
            - name: PGPORT
              value: "5432"
            - name: PGDATABASE
              value: "ibmclouddb"

      # ⚠️ IMPORTANT : il faut aussi activer vaultenv sur CE job
      # (exactement comme tu le fais sur le job liquibase)
      {{- if .Values.hashicorp.enabled }}
      {{- include "common-library.hashicorp.vaultenv" (dict
            "Values" .Values
            "Release" .Release
            "Chart" .Chart
            "Capabilities" .Capabilities
          ) | nindent 6 }}
      {{- end }}