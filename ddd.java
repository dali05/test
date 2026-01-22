liquibase:
  job:
    # ✅ AJOUTE CETTE PARTIE
    initContainers:
      - name: create-admin-schema
        image:
          # Reprends la même image que ton job si elle contient psql
          # (sinon il faut une image "toolbox" qui a psql + vault-env)
          fullName: wall-e-sql
          pullPolicy: Never

        command: ["sh", "-c"]
        args:
          - |
            set -euo pipefail
            echo "[init] Create schema admin if not exists"

            # PF_LIQUIBASE_COMMAND_URL est en jdbc:postgresql://...
            # psql ne comprend pas "jdbc:" => on enlève le préfixe
            PGURL="${PF_LIQUIBASE_COMMAND_URL#jdbc:}"

            # Pour l'init : on enlève currentSchema=admin pour ne pas dépendre du schéma
            PGURL="$(echo "$PGURL" | sed 's/[?&]currentSchema=[^&]*//g' | sed 's/?&/?/g' | sed 's/[?]$//g')"

            export PGPASSWORD="$PF_LIQUIBASE_COMMAND_PASSWORD"

            echo "[init] PGURL=$PGURL"
            psql "$PGURL" -U "$PF_LIQUIBASE_COMMAND_USERNAME" -v ON_ERROR_STOP=1 \
              -c 'CREATE SCHEMA IF NOT EXISTS "admin";'

            echo "[init] Schema admin ready"

        env:
          - name: PF_LIQUIBASE_COMMAND_URL
            value: "jdbc:postgresql://postgresql.ns-postgresql.svc.cluster.local:5432/ibmclouddb?currentSchema=admin"
          - name: PF_LIQUIBASE_COMMAND_USERNAME
            value: "vault:database/postgres/pg0000000/creds/own_pg0000000_ibmclouddb#username"
          - name: PF_LIQUIBASE_COMMAND_PASSWORD
            value: "vault:database/postgres/pg0000000/creds/own_pg0000000_ibmclouddb#password"