Test
    void getVpToken_shouldReturnString() {
        // Arrange
        String transactionId = "T123";
        String responseCode = "00";
        String expectedResponse = "my-token";

        // Chaînage WebClient mocké :
        when(idactoWebClient.get()).thenReturn(headersUriSpec);

        // Mock du .uri(Function)
        when(headersUriSpec.uri(any(Function.class))).thenReturn(headersSpec);

        // Mock du .retrieve()
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        // Mock du .bodyToMono()
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(expectedResponse));

        // Act
        String result = worker.getVpToken(transactionId, responseCode);

        // Assert
        assertEquals(expectedResponse, result);
    }
}