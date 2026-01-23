apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .Release.Name }}-db-init
data:
  init.sql: |
{{ .Files.Get "sql/init.sql" | indent 4 }}


{{- if .Values.liquibase.job.enabled }}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-db-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never

      imagePullSecrets:
        - name: docker-registry-cred

      containers:
        - name: db-init
          image: postgres:15
          imagePullPolicy: Never

          command: ["sh", "-c"]
          args:
            - |
              echo "=== DB INIT START ==="
              echo "Host: $POSTGRES_HOST"
              echo "DB: $POSTGRES_DATABASE"

              export PGPASSWORD="$PGPASSWORD"

              psql \
                -h "$POSTGRES_HOST" \
                -p "$POSTGRES_PORT" \
                -U "$POSTGRES_USER" \
                -d "$POSTGRES_DATABASE" \
                -f /sql/init.sql

              echo "=== DB INIT DONE ==="

          env:
            - name: POSTGRES_HOST
              value: "postgresql.ns-postgresql.svc.cluster.local"

            - name: POSTGRES_PORT
              value: "5432"

            - name: POSTGRES_DATABASE
              value: "ibmclouddb"

            # 🔐 Injectés par Vault (PAS EN DUR)
            - name: POSTGRES_USER
              value: "vault:database/postgres/pg0000000/creds/own_pg0000000_ibmclouddb#username"

            - name: PGPASSWORD
              value: "vault:database/postgres/pg0000000/creds/own_pg0000000_ibmclouddb#password"

          volumeMounts:
            - name: sql-init
              mountPath: /sql
              readOnly: true

      volumes:
        - name: sql-init
          configMap:
            name: {{ .Release.Name }}-db-init
{{- end }}