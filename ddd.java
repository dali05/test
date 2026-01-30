hashicorp:
  enabled: true
  method: vault-agent-initcontainer
  addr: "http://vault.ns-vault.svc.cluster.local:8200"
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
    DB_NAME=<TA_DB>
    EOT
      destination = "/applis/vault/db.env"
    }
