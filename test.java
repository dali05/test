version: "3.9"
services:
  zeebe:
    image: camunda/zeebe:8.6.0
    ports:
      - "26500:26500"
      - "9600:9600"
    environment:
      - ZEEBE_LOG_LEVEL=info

  operate:
    image: camunda/operate:8.6.0
    ports:
      - "8081:8080"
    environment:
      - CAMUNDA_OPERATE_ZEEBE_BROKER_GATEWAYADDRESS=zeebe:26500

  tasklist:
    image: camunda/tasklist:8.6.0
    ports:
      - "8082:8080"

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
    ports:
      - "9200:9200"