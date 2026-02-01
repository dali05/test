{{- /*
templates/00-vault-agent-configmap.yaml

But:
- créer le ConfigMap "wall-e-vault-agent-config" pour le Job dbBootstrap
- en réutilisant le template de la common-library
*/ -}}

{{- if and (.Values.dbBootstrap.enabled)
          (.Values.dbBootstrap.hashicorp.enabled)
          (eq .Values.dbBootstrap.hashicorp.method "vault-agent-initcontainer") }}

{{- $ctx := deepCopy . -}}
{{- /* IMPORTANT: la common-library lit .Values.hashicorp, pas .Values.dbBootstrap.hashicorp */ -}}
{{- $_ := set $ctx.Values "hashicorp" .Values.dbBootstrap.hashicorp -}}

{{- /* (Optionnel) si tu ne veux pas monter un configmap "properties" dans vault-agent */ -}}
{{- $_ := set $ctx.Values "configmap" (dict "enabled" false) -}}

{{- /* Hook: créé avant le Job (le Job est à -10) */ -}}
{{- /* Si ta common-library gère déjà les hooks, tu peux supprimer ces annotations.
      Sinon, on force ici le hook. */ -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "common-library.fullname" $ctx }}-vault-agent-config
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-20"
    "helm.sh/hook-delete-policy": before-hook-creation
  labels:
{{ include "common-library.metadata.labels" $ctx | nindent 4 }}
data:
  vault-agent-config.hcl: |
    exit_after_auth = true
    pid_file = "/home/vault/pidfile"
    auto_auth {
      method "kubernetes" {
        mount_path = "auth/{{ printf "%v" $ctx.Values.hashicorp.path }}"
        config = {
          role = "{{ default (printf "%s-%s" $ctx.Release.Namespace (include "common-library.serviceAccountName" $ctx)) $ctx.Values.hashicorp.approle }}"
        }
      }
      sink "file" {
        config = {
          path = "/home/vault/.vault-token"
        }
      }
    }
{{ $ctx.Values.hashicorp.template | indent 4 }}

{{- end }}