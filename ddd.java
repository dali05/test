containers:
  - name: liquibase-bootstrap
    image: ...
    env:
{{ toYaml .Values.liquibase.bootstrap.extraEnv | nindent 6 }}

{{- if .Values.liquibase.hashicorp.enabled }}
{{ include "common-library.hashicorp.vaultenv" (dict
    "Values" .Values.liquibase
    "Release" .Release
    "Chart" .Chart
    "Capabilities" .Capabilities
  ) | nindent 4 }}
{{- end }}