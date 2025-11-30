camunda:
  client:
    mode: cluster
    cluster:
      zeebe:
        gateway:
          address: localhost:26500
        security:
          plaintext: true