apiVersion: v1
kind: ConfigMap
metadata:
  name: wall-e-db-init
  namespace: ns-wall-e-springboot
data:
  init.sql: |
    CREATE SCHEMA IF NOT EXISTS admin;