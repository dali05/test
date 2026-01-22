env:
  {{- $vals := merge (deepCopy .Values) (dict "hashicorp" .Values.liquibase.hashicorp) -}}
  {{- $ctx := dict "Values" $vals "Release" .Release "Chart" .Chart "Capabilities" .Capabilities "Template" .Template -}}
  {{- include "common-library.hashicorp.vaultenv" $ctx | nindent 12 }}

  {{- with .Values.liquibase.job.extraEnv }}
  {{- toYaml . | nindent 12 }}
  {{- end }}
