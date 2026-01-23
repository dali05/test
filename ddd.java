{{- if .Values.liquibase.job.enabled }}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-db-bootstrap
  labels:
    app.kubernetes.io/name: {{ .Release.Name }}
    app.kubernetes.io/component: db-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded

    # Vault Agent Injection
    vault.hashicorp.com/agent-inject: "true"
    vault.hashicorp.com/agent-pre-populate-only: "true"
    vault.hashicorp.com/role: "{{ .Values.hashicorp.path }}"

spec:
  backoffLimit: 1
  template:
    metadata:
      labels:
        app.kubernetes.io/name: {{ .Release.Name }}
        app.kubernetes.io/component: db-bootstrap
    spec:
      restartPolicy: Never

      serviceAccountName: {{ .Values.serviceAccount.name | default "default" }}

      imagePullSecrets:
        - name: docker-registry-cred

      containers:
        - name: db-bootstrap
          image: {{ .Values.liquibase.job.image.fullName }}
          imagePullPolicy: {{ .Values.liquibase.job.image.pullPolicy }}

          command: ["sh", "-c"]
          args:
            - |
              set -e
              echo "=== DB Bootstrap start ==="
              export PGPASSWORD="$POSTGRES_PASSWORD"
              psql "$POSTGRES_URL" \
                -U "$POSTGRES_USERNAME" \
                -f /sql/init.sql
              echo "=== DB Bootstrap done ==="

          env:
            - name: POSTGRES_URL
              value: "{{ .Values.liquibase.job.extraEnv.POSTGRES_URL | default "" }}"
            - name: POSTGRES_USERNAME
              valueFrom:
                secretKeyRef:
                  name: postgres-creds
                  key: username
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-creds
                  key: password

          volumeMounts:
            - name: sql-init
              mountPath: /sql
              readOnly: true

      volumes:
        - name: sql-init
          configMap:
            name: {{ .Release.Name }}-db-init
{{- end }}

apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .Release.Name }}-db-init
data:
  init.sql: |
{{ .Files.Get "sql/init.sql" | indent 4 }}

