{{- /*
templates/00-vault-agent-configmap.yaml
ConfigMap consommé par le initContainer vault-agent.
Nom attendu dans tes describe pod: wall-e-vault-agent-config
*/ -}}
{{- if and (.Values.dbBootstrap.hashicorp.enabled) (eq .Values.dbBootstrap.hashicorp.method "vault-agent-initcontainer") -}}
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
      address = {{ default "http://vault.ns-vault.svc.cluster.local:8200" .Values.dbBootstrap.hashicorp.vaultAddr | quote }}
      {{- if .Values.dbBootstrap.hashicorp.caCert }}
      ca_cert = {{ .Values.dbBootstrap.hashicorp.caCert | quote }}
      {{- end }}
      {{- if .Values.dbBootstrap.hashicorp.vaultNamespace }}
      namespace = {{ .Values.dbBootstrap.hashicorp.vaultNamespace | quote }}
      {{- end }}
    }

    # ✅ Un seul auto_auth (sinon: at most one "auto_auth" block is allowed)
    auto_auth {
      method "kubernetes" {
        # ex: auth/kubernetes_kub00001_local
        mount_path = {{ printf "auth/%s" (required "dbBootstrap.hashicorp.path est requis" .Values.dbBootstrap.hashicorp.path) | quote }}
        config = {
          role = {{ required "dbBootstrap.hashicorp.approle est requis" .Values.dbBootstrap.hashicorp.approle | quote }}
        }
      }

      sink "file" {
        config = {
          path = "/home/vault/.vault-token"
        }
      }
    }

    # ✅ requis (sinon: no auto_auth, cache, listener block found…)
    cache {
      use_auto_auth_token = true
    }

    # ✅ requis si ton agent active des features qui exigent un listener (api_proxy, etc.)
    listener "tcp" {
      address     = "127.0.0.1:8200"
      tls_disable = true
    }

    template_config {
      exit_on_retry_failure = true
    }

    # Les templates Vault Agent (ce que tu as dans values.yaml -> dbBootstrap.hashicorp.template)
{{- if .Values.dbBootstrap.hashicorp.template }}
{{ .Values.dbBootstrap.hashicorp.template | nindent 4 }}
{{- end }}
{{- end }}