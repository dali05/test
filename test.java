@Test
void testGetVpToken() {
    // GIVEN
    when(webClient.get()).thenReturn(uriSpec);
    when(uriSpec.uri(any())).thenReturn(headersSpec);
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class))
            .thenReturn(Mono.just("TOKEN123"));

    // WHEN
    String result = worker.getVpToken("tx123", "OK");

    // THEN
    assertEquals("TOKEN123", result);
    verify(webClient).get();
    verify(uriSpec).uri(any());
    verify(headersSpec).retrieve();
}

fakeTemplate = new ObjectMapper().createObjectNode();
fakeTemplate.put("field", "value");
doReturn(fakeTemplate).when(worker).loadTemplateJson();
worker.templatePayload = fakeTemplate;



@Test
void testBuildFinalPayload() {
    // GIVEN
    worker.templatePayload = fakeTemplate;

    // WHEN
    ObjectNode result = worker.buildFinalPayload("MYTOKEN");

    // THEN
    assertEquals("MYTOKEN", result.get("vptoken").asText());
    assertEquals("value", result.get("field").asText());
}

@Test
void testParseVpToken() throws Exception {
    // GIVEN
    worker.templatePayload = fakeTemplate;

    WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);

    when(webClient.post()).thenReturn(bodyUriSpec);
    when(bodyUriSpec.uri(props.parseTokenPath())).thenReturn(bodySpec);
    when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
    when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
    when(headersSpec.retrieve()).thenReturn(responseSpec);

    Map<String,Object> expectedMap = Map.of("k", "v");
    when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(expectedMap));

    // WHEN
    Map<String,Object> result = worker.parseVpToken("AAA");

    // THEN
    assertEquals("v", result.get("k"));
    verify(webClient).post();
    verify(bodyUriSpec).uri(anyString());
    verify(headersSpec).retrieve();
}


