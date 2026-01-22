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






apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include "chart.fullname" . }}-liquibase
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never

      initContainers:
        - name: create-schema
          image: {{ .Values.liquibase.init.image }}
          imagePullPolicy: {{ .Values.liquibase.init.pullPolicy | default "IfNotPresent" }}
          command: ["sh", "-c"]
          args:
            - |
              set -euo pipefail

              echo "Creating schema admin if not exists..."

              # ✅ On récupère user/pass via vault-env, puis on exécute psql
              # On suppose que PF_LIQUIBASE_COMMAND_USERNAME/PASSWORD sont résolus en env vars par vault-env
              # et qu'on a une URL JDBC avec currentSchema=admin (on va l'enlever pour psql)

              # Convertir JDBC -> URL postgresql pour psql
              # jdbc:postgresql://host:5432/db?params  => postgresql://host:5432/db?params
              PGURL="$(echo "$PF_LIQUIBASE_COMMAND_URL" | sed 's/^jdbc:/ /' | tr -d ' ')"

              export PGPASSWORD="$PF_LIQUIBASE_COMMAND_PASSWORD"

              # ⚠️ IMPORTANT : enlever currentSchema=admin pour que la connexion marche même si admin n'existe pas encore
              PGURL="$(echo "$PGURL" | sed 's/[?&]currentSchema=[^&]*//g' | sed 's/?&/?/g' | sed 's/[?]$//g')"

              echo "PGURL=$PGURL"
              psql "$PGURL" -U "$PF_LIQUIBASE_COMMAND_USERNAME" -v ON_ERROR_STOP=1 -c 'CREATE SCHEMA IF NOT EXISTS "admin";'

              echo "Schema admin ready."

          env:
            # on réutilise exactement les mêmes variables que ton conteneur Liquibase
            - name: PF_LIQUIBASE_COMMAND_URL
              value: {{ .Values.liquibase.extraEnv.PF_LIQUIBASE_COMMAND_URL | quote }}
            - name: PF_LIQUIBASE_COMMAND_USERNAME
              value: {{ .Values.liquibase.extraEnv.PF_LIQUIBASE_COMMAND_USERNAME | quote }}
            - name: PF_LIQUIBASE_COMMAND_PASSWORD
              value: {{ .Values.liquibase.extraEnv.PF_LIQUIBASE_COMMAND_PASSWORD | quote }}

          # ✅ Ajouter ici tes annotations/variables vault-env si ton chart les injecte au niveau pod
          # ou directement ici si nécessaire.

      containers:
        - name: liquibase
          image: {{ .Values.liquibase.image.fullName | quote }}
          imagePullPolicy: {{ .Values.liquibase.image.pullPolicy | default "IfNotPresent" }}

          env:
            # Tes variables existantes
            - name: PF_LIQUIBASE_COMMAND_URL
              value: {{ .Values.liquibase.extraEnv.PF_LIQUIBASE_COMMAND_URL | quote }}
            - name: PF_LIQUIBASE_COMMAND_USERNAME
              value: {{ .Values.liquibase.extraEnv.PF_LIQUIBASE_COMMAND_USERNAME | quote }}
            - name: PF_LIQUIBASE_COMMAND_PASSWORD
              value: {{ .Values.liquibase.extraEnv.PF_LIQUIBASE_COMMAND_PASSWORD | quote }}
            - name: PF_LIQUIBASE_COMMAND_CHANGELOG_FILE
              value: {{ .Values.liquibase.extraEnv.PF_LIQUIBASE_COMMAND_CHANGELOG_FILE | quote }}

            # ✅ Liquibase doit écrire dans admin (maintenant qu'il existe)
            - name: LIQUIBASE_DEFAULT_SCHEMA_NAME
              value: "admin"
            - name: LIQUIBASE_LIQUIBASE_SCHEMA_NAME
              value: "admin"

