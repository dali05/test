{{- if and (.Values.hashicorp.enabled) (eq .Values.hashicorp.method "vault-agent-initcontainer") }}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "common-library.fullname" . }}-vault-agent-config
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-20"
    "helm.sh/hook-delete-policy": before-hook-creation
data:
  vault-agent-config.hcl: |
{{- /* On injecte le contenu généré par le helper dans la clé data */ -}}
{{- /* Si ton helper crée déjà un ConfigMap complet, alors utilise Option 2 ci-dessous */ -}}
{{- end }}