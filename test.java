String token = WebClient.create()
        .get()
        .uri("https://api.example.com/login")
        .retrieve()
        .bodyToMono(com.fasterxml.jackson.databind.JsonNode.class)
        .map(json -> json.get("authorization").get("token").asText())
        .block();

System.out.println("TOKEN = " + token);