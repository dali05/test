{{- $vals := merge (deepCopy .Values) (dict
    "hashicorp" .Values.liquibase.hashicorp
    "serviceAccount" .Values.liquibase.serviceAccount
    "selectors" .Values.liquibase.selectors
  ) -}}
{{- $ctx := dict
    "Values" $vals
    "Release" .Release
    "Chart" .Chart
    "Capabilities" .Capabilities
    "Template" .Template
  -}}
{{- include "common-library.hashicorp.vaultenv" $ctx | nindent 12 }}
