job:
  initcontainers:
    enabled: true
    createSchema:
      command: >
        sh -c "
        echo 'Creating schema admin';
        export PGPASSWORD=$POSTGRES_PASSWORD;
        psql -h $POSTGRES_HOST -p $POSTGRES_PORT -U $POSTGRES_USER $POSTGRES_DATABASE
        -v ON_ERROR_STOP=1
        -c 'CREATE SCHEMA IF NOT EXISTS admin';
        echo 'Schema admin ready'
        "