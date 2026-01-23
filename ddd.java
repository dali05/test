command: ["/bin/sh","-c"]
args:
  - |
    set -e
    apk add --no-cache postgresql-client || \
    apt-get update && apt-get install -y postgresql-client

    psql -v ON_ERROR_STOP=1 -f /sql/init.sql