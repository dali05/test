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
    {{- /* Si vous avez un injector vaultenv basé sur annotations, ajoutez ici vos annotations maison.
          Je laisse un bloc générique activable via values.hashicorp.enabled */ -}}
    {{- if .Values.hashicorp.enabled }}
    # --- Vault/Vaultenv (à adapter à votre entreprise si besoin) ---
    # vault.example.com/enabled: "true"
    {{- end }}
spec:
  backoffLimit: 1
  template:
    metadata:
      labels:
        app.kubernetes.io/name: {{ .Release.Name }}
        app.kubernetes.io/instance: {{ .Release.Name }}
        app.kubernetes.io/component: db-bootstrap
      {{- if .Values.hashicorp.enabled }}
      annotations:
        # --- Vault/Vaultenv (à adapter à votre entreprise si besoin) ---
        # vault.example.com/enabled: "true"
        {{- /* Mets ici les annotations exactes de votre injector si vous en avez */ -}}
      {{- end }}
    spec:
      restartPolicy: Never

      {{- if .Values.liquibase.job.imagePullSecrets }}
      imagePullSecrets:
        {{- range .Values.liquibase.job.imagePullSecrets }}
        - name: {{ .name }}
        {{- end }}
      {{- end }}

      {{- if and .Values.serviceAccount .Values.serviceAccount.create }}
      serviceAccountName: {{ default (printf "%s-sa" .Release.Name) .Values.serviceAccount.name }}
      {{- end }}

      containers:
        - name: db-bootstrap
          image: {{ default "postgres:15" .Values.liquibase.job.image.fullName | quote }}
          imagePullPolicy: {{ default "IfNotPresent" .Values.liquibase.job.image.pullPolicy | quote }}

          command: ["sh", "-c"]
          args:
            - |
              set -euo pipefail

              echo "=== DB Bootstrap start ==="

              # Vérif rapide que Vault a bien injecté des valeurs "réelles"
              if echo "${PF_LIQUIBASE_COMMAND_USERNAME:-}" | grep -q '^vault:'; then
                echo "ERROR: PF_LIQUIBASE_COMMAND_USERNAME n'a pas été résolu par vaultenv (valeur commence par 'vault:')"
                exit 1
              fi
              if echo "${PF_LIQUIBASE_COMMAND_PASSWORD:-}" | grep -q '^vault:'; then
                echo "ERROR: PF_LIQUIBASE_COMMAND_PASSWORD n'a pas été résolu par vaultenv (valeur commence par 'vault:')"
                exit 1
              fi

              # Connexion TCP explicite (évite le socket /var/run/postgresql/.s.PGSQL.5432)
              : "${POSTGRES_HOST:?POSTGRES_HOST manquant}"
              : "${POSTGRES_PORT:?POSTGRES_PORT manquant}"
              : "${POSTGRES_DATABASE:?POSTGRES_DATABASE manquant}"
              : "${PF_LIQUIBASE_COMMAND_USERNAME:?PF_LIQUIBASE_COMMAND_USERNAME manquant}"
              : "${PF_LIQUIBASE_COMMAND_PASSWORD:?PF_LIQUIBASE_COMMAND_PASSWORD manquant}"

              export PGPASSWORD="${PF_LIQUIBASE_COMMAND_PASSWORD}"

              echo "Running init SQL..."
              psql \
                -h "${POSTGRES_HOST}" \
                -p "${POSTGRES_PORT}" \
                -U "${PF_LIQUIBASE_COMMAND_USERNAME}" \
                -d "${POSTGRES_DATABASE}" \
                -v ON_ERROR_STOP=1 \
                -f /sql/init.sql

              echo "=== DB Bootstrap done ==="

          env:
            {{- /* Extra env venant de values (incluant vault:... injecté ensuite) */ -}}
            {{- range .Values.liquibase.job.extraEnv }}
            - name: {{ .name | quote }}
              value: {{ .value | quote }}
            {{- end }}

            # Valeurs nécessaires au psql (si tu ne les as pas déjà dans liquibase.job.extraEnv)
            # Reprend celles que tu as déjà dans backend.extraEnv.
            {{- if .Values.backend.extraEnv }}
            {{- range .Values.backend.extraEnv }}
            - name: {{ .name | quote }}
              value: {{ .value | quote }}
            {{- end }}
            {{- end }}

          volumeMounts:
            - name: sql-init
              mountPath: /sql
              readOnly: true

      volumes:
        - name: sql-init
          configMap:
            # IMPORTANT : tu dois créer ce ConfigMap (template séparé) avec la key "init.sql"
            name: {{ .Release.Name }}-db-init
            items:
              - key: init.sql
                path: init.sql
{{- end }}