{{- /*
templates/00-vault-agent-configmap.yaml
Crée le ConfigMap "wall-e-vault-agent-config" qui contient vault-agent-config.hcl
*/ -}}
{{- if and (.Values.dbBootstrap.enabled) (.Values.dbBootstrap.hashicorp.enabled) (eq .Values.dbBootstrap.hashicorp.method "vault-agent-initcontainer") -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: wall-e-vault-agent-config
  namespace: {{ .Release.Namespace }}
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
    "helm.sh/hook-weight": "-20"
data:
  vault-agent-config.hcl: |
    exit_after_auth = true
    pid_file = "/home/vault/pidfile"

    vault {
      address = "{{ .Values.dbBootstrap.hashicorp.vaultAddr }}"
      {{- if .Values.dbBootstrap.hashicorp.caCert }}
      ca_cert = "{{ .Values.dbBootstrap.hashicorp.caCert }}"
      {{- end }}
    }

    auto_auth {
      method "kubernetes" {
        mount_path = "auth/{{ .Values.dbBootstrap.hashicorp.authPath }}"
        config = {
          role = "{{ .Values.dbBootstrap.hashicorp.role }}"
        }
      }

      sink "file" {
        config = {
          path = "/home/vault/.vault-token"
        }
      }
    }

    cache {
      use_auto_auth_token = true
    }

    template_config {
      exit_on_retry_failure = true
    }

    template {
      destination = "{{ .Values.dbBootstrap.hashicorp.destination }}"
      perms       = "0600"
      contents = <<EOH
{{`{{- with secret "`}}{{ .Values.dbBootstrap.hashicorp.secretPath }}{{`" -}}`}}
export PGUSER="{{`{{ .Data.username }}`}}"
export PGPASSWORD="{{`{{ .Data.password }}`}}"
{{`{{- end -}}`}}
EOH
    }
{{- end -}}

secretPath: "database/postgres/pg0000000/creds/own_pg0000000_ibmclouddb"

    # Fichier généré dans le shared volume
    destination: "/etc/secrets/pg.env"
