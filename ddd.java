extraEnv:
  - name: PF_LIQUIBASE_COMMAND_URL
    value: "jdbc:postgresql://postgresql.ns-postgresql.svc.cluster.local:5432/ibmclouddb?currentSchema=admin"

  - name: PF_LIQUIBASE_COMMAND_USERNAME
    value: "vault:database/postgres/...#username"

  - name: PF_LIQUIBASE_COMMAND_PASSWORD
    value: "vault:database/postgres/...#password"

  - name: PF_LIQUIBASE_COMMAND_CHANGELOG_FILE
    value: db/changelog/db.changelog-master.yaml

  # 🔴 MANQUAIT ICI 🔴
  - name: LIQUIBASE_DEFAULT_SCHEMA_NAME
    value: admin

  - name: LIQUIBASE_LIQUIBASE_SCHEMA_NAME
    value: admin