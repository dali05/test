args:
  - |
    set -euo pipefail

    export PGSSLMODE=require

    # Parse JDBC URL: jdbc:postgresql://HOST:PORT/DB
    PGHOST="$(echo "$PF_LIQUIBASE_COMMAND_URL" | sed -E 's#^jdbc:postgresql://([^:/]+).*$#\1#')"
    PGPORT="$(echo "$PF_LIQUIBASE_COMMAND_URL" | sed -E 's#^jdbc:postgresql://[^:/]+:([0-9]+)/.*$#\1#')"
    PGDATABASE="$(echo "$PF_LIQUIBASE_COMMAND_URL" | sed -E 's#^.*/([^/?]+).*$#\1#')"

    PGUSER="$PF_LIQUIBASE_COMMAND_USERNAME"
    export PGPASSWORD="$PF_LIQUIBASE_COMMAND_PASSWORD"

    # Stop si le username n'est pas résolu (cas $username)
    if echo "$PGUSER" | grep -q '\$username'; then
      echo "ERROR: PF_LIQUIBASE_COMMAND_USERNAME is not resolved: $PGUSER"
      echo "Fix Vault env injection (username/password) before running psql."
      exit 1
    fi

    echo "Waiting for Postgres ${PGHOST}:${PGPORT}/${PGDATABASE} (sslmode=$PGSSLMODE)..."
    until psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -c "select 1" >/dev/null 2>&1; do
      sleep 2
    done

    echo "Creating schemas..."
    psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -v ON_ERROR_STOP=1 \
      -c "CREATE SCHEMA IF NOT EXISTS admin;" \
      -c "CREATE SCHEMA IF NOT EXISTS liquibase;"

    echo "DB bootstrap done ✔"