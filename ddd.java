{{ include "common-library.hashicorp.vaultenv" (dict "Values" .Values.liquibase "Release" .Release "Chart" .Chart "Capabilities" .Capabilities) | nindent 10 }}
