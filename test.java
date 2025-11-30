camunda:
  client:
    mode: self-managed          # ← ajoute / corrige cette ligne
    zeebe:
      request-timeout: 360s
      base-url: ${ZEEBE_BASE_URL:http://localhost:8080}
      gateway-url: ${ZEEBE_GATEWAY_URL:localhost:26500}
      security:
        plaintext: true         # ← force une connexion non-TLS si dispo dans ta version
    operate:
      base-url: ${OPERATE_BASE_URL:http://localhost:8081}
      username: demo
      password: demo
      auth-enabled: false