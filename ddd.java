{{- if and (.Values.dbBootstrap.enabled) (.Values.dbBootstrap.hashicorp.enabled) (eq .Values.dbBootstrap.hashicorp.method "vault-agent-initcontainer") }}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .Release.Name }}-vault-agent-config
  namespace: {{ .Release.Namespace }}
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-delete-policy": before-hook-creation
    "helm.sh/hook-weight": "-20"
data:
  vault-agent-config.hcl: |
    exit_after_auth = true
    pid_file        = "/home/vault/pidfile"

    vault {
      address = {{ .Values.dbBootstrap.hashicorp.vaultAddr | quote }}
    }

    # 1 seul auto_auth
    auto_auth {
      method "kubernetes" {
        mount_path = "auth/{{ .Values.dbBootstrap.hashicorp.path }}"
        config = {
          role = {{ .Values.dbBootstrap.hashicorp.approle | quote }}
        }
      }

      sink "file" {
        config = {
          path = "/home/vault/.vault-token"
        }
      }
    }

    # requis pour éviter "listener missing"
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

    {{- range $i, $tpl := .Values.dbBootstrap.hashicorp.template }}
    template {
      destination = {{ $tpl.destination | quote }}
      perms       = {{ default "0600" $tpl.perms | quote }}
      contents    = <<EOH
{{ $tpl.contents | nindent 0 }}
EOH
    }
    {{- end }}
{{- end }}