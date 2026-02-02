args:
  - |
    set -e

    echo "Waiting for Vault-rendered /etc/secrets/pg.env..."
    i=0
    while [ ! -s /etc/secrets/pg.env ] && [ $i -lt 120 ]; do
      i=$((i+1))
      sleep 1
    done

    if [ ! -s /etc/secrets/pg.env ]; then
      echo "ERROR: /etc/secrets/pg.env missing or empty"
      ls -la /etc/secrets || true
      exit 1
    fi

    echo "OK: pg.env exists. Loading it..."
    set -a
    . /etc/secrets/pg.env
    set +a

    echo "PGUSER=$PGUSER"
    echo "PGPASSWORD is set? $([ -n "$PGPASSWORD" ] && echo yes || echo no)"

    # exemple psql
    export PGPASSWORD="$PGPASSWORD"
    psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
      -c "CREATE SCHEMA IF NOT EXISTS admin; CREATE SCHEMA IF NOT EXISTS liquibase;"