data:
  vault-agent-config.hcl: |
    vault {
      address   = "{{ .Values.hashicorp.addr }}"
      namespace = "{{ .Values.hashicorp.ns }}"
    }

    auto_auth {
      method "kubernetes" {
        mount_path = "auth/{{ .Values.hashicorp.path }}"
        config = {
          role = "{{ .Values.hashicorp.approle }}"
        }
      }

      sink "file" {
        config = { path = "/home/vault/.vault-token" }
      }
    }

    template {
      destination = "/etc/secrets/pg.env"
      perms       = "0600"
      contents = <<EOH
{{`{{- with secret "database/postgres/pg0000000/creds/own_pg0000000_ibmclouddb" -}}`}}
export PGUSER="{{`{{ .Data.username }}`}}"
export PGPASSWORD="{{`{{ .Data.password }}`}}"
{{`{{- end -}}`}}
EOH
    }