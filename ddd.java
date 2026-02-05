apiVersion: batch/v1
kind: Job
metadata:
  name: postgres-test-job
  namespace: dev   # ⚠️ change si besoin
spec:
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: postgres
          image: postgres:15
          env:
            - name: POSTGRES_PASSWORD
              value: example