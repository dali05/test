apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include "common-library.fullName" . }}-db-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-10"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: db-bootstrap
          image: postgres:15
          imagePullPolicy: IfNotPresent
          command: ["/bin/sh","-c"]
          args:
            - |
              set -e
              echo "=== Bootstrap schema start ==="

              echo "PGHOST=$PGHOST"
              echo "PGPORT=$PGPORT"
              echo "PGDATABASE=$PGDATABASE"
              echo "PGUSER=${PGUSER:+SET}"
              echo "PGPASSWORD=${PGPASSWORD:+SET}"

              psql -v ON_ERROR_STOP=1 -f /sql/init.sql

              echo "=== Bootstrap schema done ==="

          env:
            # 🔁 Réutilisation EXACTE des env Liquibase (Vault inclus)
{{ toYaml .Values.liquibase.job.extraEnv | nindent 12 }}

            # ➕ Variables standard reconnues par psql
            - name: PGHOST
              value: "postgresql.ns-postgresql.svc.cluster.local"
            - name: PGPORT
              value: "5432"
            - name: PGDATABASE
              value: "ibmclouddb"

{{- if .Values.liquibase.hashicorp }}
{{ include "common-library.hashicorp.vaultenv" (dict
    "Values" .Values.liquibase
    "Release" .Release
    "Chart" .Chart
    "Capabilities" .Capabilities
  ) | nindent 12 }}
{{- end }}

          volumeMounts:
            - name: sql
              mountPath: /sql

      volumes:
        - name: sql
          configMap:
            name: {{ include "common-library.fullName" . }}-db-init