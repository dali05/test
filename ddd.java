apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-db-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-10"
    "helm.sh/hook-delete-policy": hook-succeeded
spec:
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: bootstrap
          image: postgres:15
          env:
            - name: DB_URL
              valueFrom:
                secretKeyRef:
                  name: db-bootstrap-secret
                  key: url
          command: ["sh","-c"]
          args:
            - psql "$DB_URL" -c 'CREATE SCHEMA IF NOT EXISTS admin;'
chart/templates/00-db-bootstrap-job.yaml
