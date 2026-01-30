liquibase:
  hashicorp:
    enabled: true
    # On reste en vaultenv pour liquibase job normal, pas besoin de toucher
    method: vaultenv

    # MAIS il faut que "template" soit défini car vault-agent-initcontainer en a besoin
    template: |
      template {
        contents = <<EOT
      DB_USER={{ with secret "database/postgres/pg000000/creds/app_pg000000_ibmclouddb" }}{{ .Data.username }}{{ end }}
      DB_PASSWORD={{ with secret "database/postgres/pg000000/creds/app_pg000000_ibmclouddb" }}{{ .Data.password }}{{ end }}
      DB_HOST=postgresql.ns-postgresql.svc.cluster.local
      DB_PORT=5432
      DB_NAME=xxxx
      EOT
        destination = "/applis/vault/db.env"
      }