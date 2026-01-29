{{- if .Values.liquibase.bootstrap.enabled }}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include "common-library.fullName" . }}-liquibase-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-20"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    metadata:
      labels:
        app.kubernetes.io/name: {{ include "common-library.fullName" . }}
    spec:
      serviceAccountName: local-ap11236-java-application-liquibase
      restartPolicy: Never
      containers:
        - name: liquibase-bootstrap
          image: {{ .Values.liquibase.job.image.fullName }}
          imagePullPolicy: {{ .Values.liquibase.job.image.pullPolicy }}
          env:
{{ toYaml .Values.liquibase.bootstrap.extraEnv | nindent 12 }}
{{ include "common-library.hashicorp.vaultenv" (dict
    "Values" .Values.liquibase
    "Release" .Release
    "Chart" .Chart
    "Capabilities" .Capabilities
  ) | nindent 12 }}
{{- end }}