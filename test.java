camunda:
  client:
    mode: saas|self-managed|legacy-zeebe  # <- tu mets self-managed si tu tournes en local
    self-managed:
      # === Zeebe Gateway ===
      zeebe:
        base-url: http://localhost:26500
        enabled: true

      # Utilisation du transport GRPC (par défaut)
      prefer-rest-over-grpc: false

      # === Security ===
      # si ton zeebe local n’est pas sécurisé
      auth:
        enabled: false

      # === Operate ===
      operate:
        base-url: http://localhost:8081
        auth:
          enabled: false
          username: demo
          password: demo