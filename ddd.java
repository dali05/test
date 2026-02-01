{{- /*
  Vault Agent ConfigMap pour le job dbBootstrap
  ⚠️ Important :
  - doit être créé AVANT le Job
  - donc hook weight plus petit que le job (-20 si job = -10)
  - on remappe dbBootstrap.hashicorp -> hashicorp pour la common-library
*/ -}}

{{- if and (.Values.dbBootstrap.hashicorp.enabled) (eq .Values.dbBootstrap.hashicorp.method "vault-agent-initcontainer") }}

{{- $ctx := deepCopy . -}}
{{- $_ := set $ctx.Values "hashicorp" .Values.dbBootstrap.hashicorp -}}

{{ include "common-library.hashicorp.initcontainer.configmap" $ctx | nindent 0 }}

{{- end }}