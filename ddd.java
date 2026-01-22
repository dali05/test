db-init-configmap.yaml

apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .Release.Name }}-db-init
  labels:
    app.kubernetes.io/name: {{ .Release.Name }}
    app.kubernetes.io/component: db-bootstrap
data:
  init.sql: |
{{ .Files.Get "sql/init.sql" | indent 4 }}