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

      serviceAccountName: {{ .Values.serviceAccount.name | default (printf "%s-sa" .Release.Name) }}

      volumes:
        - name: vault-shared-data
          emptyDir: {}
        - name: vault-config
          configMap:
            name: {{ .Release.Name }}-vault-agent-config
            optional: false

      initContainers:
        - name: vault-agent
          image: {{ .Values.hashicorp.image }}
          args:
            - "agent"
            - "-config=/etc/vault/vault-agent-config.hcl"
            - "-log-level=info"
            - "-exit-after-auth"
          env:
            - name: VAULT_ADDR
              value: "{{ .Values.hashicorp.addr }}"
            - name: VAULT_NAMESPACE
              value: "{{ .Values.hashicorp.ns }}"
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

      containers:
        - name: db-bootstrap
          image: postgres:15
          imagePullPolicy: IfNotPresent
          command: ["sh","-lc"]
          args:
            - |
              set -e

              echo "Waiting for Vault-rendered /etc/secrets/pg.env..."
              i=0
              while [ ! -f /etc/secrets/pg.env ] && [ $i -lt 60 ]; do
                i=$((i+1))
                sleep 1
              done
              if [ ! -f /etc/secrets/pg.env ]; then
                echo "ERROR: /etc/secrets/pg.env not found"
                ls -la /etc/secrets || true
                exit 1
              fi

              # charge PGUSER / PGPASSWORD
              . /etc/secrets/pg.env

              echo "Using PGUSER=$PGUSER (password hidden)"
              export PGPASSWORD="$PGPASSWORD"

              psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
                -c "CREATE SCHEMA IF NOT EXISTS admin; CREATE SCHEMA IF NOT EXISTS liquibase;"

          env:
            - name: PGHOST
              value: "postgresql.ns-postgresql.svc.cluster.local"
            - name: PGPORT
              value: "5432"
            - name: PGDATABASE
              value: "ibmclouddb"
          volumeMounts:
            - name: vault-shared-data
              mountPath: /etc/secrets