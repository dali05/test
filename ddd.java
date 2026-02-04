
serviceAccountName: {{ include "common-library.fullname" . }}-{{ .Values.bootstrap.selectors.component }}

apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .Release.Name }}-certsync-vault-config
  namespace: {{ .Release.Namespace }}
data:
  vault-agent.hcl: |
    vault {
      address = "{{ .Values.certSync.vault.addr }}"
    }

    auto_auth {
      method "kubernetes" {
        mount_path = "auth/{{ .Values.certSync.vault.authMount }}"
        config = {
          role = "{{ .Values.certSync.vault.role }}"
        }
      }
    }

    # =====================
    # KV v2 template
    # =====================

    template {
      destination = "/work/tls.crt"
      perms = "0644"
      contents = <<EOH
{{ "{{- with secret \"secret/data/ap11236/local/java-application\" -}}" }}
{{ "{{ .Data.data.client_cert_test }}" }}
{{ "{{- end -}}" }}
EOH
    }

    template {
      destination = "/work/tls.key"
      perms = "0600"
      contents = <<EOH
{{ "{{- with secret \"secret/data/ap11236/local/java-application\" -}}" }}
{{ "{{ .Data.data.client_key_test }}" }}
{{ "{{- end -}}" }}
EOH
    }


apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-cert-sync
spec:
  backoffLimit: 1

  template:
    spec:
      restartPolicy: Never
      serviceAccountName: {{ .Release.Name }}-db-bootstrap

      volumes:
        - name: work
          emptyDir: {}

        - name: vault-config
          configMap:
            name: {{ .Release.Name }}-certsync-vault-config

      initContainers:
        - name: vault-agent
          image: {{ .Values.certSync.vault.image }}
          command:
            - vault
            - agent
            - -config=/vault/config/vault-agent.hcl
          volumeMounts:
            - name: work
              mountPath: /work
            - name: vault-config
              mountPath: /vault/config

      containers:
        - name: create-secret
          image: bitnami/kubectl:latest
          command: ["/bin/sh","-c"]
          args:
            - |
              set -e

              kubectl create secret tls {{ .Values.certSync.secretName }} \
                --cert=/work/tls.crt \
                --key=/work/tls.key \
                --dry-run=client -o yaml \
              | kubectl apply -f -

              echo "TLS secret created"

certSync:
  enabled: true

  secretName: wall-e-tls

  vault:
    image: docker-registry-devops.pf.echonet/hashicorp/vault:1.21
    addr: http://vault.ns-vault.svc.cluster.local:8200
    authMount: kubernetes_kub00001_local
    role: ns-wall-e-springboot-local-ap11236-java-application


