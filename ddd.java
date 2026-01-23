{{- if .Values.liquibase.job.enabled }}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-db-bootstrap
  labels:
    app.kubernetes.io/name: {{ .Release.Name }}
    app.kubernetes.io/instance: {{ .Release.Name }}
    app.kubernetes.io/component: db-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded

    # --- Vault Agent Injection ---
    vault.hashicorp.com/agent-inject: "true"
    vault.hashicorp.com/agent-pre-populate-only: "true"
    # ⚠️ À adapter selon votre environnement Vault/K8s :
    # - soit vous avez un champ .Values.hashicorp.role
    # - soit vous utilisez le "path" comme nom de rôle (cas fréquent en interne)
    vault.hashicorp.com/role: "{{ default .Values.hashicorp.path .Values.hashicorp.role }}"
spec:
  backoffLimit: 1
  template:
    metadata:
      labels:
        app.kubernetes.io/name: {{ .Release.Name }}
        app.kubernetes.io/instance: {{ .Release.Name }}
        app.kubernetes.io/component: db-bootstrap
    spec:
      restartPolicy: Never

      {{- if .Values.liquibase.job.serviceAccountName }}
      serviceAccountName: {{ .Values.liquibase.job.serviceAccountName | quote }}
      {{- else if .Values.liquibase.serviceAccount.name }}
      serviceAccountName: {{ .Values.liquibase.serviceAccount.name | quote }}
      {{- else if .Values.serviceAccount.name }}
      serviceAccountName: {{ .Values.serviceAccount.name | quote }}
      {{- end }}

      {{- /* imagePullSecrets: support soit liste soit valeur simple */ -}}
      {{- if .Values.liquibase.job.imagePullSecrets }}
      imagePullSecrets:
{{ toYaml .Values.liquibase.job.imagePullSecrets | indent 8 }}
      {{- else if .Values.liquibase.job.imagePullSecret }}
      imagePullSecrets:
        - name: {{ .Values.liquibase.job.imagePullSecret | quote }}
      {{- else if .Values.liquibase.job.imagePullSecretsName }}
      imagePullSecrets:
        - name: {{ .Values.liquibase.job.imagePullSecretsName | quote }}
      {{- end }}

      containers:
        - name: db-bootstrap
          image: {{ .Values.liquibase.job.image.fullName | quote }}
          imagePullPolicy: {{ .Values.liquibase.job.image.pullPolicy | default "IfNotPresent" | quote }}

          # IMPORTANT: vaultenv doit être l'entrypoint pour résoudre les valeurs "vault:..."
          command: ["vaultenv"]
          args:
            - "sh"
            - "-c"
            - |
              set -e
              echo "=== DB Bootstrap start ==="
              echo "Using URL env PF_LIQUIBASE_COMMAND_URL"
              # psql utilisera PF_LIQUIBASE_COMMAND_USERNAME / PASSWORD après résolution vaultenv
              export PGPASSWORD="$PF_LIQUIBASE_COMMAND_PASSWORD"
              psql "$PF_LIQUIBASE_COMMAND_URL" \
                -U "$PF_LIQUIBASE_COMMAND_USERNAME" \
                -f /sql/init.sql
              echo "=== DB Bootstrap done ==="

          volumeMounts:
            - name: sql-init
              mountPath: /sql
              readOnly: true

          env:
            # On récupère tes extraEnv du values.yaml (incluant les vault:...)
{{- if .Values.liquibase.job.extraEnv }}
{{ toYaml .Values.liquibase.job.extraEnv | indent 12 }}
{{- else if .Values.liquibase.extraEnv }}
{{ toYaml .Values.liquibase.extraEnv | indent 12 }}
{{- end }}

          resources:
{{- if .Values.liquibase.job.resources }}
{{ toYaml .Values.liquibase.job.resources | indent 12 }}
{{- else if .Values.liquibase.resources }}
{{ toYaml .Values.liquibase.resources | indent 12 }}
{{- end }}

      volumes:
        - name: sql-init
          configMap:
            name: {{ .Release.Name }}-db-init
            items:
              - key: init.sql
                path: init.sql
{{- end }}