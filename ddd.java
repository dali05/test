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

      serviceAccountName: {{ include "common-library.serviceAccountName" . }}

      volumes:
        - name: vault-shared
          emptyDir: {}

      initContainers:
        - name: vaultenv-render
          image: {{ .Values.liquibase.job.image.fullName }}   # image qui contient vaultenv (ex: wall-e-sql)
          imagePullPolicy: {{ .Values.liquibase.job.image.pullPolicy }}
          command: ["sh","-c"]
          args:
            - |
              set -e
              # vaultenv remplace vault:... et on écrit un env file lisible par sh
              vaultenv sh -c 'cat <<EOF > /vault/secrets/db.env
              export PGUSER="$PGUSER"
              export PGPASSWORD="$PGPASSWORD"
              EOF'
          env:
            - name: PGUSER
              value: "vault:database/postgres/pg00080000/creds/own_pg00080000_ibmclouddb#username"
            - name: PGPASSWORD
              value: "vault:database/postgres/pg00080000/creds/own_pg00080000_ibmclouddb#password"
{{ include "common-library.hashicorp.vaultenv" (dict "Values" .Values "Release" .Release "Chart" .Chart "Capabilities" .Capabilities ) | nindent 10 }}
          volumeMounts:
            - name: vault-shared
              mountPath: /vault/secrets

      containers:
        - name: db-bootstrap
          image: postgres:15
          imagePullPolicy: IfNotPresent
          command: ["sh","-c"]
          args:
            - |
              set -e
              . /vault/secrets/db.env
              export PGSSLMODE=require   # si besoin, sinon retire
              psql -v ON_ERROR_STOP=1 -h "$PGHOST" -p "$PGPORT" -d "$PGDATABASE" -U "$PGUSER" \
                -c "CREATE SCHEMA IF NOT EXISTS liquibase_meta;"
              psql -v ON_ERROR_STOP=1 -h "$PGHOST" -p "$PGPORT" -d "$PGDATABASE" -U "$PGUSER" \
                -c "CREATE SCHEMA IF NOT EXISTS admin;"
          env:
            - name: PGHOST
              value: postgresql.ns-postgresql.svc.cluster.local
            - name: PGPORT
              value: "5432"
            - name: PGDATABASE
              value: ibmclouddb
          volumeMounts:
            - name: vault-shared
              mountPath: /vault/secrets