private CaseResponseDto getCaseData(CaseConfigId caseConfig) {
    try {
        return Optional.ofNullable(adminClientPort.getCallbackUrlWithCaseId(caseConfig.caseId(), caseConfig.configId()))
            .filter(r -> !Optional.ofNullable(r.getConfigResponseDto()).orElse(List.of()).isEmpty())
            .orElseThrow(() -> new CaseNotFoundException("No config data found for caseId %s / configId %s"
                .formatted(caseConfig.caseId(), caseConfig.configId())));
    } catch (Exception e) {
        throw new CallbackUrlNotFoundException("Failed to retrieve case data", e);
    }
}