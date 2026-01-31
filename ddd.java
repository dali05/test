set -euo pipefail

DB_HOST="$(echo "$PF_LIQUIBASE_COMMAND_URL" | sed -E 's|^jdbc:postgresql://([^:/]+).*|\1|')"
DB_PORT="$(echo "$PF_LIQUIBASE_COMMAND_URL" | sed -E 's|.*:([0-9]+)/.*|\1|')"
DB_NAME="$(echo "$PF_LIQUIBASE_COMMAND_URL" | sed -E 's|.*/([^/?]+).*|\1|')"

export PGPASSWORD="$PF_LIQUIBASE_COMMAND_PASSWORD"

psql -h "$DB_HOST" -p "$DB_PORT" -U "$PF_LIQUIBASE_COMMAND_USERNAME" -d "$DB_NAME" \
  -c "CREATE SCHEMA IF NOT EXISTS liquibase;"