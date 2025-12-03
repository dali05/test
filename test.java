
@JobWorker(
    type = "generateUriRequest",
    autoComplete = false,
    timeout = 20000      // 🔥 20 secondes pour éviter les expirations
)
public void handleGenerateUriRequest(final ActivatedJob job, final JobClient client) {

    Map<String, Object> vars = job.getVariablesAsMap();
    String requestIdStr = (String) vars.get("requestId");
    UUID requestId = UUID.fromString(requestIdStr);

    try {

        log.info("🚀 Starting URI generation for requestId {}", requestId);

        // 1️⃣ Code métier (sans rollback Spring !)
        String generatedUri = processUriGeneration(requestId, vars);

        // 2️⃣ Report au mécanisme sync future
        notifyCompletionUseCase.notifySuccess(requestId, generatedUri);

        // 3️⃣ Terminer le job Zeebe une seule fois
        client
            .newCompleteCommand(job.getKey())
            .variables(Map.of("requestUriDeepLink", generatedUri))
            .send()
            .join();

        log.info("✔ Job generateUriRequest COMPLETED for requestId {}", requestId);

    } catch (Exception e) {

        log.error("❌ ERROR generateUriRequest for requestId {}", requestId, e);

        // 4️⃣ On échoue le job une seule fois
        client
            .newFailCommand(job.getKey())
            .retries(0)   // aucun retry
            .errorMessage(e.getMessage())
            .send()
            .join();
    }
}