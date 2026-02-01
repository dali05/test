
apiVersion: v1
kind: ConfigMap
metadata:
  name: wall-e-vault-agent-config
  namespace: ns-wall-e-springboot
data:
  vault-agent-config.hcl: |
    exit_after_auth = true
    pid_file = "/home/vault/pidfile"

    vault {
      address = "http://vault.ns-vault.svc.cluster.local:8200"
    }

    auto_auth {
      method "kubernetes" {
        mount_path = "auth/kubernetes_kub0001_local"
        config = {
          role = "ns-wall-e-springboot-local-ap11236-java-application-liquibase"
        }
      }

      sink "file" {
        config = {
          path = "/home/vault/.vault-token"
        }
      }
    }

    template {
      destination = "/etc/secrets/pg.env"
      perms       = "0600"
      contents    = <<EOF
{{ with secret "database/postgres/pg000000/creds/own_pg000000_ibmcloudb" }}
export PGUSER="{{ .Data.username }}"
export PGPASSWORD="{{ .Data.password }}"
{{ end }}
EOF
    }