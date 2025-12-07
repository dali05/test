@Test
void testSuccess() throws Exception {
    UUID id = UUID.randomUUID();

    when(job.getVariablesAsMap())
            .thenReturn(Map.of("requestId", id.toString()));

    // Persistence returns an AccessRequest OK
    AccessRequest ar = mock(AccessRequest.class);
    when(ar.getResponseCode()).thenReturn("K");
    when(persistence.findById(id)).thenReturn(Optional.of(ar));

    //
    // ---- Mock WebClient getVpToken() ----
    //
    when(idactoWebClient.get()).thenReturn(uriSpec);
    when(uriSpec.uri(any())).thenReturn(headersSpec);
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class))
            .thenReturn(Mono.just("TOKEN123"));

    //
    // ---- Mock WebClient parseVpToken() ----
    //
    when(idactoWebClient.post()).thenReturn(bodySpec);
    when(bodySpec.uri(props.parseTokenPath())).thenReturn(bodySpec);
    when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
    when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(Map.of("k", "v")));

    //
    // ---- Mock Zeebe Complete Command ----
    //
    CompleteJobCommandStep1 complete1 = mock(CompleteJobCommandStep1.class);
    FinalCommandStep<Void> finalComplete = mock(FinalCommandStep.class);

    ZeebeFuture<CompleteJobResponse> future = mock(ZeebeFuture.class);
    when(future.join()).thenReturn(null);

    when(jobClient.newCompleteCommand(job.getKey())).thenReturn(complete1);
    when(complete1.variables(any(Map.class))).thenReturn(finalComplete);
    when(finalComplete.send()).thenReturn(future);

    //
    // ---- Execute worker ----
    //
    worker.handleRequestWalletData(job, jobClient);

    //
    // ---- Verify ----
    //
    verify(complete1).variables(Map.of("k", "v"));
    verify(jobClient).newCompleteCommand(job.getKey());
}