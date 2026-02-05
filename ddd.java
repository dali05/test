apiVersion: v1
kind: Pod
metadata:
  name: postgres-test-pod
  namespace: ns002i012773
  annotations:
    com.illumio.app: postgres-test
    com.illumio.env: dev
    com.illumio.app-tier: database
    com.illumio.middleware: postgres
    com.illumio.role: test
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