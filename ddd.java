apiVersion: batch/v1
kind: Job
metadata:
  name: postgres-test-job
  namespace: ns002i012773
  annotations:
    com.illumio.app: postgres-test
    com.illumio.env: dev
    com.illumio.app-tier: database
    com.illumio.middleware: postgres
    com.illumio.role: job
spec:
  backoffLimit: 0
  template:
    metadata:
      annotations:
        com.illumio.app: postgres-test
        com.illumio.env: dev
        com.illumio.app-tier: database
        com.illumio.middleware: postgres
        com.illumio.role: job
    spec:
      restartPolicy: Never
      securityContext:
        seccompProfile:
          type: RuntimeDefault
      containers:
        - name: postgres
          image: postgres:15
          securityContext:
            allowPrivilegeEscalation: false
          env:
            - name: POSTGRES_PASSWORD
              value: example