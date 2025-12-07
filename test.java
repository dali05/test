@Test
void testSuccess() throws Exception {
    // GIVEN
    UUID id = UUID.randomUUID();

    // variables du job
    when(job.getVariablesAsMap())
            .thenReturn(Map.of("requestId", id.toString()));

    // AccessRequest trouvé en base
    AccessRequest ar = mock(AccessRequest.class);
    when(ar.getResponseCode()).thenReturn("K");
    when(persistence.findById(id)).thenReturn(Optional.of(ar));

    // On ne teste pas WebClient ici : on stub les méthodes du worker
    doReturn("TOKEN123")
            .when(worker)
            .getVpToken(anyString(), anyString());

    doReturn(Map.of("k", "v"))
            .when(worker)
            .parseVpToken(anyString());

    // -----------------------------
    // Mock Zeebe complete command
    // -----------------------------
    CompleteJobCommandStep1 completeStep = mock(CompleteJobCommandStep1.class);
    ZeebeFuture<CompleteJobResponse> completeFuture = mock(ZeebeFuture.class);

    when(jobClient.newCompleteCommand(job.getKey())).thenReturn(completeStep);

    // Dans TA version de Zeebe, variables(...) renvoie le même step
    when(completeStep.variables(any(Map.class))).thenReturn(completeStep);

    // send() -> ZeebeFuture<CompleteJobResponse>
    when(completeStep.send()).thenReturn(completeFuture);

    // join() ne fait rien de spécial dans le test
    when(completeFuture.join()).thenReturn(null);

    // WHEN
    worker.handleRequestWalletData(job, jobClient);

    // THEN
    // le job est complété avec le bon payload
    verify(jobClient).newCompleteCommand(job.getKey());
    verify(completeStep).variables(Map.of("k", "v"));

    // et aucune erreur n'est notifiée
    verify(notifyUseCase, never()).notifyError(any(), any());
}