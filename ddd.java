{{- if and (.Values.hashicorp.enabled) (eq .Values.hashicorp.method "vault-agent-initcontainer") }}
{{ include "common-library.hashicorp.initcontainer.configmap" . }}
{{- end }}