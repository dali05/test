spring:
  jpa:
    ...
h2:
  console:
    enabled: true
    path: /h2-console

zeebe:
  client:
    gateway-url: http://localhost:26500
    prefer-rest-over-grpc: false   # très important sinon le client essaie d’utiliser le REST endpoint !

camunda:
  client:
    operate:
      base-url: http://localhost:8081
      username: demo
      password: demo
      auth-enabled: false