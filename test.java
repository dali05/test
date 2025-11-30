camunda:
  client:
    mode: simple

    zeebe:
      gateway-address: "localhost:26500"
      security:
        plaintext: true   # ⬅️ TRÈS IMPORTANT

    operate:
      base-url: "http://localhost:8081"
      auth-enabled: false