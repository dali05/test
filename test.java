Test
    void getVpToken_shouldReturnToken_whenServiceResponds() {

        // GIVEN
        String expectedToken = "TOKEN-1234";

        Mockito.when(
                idactoWebClient
                        .get()
                        .uri(any(Function.class))
                        .retrieve()
                        .bodyToMono(String.class)
        ).thenReturn(Mono.just(expectedToken));

        // WHEN
        String result = worker.getVpToken("TX999", "200");

        // THEN
        assertEquals(expectedToken, result);
    }
}