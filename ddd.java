initContainers:
  {{- include "common-library.hashicorp.initcontainer" . | nindent 8 }}

volumes:
  {{- include "common-library.hashicorp.initcontainer.volumes" . | nindent 8 }}