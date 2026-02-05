apiVersion: batch/v1
kind: Job
metadata:
  name: postgres-test-job
  namespace: dev
spec:
  backoffLimit: 0
  template:
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