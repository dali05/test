@Autowired
private ObjectMapper objectMapper;

public JsonNode loadTemplateJson() throws IOException {
    InputStream is = getClass().getResourceAsStream("/template.json");
    return objectMapper.readTree(is);
}


public ObjectNode buildFinalPayload(String vptoken) throws IOException {
    ObjectNode root = (ObjectNode) loadTemplateJson();
    root.put("vptoken", vptoken); // injection du token dynamique
    return root;
}


public Mono<String> sendPayload(String vptoken) throws IOException {

    ObjectNode body = buildFinalPayload(vptoken);

    return WebClient.create()
            .post()
            .uri("https://autre-service.com/api/receive")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class);
}