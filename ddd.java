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
            # 1) On réutilise les env Liquibase (Vault:...#username/#password, etc.)
{{ toYaml .Values.liquibase.job.extraEnv | nindent 12 }}

            # 2) Mapping explicite des secrets Vault vers les variables comprises par psql
            #    ⚠️ Hypothèse: dans extraEnv, l'index 1 = USERNAME et l'index 2 = PASSWORD
            - name: PGUSER
              value: {{ (index .Values.liquibase.job.extraEnv 1).value | quote }}
            - name: PGPASSWORD
              value: {{ (index .Values.liquibase.job.extraEnv 2).value | quote }}

            # 3) Paramètres de connexion (non secrets)
            - name: PGHOST
              value: "postgresql.ns-postgresql.svc.cluster.local"
            - name: PGPORT
              value: "5432"
            - name: PGDATABASE
              value: "ibmclouddb"

            # 4) Injection Vault (scope liquibase) — évite le nil pointer
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
