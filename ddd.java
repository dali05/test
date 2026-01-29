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

      {{- /* ✅ vaultenv DOIT être ici, PAS dans metadata.annotations */}}
      {{- if .Values.liquibase.hashicorp.enabled }}
      {{- include "common-library.hashicorp.vaultenv" (dict
            "Values" .Values.liquibase
            "Release" .Release
            "Chart" .Chart
            "Capabilities" .Capabilities
          ) | nindent 6 }}
      {{- end }}

      containers:
        - name: db-bootstrap
          image: postgres:15
          imagePullPolicy: IfNotPresent
          command: ["sh", "-c"]
          args:
            - |
              set -e
              echo "=== Bootstrap schema start ==="

              # Vault injecte ces variables
              export PGUSER="${PF_LIQUIBASE_COMMAND_USERNAME}"
              export PGPASSWORD="${PF_LIQUIBASE_COMMAND_PASSWORD}"

              echo "PGHOST=$PGHOST"
              echo "PGPORT=$PGPORT"
              echo "PGDATABASE=$PGDATABASE"
              echo "PGUSER=$PGUSER"

              psql -v ON_ERROR_STOP=1 <<SQL
              CREATE SCHEMA IF NOT EXISTS admin AUTHORIZATION CURRENT_USER;
              CREATE SCHEMA IF NOT EXISTS liquibase AUTHORIZATION CURRENT_USER;
              SQL

              echo "=== Bootstrap schema done ==="

          env:
            # ♻️ Réutilisation EXACTE des env Liquibase (Vault inclus)
            {{- toYaml .Values.liquibase.job.extraEnv | nindent 12 }}

            # Variables standard Postgres
            - name: PGHOST
              value: "postgresql.ns-postgresql.svc.cluster.local"
            - name: PGPORT
              value: "5432"
            - name: PGDATABASE
              value: "ibmclouddb"