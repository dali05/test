args:
            - |
              set -e
              echo "=== DB Bootstrap start ==="

              echo ">>> Create schema admin if not exists (via liquibase executeSql)"
              vaultenv liquibase \
                --url="$PF_LIQUIBASE_COMMAND_URL" \
                --username="$PF_LIQUIBASE_COMMAND_USERNAME" \
                --password="$PF_LIQUIBASE_COMMAND_PASSWORD" \
                executeSql --sql="CREATE SCHEMA IF NOT EXISTS admin;"

              echo ">>> Run liquibase update"
              vaultenv liquibase \
                --url="$PF_LIQUIBASE_COMMAND_URL" \
                --username="$PF_LIQUIBASE_COMMAND_USERNAME" \
                --password="$PF_LIQUIBASE_COMMAND_PASSWORD" \
                --changeLogFile="$PF_LIQUIBASE_COMMAND_CHANGELOG_FILE" \
                update

              echo "=== DB Bootstrap done ==="