configmap:
  enabled: false

hashicorp:
  enabled: true
  method: vault-agent-initcontainer
  ns: root
  path: kubernetes_kub00001_local
  approle: ns-wall-e-springboot-local-ap11236-java-application-liquibase
  template: |
    template {
      destination = "/etc/secrets/pg.env"
      perms       = "0600"
      contents = <<EOH
{{- with secret "database/postgres/pg0000000/creds/own_pg0000000_ibmclouddb" -}}
export PGUSER="{{ .Data.username }}"
export PGPASSWORD="{{ .Data.password }}"
{{- end -}}
EOH
    }