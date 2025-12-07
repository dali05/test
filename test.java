@Test
void testNotFound() {
    UUID id = UUID.randomUUID();

    when(job.getVariablesAsMap())
            .thenReturn(Map.of("requestId", id.toString()));

    when(persistence.findById(id))
            .thenReturn(Optional.empty());

    // --- Mock Zeebe Fail Command (version Zeebe où errorMessage returns Step2) ---

    FailJobCommandStep1 fail1 = mock(FailJobCommandStep1.class);
    FailJobCommandStep1.FailJobCommandStep2 fail2 =
            mock(FailJobCommandStep1.FailJobCommandStep2.class);

    ZeebeFuture<Void> future = mock(ZeebeFuture.class);
    when(future.join()).thenReturn(null);

    // Start chain
    when(jobClient.newFailCommand(job.getKey())).thenReturn(fail1);

    // retries → Step2
    when(fail1.retries(0)).thenReturn(fail2);

    // errorMessage → returns the same Step2
    when(fail2.errorMessage(anyString())).thenReturn(fail2);

    // send → returns ZeebeFuture
    when(fail2.send()).thenReturn(future);

    // --- Execute ---
    worker.handleRequestWalletData(job, jobClient);

    // --- Verify ---
    verify(notifyUseCase).notifyError(eq(id), any());
}