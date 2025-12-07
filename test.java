@Test
void testNotFound() {
    UUID id = UUID.randomUUID();

    when(job.getVariablesAsMap())
            .thenReturn(Map.of("requestId", id.toString()));

    when(persistence.findById(id))
            .thenReturn(Optional.empty());

    // Fix
    FailJobCommandStep1 fail1 = mock(FailJobCommandStep1.class);
    FailJobCommandStep1.FailJobCommandStep2 fail2 =
            mock(FailJobCommandStep1.FailJobCommandStep2.class);
    FinalCommandStep<Void> finalFail = mock(FinalCommandStep.class);

    when(jobClient.newFailCommand(job.getKey())).thenReturn(fail1);
    when(fail1.retries(0)).thenReturn(fail2);
    when(fail2.errorMessage(anyString())).thenReturn(finalFail);
    when(finalFail.send()).thenReturn(CompletableFuture.completedFuture(null));

    worker.handleRequestWalletData(job, jobClient);

    verify(notifyUseCase).notifyError(eq(id), any());
}


@Test
void testSuccess() throws Exception {
    UUID id = UUID.randomUUID();

    when(job.getVariablesAsMap())
            .thenReturn(Map.of("requestId", id.toString()));

    AccessRequest ar = mock(AccessRequest.class);
    when(ar.getResponseCode()).thenReturn("K");
    when(persistence.findById(id)).thenReturn(Optional.of(ar));

    // WebClient mocks …
    // (je ne réécris pas ce bloc, il est OK)

    // Fix Zeebe complete chain
    CompleteJobCommandStep1 complete1 = mock(CompleteJobCommandStep1.class);
    FinalCommandStep<Void> completeFinal = mock(FinalCommandStep.class);

    when(jobClient.newCompleteCommand(job.getKey())).thenReturn(complete1);
    when(complete1.variables(any(Map.class))).thenReturn(completeFinal);
    when(completeFinal.send()).thenReturn(CompletableFuture.completedFuture(null));

    worker.handleRequestWalletData(job, jobClient);

    verify(complete1).variables(Map.of("k", "v"));
    verify(jobClient).newCompleteCommand(job.getKey());
}
