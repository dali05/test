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
              echo "=== Bootstrap schema start ==="
              echo "PGHOST=$PGHOST"
              echo "PGDATABASE=$PGDATABASE"
              echo "PGUSER=$PGUSER"
              psql -v ON_ERROR_STOP=1 <<SQL
              CREATE SCHEMA IF NOT EXISTS admin AUTHORIZATION CURRENT_USER;
              CREATE SCHEMA IF NOT EXISTS liquibase AUTHORIZATION CURRENT_USER;
              SQL
              echo "=== Bootstrap schema done ==="
          env:
            # vars postgres
            - name: PGHOST
              value: "postgresql.ns-postgresql.svc.cluster.local"
            - name: PGPORT
              value: "5432"
            - name: PGDATABASE
              value: "ibmclouddb"

            # mapping liquibase -> psql (valeurs résolues par vaultenv)
            - name: PGUSER
              value: "$(PF_LIQUIBASE_COMMAND_USERNAME)"
            - name: PGPASSWORD
              value: "$(PF_LIQUIBASE_COMMAND_PASSWORD)"

            # on garde aussi les env liquibase (vault refs)
            {{- toYaml .Values.liquibase.job.extraEnv | nindent 12 }}

      {{- include "common-library.hashicorp.vaultenv" (dict
            "Values" .Values.liquibase
            "Release" .Release
            "Chart" .Chart
            "Capabilities" .Capabilities
          ) | nindent 6 }}