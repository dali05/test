{{- $ctx := deepCopy $ -}}
{{- $_ := set $ctx.Values "configmap" (dict "enabled" false) -}}

initContainers:
  {{- include "common-library.hashicorp.initcontainer" $ctx | nindent 8 }}

...

volumes:
  {{- include "common-library.hashicorp.initcontainer.volumes" $ctx | nindent 8 }}