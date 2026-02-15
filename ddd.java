databaseChangeLog:

  # Active gen_random_uuid() (pgcrypto)
  - changeSet:
      id: qeaa-seed-000-enable-pgcrypto
      author: walle
      changes:
        - sql:
            splitStatements: false
            sql: |
              CREATE EXTENSION IF NOT EXISTS pgcrypto;

  # Seed QEAAType + QEAAData (idempotent by name)
  - changeSet:
      id: qeaa-seed-001-types-and-data
      author: walle
      changes:
        - sql:
            splitStatements: false
            sql: |
              -- QEAAType
              INSERT INTO qeaa_type (id, name, created_at, updated_at)
              SELECT gen_random_uuid(), v.name, now(), now()
              FROM (VALUES
                ('PID'),
                ('Diploma'),
                ('Driver LICENCE')
              ) AS v(name)
              WHERE NOT EXISTS (
                SELECT 1 FROM qeaa_type t WHERE t.name = v.name
              );

              -- QEAAData
              INSERT INTO qeaa_data (id, name, created_at, updated_at)
              SELECT gen_random_uuid(), v.name, now(), now()
              FROM (VALUES
                ('given_name'),
                ('family_name'),
                ('birth_date'),
                ('place_of_birth'),
                ('nationality'),
                ('personal_administrative_number'),
                ('tax_id_code'),
                ('driver_licence_number')
              ) AS v(name)
              WHERE NOT EXISTS (
                SELECT 1 FROM qeaa_data d WHERE d.name = v.name
              );

  # Seed QEAADataset (idempotent by (dataset_name,type,data))
  - changeSet:
      id: qeaa-seed-002-datasets
      author: walle
      changes:
        - sql:
            splitStatements: false
            sql: |
              WITH
              type_pid AS (SELECT id AS type_id FROM qeaa_type WHERE name = 'PID'),
              rows_to_insert AS (
                SELECT 'FRPID'::text AS dataset_name, 'given_name'::text AS data_name
                UNION ALL SELECT 'FRPID', 'family_name'
                UNION ALL SELECT 'FRPID', 'birth_date'
                UNION ALL SELECT 'FRPID', 'place_of_birth'
                UNION ALL SELECT 'FRPID', 'nationality'
                UNION ALL SELECT 'FRPID', 'personal_administrative_number'

                UNION ALL SELECT 'SPPID', 'given_name'
                UNION ALL SELECT 'SPPID', 'family_name'
                UNION ALL SELECT 'SPPID', 'birth_date'
                UNION ALL SELECT 'SPPID', 'place_of_birth'
                UNION ALL SELECT 'SPPID', 'nationality'
                UNION ALL SELECT 'SPPID', 'personal_administrative_number'
                UNION ALL SELECT 'SPPID', 'tax_id_code'
              )
              INSERT INTO qeaa_dataset (id, name, type_id, data_id, created_at, updated_at)
              SELECT
                gen_random_uuid(),
                r.dataset_name,
                tp.type_id,
                d.id,
                now(), now()
              FROM rows_to_insert r
              CROSS JOIN type_pid tp
              JOIN qeaa_data d ON d.name = r.data_name
              WHERE NOT EXISTS (
                SELECT 1
                FROM qeaa_dataset ds
                WHERE ds.name = r.dataset_name
                  AND ds.type_id = tp.type_id
                  AND ds.data_id = d.id
              );


databaseChangeLog:
  - include:
      file: db/changelog/db.changelog-qeaa.yaml
  - include:
      file: db/changelog/db.changelog-qeaa-seed.yaml


spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml