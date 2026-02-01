{{- if and (.Values.dbBootstrap.hashicorp.enabled) (eq .Values.dbBootstrap.hashicorp.method "vault-agent-initcontainer") }}
{{- $ctx := deepCopy . -}}
{{- $_ := set $ctx.Values "hashicorp" .Values.dbBootstrap.hashicorp -}}
{{ include "common-library.hashicorp.initcontainer.configmap" $ctx }}
{{- end }}

dbBootstrap:
  hashicorp:
    enabled: true
    method: vault-agent-initcontainer
    ns: root
    path: kubernetes_kub00001_local
    approle: ns-wall-e-springboot-local-ap11236-java-application-liquibase
    image: "docker-registry-devops.pf.echonet/hashicorp/vault:1.21"
    template: |
      listener "tcp" {
        address = "127.0.0.1:8200"
        tls_disable = true
      }

      auto_auth {
        method "kubernetes" {
          mount_path = "auth/{{ .Values.dbBootstrap.hashicorp.path }}"
          config = {
            role = "{{ .Values.dbBootstrap.hashicorp.approle }}"
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
        perms = "0600"
        contents = <<EOH
{{- with secret "database/postgres/pg0000000/creds/own_pg0000000_ibmclouddb" -}}
export PGUSER="{{ .Data.username }}"
export PGPASSWORD="{{ .Data.password }}"
{{- end -}}
EOH
      }
