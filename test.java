camunda:
  client:
    mode: self-managed
    zeebe:
      gateway:
        address: "localhost:26500"
        plaintext: true
    operate:
      base-url: "http://localhost:8081"
      auth:
        enabled: false