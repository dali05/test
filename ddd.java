args:
  - |
    set -e
    psql -h "$PGHOST" -p "${PGPORT:-5432}" -U "$PGUSER" -d "$PGDATABASE" \
      -v ON_ERROR_STOP=1 \
      -c "CREATE SCHEMA IF NOT EXISTS admin;" \
      -c "CREATE SCHEMA IF NOT EXISTS liquibase;"
env:
  - name: PGHOST
    value: postgresql   # <-- nom du Service k8s PostgreSQL
  - name: PGUSER
    valueFrom: ...
  - name: PGPASSWORD
    valueFrom: ...
  - name: PGDATABASE
    value: mydb
