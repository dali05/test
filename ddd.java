apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include "common-library.fullname" . }}-liquibase-schema
  annotations:
    "helm.sh/hook": pre-install, pre-upgrade
    "helm.sh/hook-weight": "-2"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: create-schema
          image: postgres:15-alpine
          command: ["sh","-c"]
          args:
            - |
              set -e
              echo "Creating schema liquibase if not exists"
              export PGPASSWORD="$DB_PASSWORD"
              psql \
                -h "$DB_HOST" \
                -p "$DB_PORT" \
                -U "$DB_USER" \
                -d "$DB_NAME" \
                -c "CREATE SCHEMA IF NOT EXISTS liquibase;"
          env:
            - name: DB_HOST
              value: "<host>"
            - name: DB_PORT
              value: "5432"
            - name: DB_NAME
              value: "<database>"
            - name: DB_USER
              valueFrom:
                secretKeyRef:
                  name: <secret>
                  key: username
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: <secret>
                  key: password
