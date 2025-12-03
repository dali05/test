@Override
public CompletableFuture‹String> register(UUID requestid) {
CompLetableFuture«String> future = new CompLetableFutures>();
LocalRegistry.put(requestid, future);
registrationTimestamps.put(requestId, System.currentTimeMitLis());
// Safety mechanism: auto-timeout after TTL
future.orTimeout(ttLSeconds, TimeUnit.SECONDS)
•exceptionally Throwable throwable -> {
Log. debug("Future auto-expired for requestid: () after f)s", requestid, ttlseconds); cleanup (requestid);
return null;
);
log. debug("Registered sync future for requestid: ( CTTL: Os, pending: f)", requestid, ttlSeconds, LocalRegistry.sizeO):
return future;
