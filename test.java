@Test
void parseVpToken_shouldReturnMap_whenHttpCallIsSuccessful() throws Exception {

    // GIVEN
    String token = "abc123";

    ObjectNode finalPayload = new ObjectMapper().createObjectNode();
    Mockito.when(objectMapper.createObjectNode()).thenReturn(finalPayload);

    Map<String, Object> responseMap = Map.of("status", "ok");

    // Mock WebClient chain
    Mockito.when(idactoWebClient.post()).thenReturn(requestBodyUriSpec);
    Mockito.when(requestBodyUriSpec.uri("/parse/token")).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.bodyValue(finalPayload)).thenReturn(requestHeadersSpec);
    Mockito.when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    Mockito.when(responseSpec.bodyToMono(Mockito.any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(responseMap));

    // WHEN
    Map<String, Object> result = worker.parseVpToken(token);

    // THEN
    assertNotNull(result);
    assertEquals("ok", result.get("status"));
}