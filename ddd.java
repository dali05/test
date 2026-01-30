{{- $ctx := dict "Values" .Values.liquibase "Chart" .Chart "Release" .Release "Capabilities" .Capabilities -}}

apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "common-library.fullname" . }}-vault-agent-config
  annotations:
    "helm.sh/hook": pre-install, pre-upgrade
    "helm.sh/hook-weight": "-3"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
  labels:
{{ include "common-library.metadata.labels" $ctx | nindent 4 }}
data:
  vault-agent.hcl: |
{{ .Values.liquibase.hashicorp.template | nindent 4 }}