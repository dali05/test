databaseChangeLog:
  - changeSet:
      id: 000-precreate-schemas
      author: you
      runInTransaction: false
      changes:
        - sql:
            splitStatements: false
            stripComments: true
            sql: |
              CREATE SCHEMA IF NOT EXISTS admin;
              CREATE SCHEMA IF NOT EXISTS liquibase;
              GRANT USAGE, CREATE ON SCHEMA admin TO CURRENT_USER;
              GRANT USAGE, CREATE ON SCHEMA liquibase TO CURRENT_USER;
              GRANT USAGE ON SCHEMA public TO CURRENT_USER;
