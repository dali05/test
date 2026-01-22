{{- if .Values.liquibase.enabled }}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-liquibase
  labels:
    app.kubernetes.io/name: {{ .Chart.Name }}
    app.kubernetes.io/instance: {{ .Release.Name }}
    app.kubernetes.io/component: liquibase
spec:
  backoffLimit: 1
  template:
    metadata:
      labels:
        app.kubernetes.io/name: {{ .Chart.Name }}
        app.kubernetes.io/instance: {{ .Release.Name }}
        app.kubernetes.io/component: liquibase
    spec:
      restartPolicy: Never

      {{- if .Values.liquibase.serviceAccount.create }}
      serviceAccountName: {{ .Release.Name }}-liquibase
      {{- end }}

      {{- if .Values.liquibase.job.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml .Values.liquibase.job.imagePullSecrets | nindent 8 }}
      {{- end }}

      # ✅ 1) initContainer : crée le schéma "admin" AVANT Liquibase
      initContainers:
        - name: create-admin-schema
          image: "{{ .Values.liquibase.job.image.fullName }}"
          imagePullPolicy: {{ .Values.liquibase.job.image.pullPolicy }}
          command: ["sh", "-c"]
          args:
            - |
              set -euo pipefail
              echo "[init] Create schema admin if not exists"

              # jdbc:postgresql://... => postgresql://...
              PGURL="${PF_LIQUIBASE_COMMAND_URL#jdbc:}"

              # IMPORTANT : enlever currentSchema=admin pour pouvoir se connecter même si admin n'existe pas
              PGURL="$(echo "$PGURL" | sed 's/[?&]currentSchema=[^&]*//g' | sed 's/?&/?/g' | sed 's/[?]$//g')"

              export PGPASSWORD="$PF_LIQUIBASE_COMMAND_PASSWORD"

              echo "[init] PGURL=$PGURL"
              psql "$PGURL" -U "$PF_LIQUIBASE_COMMAND_USERNAME" -v ON_ERROR_STOP=1 \
                -c 'CREATE SCHEMA IF NOT EXISTS "admin";'

              echo "[init] Schema admin ready"

          env:
            {{- range .Values.liquibase.job.extraEnv }}
            - name: {{ .name }}
              value: {{ .value | quote }}
            {{- end }}

      # ✅ 2) conteneur Liquibase
      containers:
        - name: liquibase
          image: "{{ .Values.liquibase.job.image.fullName }}"
          imagePullPolicy: {{ .Values.liquibase.job.image.pullPolicy }}

          env:
            {{- range .Values.liquibase.job.extraEnv }}
            - name: {{ .name }}
              value: {{ .value | quote }}
            {{- end }}

          resources:
            {{- toYaml .Values.liquibase.job.resources | nindent 12 }}
{{- end }}