db:
  host: "postgresql.ns-postgresql.svc.cluster.local"
  port: "5432"
  database: "ibmclouddb"

# Vault pour le Job bootstrap
vault:
  # Role Vault Kubernetes (lié au ServiceAccount ci-dessous)
  role: "own_pg000000_ibmclouddb"
  # Chemin du secret dynamique (engine database)
  dbCredsPath: "database/creds/own_pg000000_ibmclouddb"
  # optionnel si vous utilisez Vault namespaces
  namespace: ""

serviceAccount:
  # IMPORTANT: ce SA doit exister et être autorisé côté Vault
  # Si tu en as déjà un, mets son nom ici
  name: "wall-e-vault-sa"

liquibase:
  job:
    enabled: true