apiVersion: v1
kind: ServiceAccount
metadata:
  name: local-ap11236-java-application-liquibase
  namespace: {{ .Release.Namespace }}
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-30"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded