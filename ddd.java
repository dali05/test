
args:
  - |
    set -euo pipefail

    set -a
    . /etc/secrets/pg.env
    set +a

    {{- $schemas := join "; CREATE SCHEMA IF NOT EXISTS " .Values.bootstrap.schemas }}

    psql \
      -h "{{ .Values.bootstrap.db.host }}" \
      -p "{{ .Values.bootstrap.db.port }}" \
      -U "$PGUSER" \
      -d "{{ .Values.bootstrap.db.name }}" \
      -v ON_ERROR_STOP=1 \
      -c "CREATE SCHEMA IF NOT EXISTS {{ $schemas }};"

    echo "DONE."


bootstrap:
  enabled: true
  jobNameSuffix: db-bootstrap

  # ServiceAccount utilisé pour l'auth Kubernetes -> Vault
  serviceAccountName: local-ap11236-java-application-liquibase

  # Schémas à créer
  schemas:
    - admin
    - liquibase

  db:
    host: postgresql.ns-postgresql.svc.cluster.local
    port: 5432
    name: ibmclouddb

hashicorp:
  enabled: true
  method: vault-agent-initcontainer

  image: docker-registry-devops.pf.echonet/hashicorp/vault:1.21

  # Adresse Vault
  addr: "http://vault.ns-vault.svc.cluster.local:8200"

  # Vault Namespace (header X-Vault-Namespace) -> laisse vide si non utilisé
  namespace: ""

  # Kubernetes auth mount (⚠️ sans "auth/" devant)
  path: kubernetes_kub00001_local

  # Vault role Kubernetes auth
  role: ns-wall-e-springboot-local-ap11236-java-application-liquibase

  # Secret DB (database secrets engine)
  postgresCredsPath: database/postgres/pg0000000/creds/own_pg0000000_ibmclouddb






{{- if and .Values.hashicorp.enabled (eq .Values.hashicorp.method "vault-agent-initcontainer") .Values.bootstrap.enabled }}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .Release.Name }}-vault-agent-config
  namespace: {{ .Release.Namespace }}
data:
  vault-agent-config.hcl: |
    pid_file = "/tmp/vault-agent.pid"

    vault {
      address = "{{ .Values.hashicorp.addr }}"
      {{- if .Values.hashicorp.namespace }}
      namespace = "{{ .Values.hashicorp.namespace }}"
      {{- end }}
    }

    auto_auth {
      method "kubernetes" {
        mount_path = "auth/{{ .Values.hashicorp.path }}"
        config = {
          role = "{{ .Values.hashicorp.role }}"
        }
      }

      sink "file" {
        config = {
          path = "/etc/secrets/.vault-token"
        }
      }
    }

    template {
      destination = "/etc/secrets/pg.env"
      perms       = "0600"
      contents = <<EOH
{{ "{{- with secret \"" }}{{ .Values.hashicorp.postgresCredsPath }}{{ "\" -}}" }}
export PGUSER="{{ "{{ .Data.username }}" }}"
export PGPASSWORD="{{ "{{ .Data.password }}" }}"
{{ "{{- end -}}" }}
EOH
    }
{{- end }}



{{- if .Values.bootstrap.enabled }}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-{{ .Values.bootstrap.jobNameSuffix }}
  namespace: {{ .Release.Namespace }}
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-10"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never
      serviceAccountName: {{ .Values.bootstrap.serviceAccountName | quote }}

      volumes:
        - name: vault-shared-data
          emptyDir: {}

        - name: vault-config
          configMap:
            name: {{ .Release.Name }}-vault-agent-config

      initContainers:
        {{- if and .Values.hashicorp.enabled (eq .Values.hashicorp.method "vault-agent-initcontainer") }}
        - name: vault-agent
          image: {{ .Values.hashicorp.image | quote }}
          imagePullPolicy: IfNotPresent
          args:
            - "agent"
            - "-config=/etc/vault/vault-agent-config.hcl"
            - "-log-level=info"
            - "-exit-after-auth"
          env:
            - name: VAULT_ADDR
              value: {{ .Values.hashicorp.addr | quote }}
            {{- if .Values.hashicorp.namespace }}
            - name: VAULT_NAMESPACE
              value: {{ .Values.hashicorp.namespace | quote }}
            {{- end }}
            - name: SKIP_CHOWN
              value: "true"
            - name: SKIP_SETCAP
              value: "true"
          volumeMounts:
            - name: vault-shared-data
              mountPath: /etc/secrets
            - name: vault-config
              mountPath: /etc/vault
              readOnly: true
        {{- end }}

      containers:
        - name: db-bootstrap
          image: postgres:15
          imagePullPolicy: IfNotPresent
          command: ["sh", "-lc"]
          args:
            - |
              set -euo pipefail

              echo "Waiting for Vault-rendered /etc/secrets/pg.env..."
              i=0
              while [ ! -s /etc/secrets/pg.env ] && [ $i -lt 120 ]; do
                i=$((i+1))
                sleep 1
              done

              if [ ! -s /etc/secrets/pg.env ]; then
                echo "ERROR: /etc/secrets/pg.env missing or empty"
                ls -la /etc/secrets || true
                exit 1
              fi

              # Load exports
              set -a
              . /etc/secrets/pg.env
              set +a

              echo "PGUSER=$PGUSER"
              echo "PGPASSWORD set? $([ -n "${PGPASSWORD:-}" ] && echo yes || echo no)"

              export PGPASSWORD="$PGPASSWORD"

              # Create schemas (idempotent)
              psql \
                -h "{{ .Values.bootstrap.db.host }}" \
                -p "{{ .Values.bootstrap.db.port }}" \
                -U "$PGUSER" \
                -d "{{ .Values.bootstrap.db.name }}" \
                -v ON_ERROR_STOP=1 \
                -c "CREATE SCHEMA IF NOT EXISTS {{ join \"; CREATE SCHEMA IF NOT EXISTS \" .Values.bootstrap.schemas }};"

              echo "DONE."
          volumeMounts:
            - name: vault-shared-data
              mountPath: /etc/secrets
{{- end }}

