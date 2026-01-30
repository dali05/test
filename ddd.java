job:
  initContainers:
    enabled: true
    containers:
      - name: init-liquibase-schema
        image: "<LA_MEME_IMAGE_QUE_LIQUIBASE>"
        command: ["sh","-c"]
        args:
          - >
            psql "$PF_LIQUIBASE_COMMAND_URL"
            -U "$PF_LIQUIBASE_COMMAND_USERNAME"
            -d "$PF_LIQUIBASE_COMMAND_DATABASE"
            -c "CREATE SCHEMA IF NOT EXISTS liquibase;"
        env:
          # Reprendre les mêmes variables que le container Liquibase
          - name: PF_LIQUIBASE_COMMAND_URL
            value: "jdbc:postgresql://...."   # ou ta valeur existante
          - name: PF_LIQUIBASE_COMMAND_USERNAME
            value: "omm_...@username"         # vaultenv va résoudre
          - name: PF_LIQUIBASE_COMMAND_PASSWORD
            value: "omm_...@password"         # vaultenv va résoudre
          - name: PF_LIQUIBASE_COMMAND_DATABASE
            value: "..."                      # si vous l’avez
