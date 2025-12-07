private ZeebeFuture<CompleteJobResponse> mockZeebeFuture() {
    ZeebeFuture<CompleteJobResponse> future = mock(ZeebeFuture.class);

    // Si ton code appelle .join() dans RequestWalletDataWorker
    when(future.join()).thenReturn(new CompleteJobResponse());

    return future;
}


