ano docker-compose.yml


version: '3.8'

services:

  zeebe:
    image: camunda/zeebe:8.5.0
    container_name: zeebe
    environment:
      ZEEBE_LOG_LEVEL: info
    ports:
      - "26500:26500" # Zeebe Gateway
    volumes:
      - zeebe_data:/usr/local/zeebe/data

  operate:
    image: camunda/operate:8.5.0
    container_name: operate
    environment:
      - CAMUNDA_OPERATE_ZEEBE_BROKER_GATEWAYADDRESS=zeebe:26500
      - CAMUNDA_OPERATE_ELASTICSEARCH_URL=http://elasticsearch:9200
    ports:
      - "8080:8080"
    depends_on:
      - zeebe
      - elasticsearch

  tasklist:
    image: camunda/tasklist:8.5.0
    container_name: tasklist
    environment:
      - CAMUNDA_TASKLIST_ZEEBE_BROKER_GATEWAYADDRESS=zeebe:26500
      - CAMUNDA_TASKLIST_ELASTICSEARCH_URL=http://elasticsearch:9200
    ports:
      - "8081:8080"
    depends_on:
      - zeebe
      - elasticsearch

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.17.9
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data

volumes:
  zeebe_data:
  es_data:
