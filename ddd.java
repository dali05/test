apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-db-bootstrap
  labels:
    app.kubernetes.io/name: {{ .Chart.Name }}
    app.kubernetes.io/instance: {{ .Release.Name }}
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-10"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    metadata:
      labels:
        app.kubernetes.io/name: {{ .Chart.Name }}
        app.kubernetes.io/instance: {{ .Release.Name }}
      annotations:
        {{- /* Important: vaultenv doit injecter les env au runtime */}}
        {{- if .Values.liquibase.hashicorp.enabled }}
        {{- include "common-library.hashicorp.vaultenv" (dict
              "Values" .Values.liquibase
              "Release" .Release
              "Chart" .Chart
              "Capabilities" .Capabilities
            ) | nindent 8 }}
        {{- end }}
    spec:
      restartPolicy: Never
      containers:
        - name: db-bootstrap
          image: postgres:15
          imagePullPolicy: IfNotPresent
          command: ["sh", "-c"]
          args:
            - |
              set -e

              echo "=== Bootstrap schema start ==="

              # vaultenv injecte ces variables (comme dans values.yaml)
              # On les convertit pour psql
              export PGUSER="${PF_LIQUIBASE_COMMAND_USERNAME}"
              export PGPASSWORD="${PF_LIQUIBASE_COMMAND_PASSWORD}"

              echo "PGHOST=${PGHOST}"
              echo "PGPORT=${PGPORT}"
              echo "PGDATABASE=${PGDATABASE}"
              echo "PGUSER=${PGUSER}"

              # IMPORTANT: créer les schémas AVANT Liquibase
              # AUTHORIZATION CURRENT_USER => le owner sera le user DB utilisé (celui issu de Vault)
              psql -v ON_ERROR_STOP=1 <<'SQL'
              CREATE SCHEMA IF NOT EXISTS admin     AUTHORIZATION CURRENT_USER;
              CREATE SCHEMA IF NOT EXISTS liquibase AUTHORIZATION CURRENT_USER;
              SQL

              echo "=== Bootstrap schema done ==="

          env:
            # Réutilisation EXACTE des env Liquibase (Vault inclus : PF_LIQUIBASE_COMMAND_USERNAME/PASSWORD, etc.)
            {{- toYaml .Values.liquibase.job.extraEnv | nindent 12 }}

            # Variables standard Postgres pour psql
            - name: PGHOST
              value: "postgresql.ns-postgresql.svc.cluster.local"
            - name: PGPORT
              value: "5432"
            - name: PGDATABASE
              value: "ibmclouddb"