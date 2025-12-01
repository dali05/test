zeebe: client:
connectionMode: ADDRESS
default-tenant-id: ${ZEEBE TENANT ID)
broker:
grpc-address: SIZEEBE.
GRPC ADDRESS)
security:
plaintext: false
default-tenant-id: ${ZEEBE TENANT ID} worker:
threads: $(ZEEBE WORKER THREADS: 10)
max-jobs-active: ${ZEEBE WORKER MAX JOBS: 32}