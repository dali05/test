@Test
void testNotFound() {
    UUID id = UUID.randomUUID();

    when(job.getVariablesAsMap())
            .thenReturn(Map.of("requestId", id.toString()));

    when(persistence.findById(id))
            .thenReturn(Optional.empty());

    // --- Mock Zeebe fail command chain (your Zeebe version) ---

    FailJobCommandStep1 fail1 = mock(FailJobCommandStep1.class);
    FailJobCommandStep1.FailJobCommandStep2 fail2 =
            mock(FailJobCommandStep1.FailJobCommandStep2.class);

    // IMPORTANT → ZeebeFuture<FailJobResponse>
    ZeebeFuture<FailJobResponse> future = mock(ZeebeFuture.class);

    // join() must return a FailJobResponse (or null is acceptable)
    when(future.join()).thenReturn(null);

    // newFailCommand → step1
    when(jobClient.newFailCommand(job.getKey())).thenReturn(fail1);

    // retries → step2
    when(fail1.retries(0)).thenReturn(fail2);

    // errorMessage → SAME step2
    when(fail2.errorMessage(anyString())).thenReturn(fail2);

    // send() → ZeebeFuture<FailJobResponse>
    when(fail2.send()).thenReturn(future);

    // --- Execute ---
    worker.handleRequestWalletData(job, jobClient);

    // --- Verify ---
    verify(notifyUseCase).notifyError(eq(id), any());
}