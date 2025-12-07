Test
void testWalletRequest() {

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

    when(responseSpec.bodyToMono(String.class))
            .thenReturn(Mono.just("mock-response"));

    // call method under test
    var result = worker.requestWalletData();

    assertEquals("mock-response", result.block());