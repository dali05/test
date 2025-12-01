zeebe:
  client:
    connectionMode: ADDRESS        # on se connecte directement au gateway
    default-tenant-id: default     # si tu n'utilises pas le multi-tenant
    broker:
      grpc-address: localhost:26500
    security:
      plaintext: true              # Camunda local fonctionne en non-TLS
  worker:
    threads: 10
    max-jobs-active: 32

camunda:
  client:
    operate:
      base-url: http://localhost:8081
      auth-enabled: false
    tasklist:
      base-url: http://localhost:8082
      auth-enabled: false