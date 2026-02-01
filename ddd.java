{{- if and (.Values.dbBootstrap.hashicorp.enabled) (eq .Values.dbBootstrap.hashicorp.method "vault-agent-initcontainer") -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .Release.Name }}-vault-agent-config
  namespace: {{ .Release.Namespace }}
  labels:
    app.kubernetes.io/name: {{ .Chart.Name }}
    app.kubernetes.io/instance: {{ .Release.Name }}
data:
  vault-agent-config.hcl: |-
    exit_after_auth = true
    pid_file        = "/home/vault/pidfile"

    vault {
      address = "{{ required "dbBootstrap.hashicorp.vaultAddr is required" .Values.dbBootstrap.hashicorp.vaultAddr }}"
    }

    auto_auth {
      method "kubernetes" {
        mount_path = "{{ required "dbBootstrap.hashicorp.path is required" .Values.dbBootstrap.hashicorp.path }}"
        config = {
          role = "{{ required "dbBootstrap.hashicorp.approle is required" .Values.dbBootstrap.hashicorp.approle }}"
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

    listener "tcp" {
      address     = "127.0.0.1:8200"
      tls_disable = true
    }

    template_config {
      exit_on_retry_failure = true
    }

    {{- $templates := .Values.dbBootstrap.hashicorp.template | default (list) -}}
    {{- range $i, $tpl := $templates }}
    template {
      destination = "{{ required (printf "dbBootstrap.hashicorp.template[%d].destination is required" $i) $tpl.destination }}"
      perms       = "{{ $tpl.perms | default "0600" }}"
      contents = <<EOH
{{- required (printf "dbBootstrap.hashicorp.template[%d].contents is required" $i) $tpl.contents | nindent 4 }}
EOH
    }
    {{- end }}
{{- end }}