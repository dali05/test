apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include "common-library.fullName" . }}-db-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-10"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: psql
          image: postgres:15
          command: ["/bin/sh","-c"]
          args:
            - |
              set -e
              echo "Bootstrap schema..."
              psql "host=$DB_HOST port=$DB_PORT dbname=$DB_NAME user=$DB_USER password=$DB_PASSWORD sslmode=require" \
                -v ON_ERROR_STOP=1 -f /sql/init.sql
          env:
            # ici tes env viennent de Vault (via vaultenv include)
            # DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
{{ include "common-library.hashicorp.vaultenv" . | nindent 10 }}
          volumeMounts:
            - name: sql
              mountPath: /sql
      volumes:
        - name: sql
          configMap:
            name: {{ include "common-library.fullName" . }}-db-init
