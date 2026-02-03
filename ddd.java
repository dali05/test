vault:
  enabled: true
  address: "http://vault.ns-vault.svc.cluster.local:8200"

  auth:
    mountPath: "auth/kubernetes"
    role: "ns-wall-e-springboot-local-ap11236-java-application-liquibase"

  # Creds dynamiques Postgres (database secrets engine)
  secrets:
    postgresCredsPath: "database/postgres/pg0000000/creds/own_pg0000000_ibmclouddb"

  # Certificats (ex KV v2)
  certs:
    enabled: true
    # Exemple KV v2: secret/data/... (Vault Agent template doit lire .Data.data.*)
    path: "secret/data/ap11236/local/java-application"
    keys:
      ca: "ca_crt"
      crt: "client_crt"
      key: "client_key"
    outDir: "/vault/secrets/certs"

  image:
    repository: "hashicorp/vault"
    tag: "1.15.2"
    pullPolicy: IfNotPresent

  paths:
    configDir: "/vault/config"
    secretsDir: "/vault/secrets"
    tokenFile: "/vault/secrets/.vault-token"
    pgEnvFile: "/vault/secrets/pg.env"

db:
  host: "postgresql.ns-postgresql.svc.cluster.local"
  port: 5432
  database: "ibmclouddb"

bootstrap:
  schemas:
    - "admin"
    - "liquibase"


{{- if .Values.vault.enabled }}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "common.names.fullname" . }}-vault-agent-config
  labels:
    app.kubernetes.io/name: {{ include "common.names.name" . }}
    app.kubernetes.io/instance: {{ .Release.Name }}
data:
  agent.hcl: |
    pid_file = "/tmp/vault-agent.pid"

    vault {
      address = "{{ .Values.vault.address }}"
    }

    auto_auth {
      method "kubernetes" {
        mount_path = "{{ .Values.vault.auth.mountPath }}"
        config = {
          role = "{{ .Values.vault.auth.role }}"
        }
      }

      sink "file" {
        config = {
          path = "{{ .Values.vault.paths.tokenFile }}"
        }
      }
    }

    template {
      source      = "{{ .Values.vault.paths.configDir }}/pg.env.ctmpl"
      destination = "{{ .Values.vault.paths.pgEnvFile }}"
      perms       = "0600"
    }

    {{- if .Values.vault.certs.enabled }}
    template {
      source      = "{{ .Values.vault.paths.configDir }}/certs.ctmpl"
      destination = "{{ .Values.vault.certs.outDir }}/bundle.env"
      perms       = "0600"
    }
    {{- end }}

  pg.env.ctmpl: |
    {{`{{- with secret "`}}{{ .Values.vault.secrets.postgresCredsPath }}{{`" -}}`}}
    export PGUSER="{{`{{ .Data.username }}`}}"
    export PGPASSWORD="{{`{{ .Data.password }}`}}"
    {{`{{- end }}`}}

  {{- if .Values.vault.certs.enabled }}
  certs.ctmpl: |
    {{`{{- with secret "`}}{{ .Values.vault.certs.path }}{{`" -}}`}}
    # KV v2 => .Data.data.*
    export CA_CRT="{{`{{ index .Data.data "`}}{{ .Values.vault.certs.keys.ca }}{{`" }}`}}"
    export TLS_CRT="{{`{{ index .Data.data "`}}{{ .Values.vault.certs.keys.crt }}{{`" }}`}}"
    export TLS_KEY="{{`{{ index .Data.data "`}}{{ .Values.vault.certs.keys.key }}{{`" }}`}}"
    {{`{{- end }}`}}
{{- end }}

apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-db-bootstrap
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-10"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never

      volumes:
        # Secrets rendus par vault-agent (pg.env + certs)
        - name: vault-secrets
          emptyDir:
            medium: Memory

        # ConfigMap vault agent
        - name: vault-agent-config
          configMap:
            name: {{ include "common.names.fullname" . }}-vault-agent-config

      initContainers:
        {{- if .Values.vault.enabled }}
        - name: vault-agent
          image: "{{ .Values.vault.image.repository }}:{{ .Values.vault.image.tag }}"
          imagePullPolicy: {{ .Values.vault.image.pullPolicy }}
          command: ["vault","agent","-config={{ .Values.vault.paths.configDir }}/agent.hcl"]
          env:
            - name: VAULT_ADDR
              value: "{{ .Values.vault.address }}"
          volumeMounts:
            - name: vault-agent-config
              mountPath: {{ .Values.vault.paths.configDir }}
              readOnly: true
            - name: vault-secrets
              mountPath: {{ .Values.vault.paths.secretsDir }}
        {{- end }}

        {{- if and .Values.vault.enabled .Values.vault.certs.enabled }}
        - name: certs-materialize
          image: alpine:3.20
          command: ["sh","-c"]
          args:
            - |
              set -e
              mkdir -p "{{ .Values.vault.certs.outDir }}"
              echo "Waiting for bundle.env..."
              i=0
              while [ ! -f "{{ .Values.vault.certs.outDir }}/bundle.env" ] && [ $i -lt 60 ]; do
                i=$((i+1)); sleep 1
              done
              test -f "{{ .Values.vault.certs.outDir }}/bundle.env"

              # charge env
              . "{{ .Values.vault.certs.outDir }}/bundle.env"

              # écrit fichiers
              echo "$CA_CRT"  > "{{ .Values.vault.certs.outDir }}/ca.crt"
              echo "$TLS_CRT" > "{{ .Values.vault.certs.outDir }}/tls.crt"
              echo "$TLS_KEY" > "{{ .Values.vault.certs.outDir }}/tls.key"

              chmod 600 "{{ .Values.vault.certs.outDir }}/tls.key"
              chmod 644 "{{ .Values.vault.certs.outDir }}/ca.crt" "{{ .Values.vault.certs.outDir }}/tls.crt"

              echo "OK certs written:"
              ls -l "{{ .Values.vault.certs.outDir }}"
          volumeMounts:
            - name: vault-secrets
              mountPath: {{ .Values.vault.paths.secretsDir }}
        {{- end }}

      containers:
        - name: db-bootstrap
          image: postgres:15
          imagePullPolicy: IfNotPresent
          command: ["sh","-c"]
          args:
            - |
              set -e

              echo "Waiting for Vault-rendered {{ .Values.vault.paths.pgEnvFile }}..."
              i=0
              while [ ! -f "{{ .Values.vault.paths.pgEnvFile }}" ] && [ $i -lt 60 ]; do
                i=$((i+1)); sleep 1
              done

              if [ ! -f "{{ .Values.vault.paths.pgEnvFile }}" ]; then
                echo "ERROR: {{ .Values.vault.paths.pgEnvFile }} not found"
                ls -la "{{ .Values.vault.paths.secretsDir }}" || true
                exit 1
              fi

              echo "OK: pg.env exists. Loading it..."
              . "{{ .Values.vault.paths.pgEnvFile }}"

              # Option: si tu utilises SSL côté Postgres
              {{- if and .Values.vault.enabled .Values.vault.certs.enabled }}
              export PGSSLMODE=verify-full
              export PGSSLROOTCERT="{{ .Values.vault.certs.outDir }}/ca.crt"
              export PGSSLCERT="{{ .Values.vault.certs.outDir }}/tls.crt"
              export PGSSLKEY="{{ .Values.vault.certs.outDir }}/tls.key"
              {{- end }}

              echo "Creating schemas..."
              psql \
                -h "{{ .Values.db.host }}" \
                -p "{{ .Values.db.port }}" \
                -U "$PGUSER" \
                -d "{{ .Values.db.database }}" \
                -v ON_ERROR_STOP=1 \
                -c "DO $$ BEGIN
                      {{- range $i, $s := .Values.bootstrap.schemas }}
                      EXECUTE 'CREATE SCHEMA IF NOT EXISTS {{ $s }}';
                      {{- end }}
                    END $$;"

              echo "DONE."
          volumeMounts:
            - name: vault-secrets
              mountPath: {{ .Values.vault.paths.secretsDir }}

