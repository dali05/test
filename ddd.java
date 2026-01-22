echo "Creating schema '${LIQUIBASE_DEFAULT_SCHEMA_NAME}' if not exists (via liquibase executeSql)..."
liquibase \
  --url="${PF_LIQUIBASE_COMMAND_URL}" \
  --username="${PF_LIQUIBASE_COMMAND_USERNAME}" \
  --password="${PF_LIQUIBASE_COMMAND_PASSWORD}" \
  executeSql --sql="CREATE SCHEMA IF NOT EXISTS ${LIQUIBASE_DEFAULT_SCHEMA_NAME};"