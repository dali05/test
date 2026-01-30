USER root
RUN apt-get update \
 && apt-get install -y --no-install-recommends postgresql-client \
 && rm -rf /var/lib/apt/lists/*
USER 1001


USER root
RUN apk add --no-cache postgresql-client
USER 1001
