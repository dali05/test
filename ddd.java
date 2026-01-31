dbBootstrap:
  enabled: true
  image: postgres:15

  env:
    PGHOST: "postgresql.ns-postgresql.svc.cluster.local"
    PGPORT: "5432"
    PGDATABASE: "ibmclouddb"

  hashicorp:
    enabled: true
    method: vault-agent-initcontainer
    ns: "root"
    path: "kubernetes_kub00001_local"
    approle: "ns-wall-e-springboot-local-ap11236-java-application-liquibase"
    template: |
      template {
        destination = "/etc/secrets/pg.env"
        perms       = "0600"
        contents = <<EOH
{{- with secret "database/postgres/pg0000000/creds/own_pg0000000_ibmclouddb" -}}
export PGUSER="{{ .Data.username }}"
export PGPASSWORD="{{ .Data.password }}"
{{- end -}}
EOH
      }



{{- if .Values.dbBootstrap.enabled }}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-db-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-10"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never

      {{- /* Vault agent initcontainer (vos conventions) */}}
      {{- with .Values.dbBootstrap.hashicorp }}
      initContainers:
        {{- include "common-library.hashicorp.initcontainer" (dict "Values" (dict "hashicorp" .) "Release" $.Release "Chart" $.Chart) | nindent 8 }}
      {{- end }}

      containers:
        - name: db-bootstrap
          image: {{ .Values.dbBootstrap.image | quote }}
          command: ["sh","-c"]
          args:
            - |
              set -e
              . /etc/secrets/pg.env
              psql -v ON_ERROR_STOP=1 \
                -h "$PGHOST" \
                -p "$PGPORT" \
                -d "$PGDATABASE" \
                -c "CREATE SCHEMA IF NOT EXISTS liquibase; CREATE SCHEMA IF NOT EXISTS admin;"

          env:
            - name: PGHOST
              value: {{ .Values.dbBootstrap.env.PGHOST | quote }}
            - name: PGPORT
              value: {{ .Values.dbBootstrap.env.PGPORT | quote }}
            - name: PGDATABASE
              value: {{ .Values.dbBootstrap.env.PGDATABASE | quote }}

          volumeMounts:
            - name: vault-shared-data
              mountPath: /etc/secrets

      {{- with .Values.dbBootstrap.hashicorp }}
      volumes:
        {{- include "common-library.hashicorp.initcontainer.volumes" (dict "Values" (dict "hashicorp" .) "Release" $.Release "Chart" $.Chart) | nindent 8 }}
      {{- end }}
{{- end }}
