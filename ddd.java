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