{{- /*
ConfigMap Vault Agent - initContainer method
Génère /etc/vault/vault-agent-config.hcl
*/ -}}

{{- if and (.Values.dbBootstrap.hashicorp.enabled) (eq .Values.dbBootstrap.hashicorp.method "vault-agent-initcontainer") -}}

{{- $fullName := .Release.Name -}}
{{- $cmName := printf "%s-vault-agent-config" $fullName -}}
{{- $vaultAddr := default "http://vault.ns-vault.svc.cluster.local:8200" .Values.dbBootstrap.hashicorp.vaultAddr -}}
{{- $vaultNamespace := default "" .Values.dbBootstrap.hashicorp.vaultNamespace -}}
{{- $k8sAuthMount := default "auth/kubernetes" .Values.dbBootstrap.hashicorp.path -}}
{{- $role := required "dbBootstrap.hashicorp.approle (role Vault Kubernetes auth) est requis" .Values.dbBootstrap.hashicorp.approle -}}

apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ $cmName }}
  namespace: {{ .Release.Namespace }}
  labels:
    app.kubernetes.io/name: {{ .Chart.Name }}
    app.kubernetes.io/instance: {{ .Release.Name }}
    app.kubernetes.io/managed-by: Helm
    helm.sh/chart: "{{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}"
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-delete-policy": before-hook-creation
    "helm.sh/hook-weight": "-20"
data:
  vault-agent-config.hcl: |
    exit_after_auth = true
    pid_file = "/home/vault/pidfile"

    vault {
      address = "{{ $vaultAddr }}"
      {{- if $vaultNamespace }}
      namespace = "{{ $vaultNamespace }}"
      {{- end }}
    }

    auto_auth {
      method "kubernetes" {
        mount_path = "{{ $k8sAuthMount }}"
        config = {
          role = "{{ $role }}"
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
      address = "127.0.0.1:8200"
      tls_disable = true
    }

    api_proxy {
      use_auto_auth_token = true
    }

    {{- /* Templates Vault Agent (uniquement des blocks template {}) */}}
    {{- if .Values.dbBootstrap.hashicorp.template }}
    {{- range $idx, $tpl := .Values.dbBootstrap.hashicorp.template }}
    template {
      destination = "{{ required "template.destination requis" $tpl.destination }}"
      perms       = "{{ default "0600" $tpl.perms }}"
      contents = <<EOH
{{ $tpl.contents | nindent 0 }}
EOH
    }
    {{- end }}
    {{- end }}

{{- end -}}