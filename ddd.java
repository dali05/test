apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-db-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-30"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      serviceAccountName: local-ap11236-java-application-liquibase
      restartPolicy: Never
      containers:
        - name: db-bootstrap
          image: postgres:15
          imagePullPolicy: IfNotPresent
          command: ["sh", "-c"]
          args:
            - |
              set -e
              psql -c "CREATE SCHEMA IF NOT EXISTS liquibase_meta;"
              psql -c "CREATE SCHEMA IF NOT EXISTS admin;"
          env:
            - name: PGHOST
              value: postgresql.ns-postgresql.svc.cluster.local
            - name: PGPORT
              value: "5432"
            - name: PGDATABASE
              value: ibmclouddb
            - name: PGUSER
              value: vault:database/postgres/pg00080000/creds/own_pg00080000_ibmclouddb#username
            - name: PGPASSWORD
              value: vault:database/postgres/pg00080000/creds/own_pg00080000_ibmclouddb#password

{{ include "common-library.hashicorp.vaultenv" (dict
    "Values" .Values
    "Release" .Release
    "Chart" .Chart
    "Capabilities" .Capabilities
) | nindent 10 }}