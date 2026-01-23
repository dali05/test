env:
  - name: PGHOST
    value: "postgresql.ns-postgresql.svc.cluster.local"
  - name: PGPORT
    value: "5432"
  - name: PGDATABASE
    value: "ibmclouddb"
  - name: PGUSER
    value: "vault:database/postgres/pg0000000/creds/own_pg0000000_ibmclouddb#username"
  - name: PGPASSWORD
    value: "vault:database/postgres/pg0000000/creds/own_pg0000000_ibmclouddb#password"
{{ include "common-library.hashicorp.vaultenv" (dict "Values" .Values.liquibase "Release" .Release "Chart" .Chart "Capabilities" .Capabilities) | nindent 2 }}