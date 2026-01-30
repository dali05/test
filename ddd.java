liquibase:
  hashicorp:
    enabled: true
    method: vault-agent-initcontainer
    addr: "http://vault.ns-vault.svc.cluster.local:8200"
    ns: ""           # si vous utilisez Vault namespace, sinon vide
    # role: parfois dans approle, parfois autre, selon votre lib
    template: |
      exit_after_auth = true
      pid_file = "/home/vault/pidfile"

      auto_auth {
        method "kubernetes" {
          mount_path = "auth/kubernetes"
          config = {
            role = "ns-wall-e-springboot-local-ap11236-java-application-liquibase"
          }
        }
        sink "file" {
          config = {
            path = "/home/vault/.token"
          }
        }
      }

      template {
        contents = <<EOT
      DB_USER={{ with secret "database/postgres/pg000000/creds/app_pg000000_ibmclouddb" }}{{ .Data.username }}{{ end }}
      DB_PASSWORD={{ with secret "database/postgres/pg000000/creds/app_pg000000_ibmclouddb" }}{{ .Data.password }}{{ end }}
      DB_HOST=postgresql.ns-postgresql.svc.cluster.local
      DB_PORT=5432
      DB_NAME=<TON_DB>
      EOT
        destination = "/applis/vault/db.env"
      }


. /applis/vault/db.env
export PGPASSWORD="$DB_PASSWORD"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -c 'CREATE SCHEMA IF NOT EXISTS liquibase;'