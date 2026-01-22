java -jar /liquibase/liquibase.jar \
  --url="${PF_LIQUIBASE_COMMAND_URL}" \
  --username="${PF_LIQUIBASE_COMMAND_USERNAME}" \
  --password="${PF_LIQUIBASE_COMMAND_PASSWORD}" \
  executeSql \
  --sql="CREATE SCHEMA IF NOT EXISTS ${LIQUIBASE_DEFAULT_SCHEMA_NAME};"