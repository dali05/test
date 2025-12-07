@ExtendWith(MockitoExtension.class)
class RequestWalletDataWorkerTest {

    @Mock WebClient webClient;

    // IMPORTANT — on enlève complètement les génériques "<?>"
    @Mock WebClient.RequestHeadersUriSpec uriSpec;
    @Mock WebClient.RequestHeadersSpec headersSpec;
    @Mock WebClient.RequestBodySpec bodySpec;
    @Mock WebClient.ResponseSpec responseSpec;

    @Mock IdactoProperties props;
    @Mock AccessRequestPersistencePort persistencePort;
    @Mock NotifySyncCompletionUseCase notifyUseCase;

    @Mock ActivatedJob job;
    @Mock JobClient jobClient;

    @Mock FinalCommandStep completeStep;
    @Mock FinalCommandStep failStep;

    RequestWalletDataWorker worker;
    ObjectNode template;

    @BeforeEach
    void setup() throws Exception {

        worker = Mockito.spy(new RequestWalletDataWorker(
                webClient,
                props,
                persistencePort,
                notifyUseCase,
                new ObjectMapper()
        ));

        template = new ObjectMapper().createObjectNode();
        template.put("field", "value");

        doReturn(template).when(worker).loadTemplateJson();

        when(props.authorizationPath()).thenReturn("/auth");
        when(props.parseTokenPath()).thenReturn("/parse");
    }

    // -----------------------------
    // SUCCESS
    // -----------------------------
    @Test
    void testSuccess() throws Exception {

        UUID id = UUID.randomUUID();
        when(job.getVariablesAsMap()).thenReturn(Map.of("requestId", id.toString()));

        AccessRequest ar = mock(AccessRequest.class);
        when(ar.getResponseCode()).thenReturn("200");
        when(persistencePort.findById(id)).thenReturn(Optional.of(ar));

        // Mock GET
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("TOKEN"));

        // Mock POST
        when(webClient.post()).thenReturn(bodySpec);
        when(bodySpec.uri("/parse")).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(Map.of("ok", true)));

        // Mock Zeebe
        when(jobClient.newCompleteCommand(job.getKey())).thenReturn(completeStep);
        when(completeStep.variables(any())).thenReturn(completeStep);
        when(completeStep.send()).thenReturn(CompletableFuture.completedFuture(null));

        worker.handleRequestWalletData(job, jobClient);

        verify(jobClient).newCompleteCommand(job.getKey());
        verify(completeStep).variables(Map.of("ok", true));
    }

    // -----------------------------
    // NOT FOUND
    // -----------------------------
    @Test
    void testNotFound() {

        UUID id = UUID.randomUUID();
        when(job.getVariablesAsMap()).thenReturn(Map.of("requestId", id.toString()));
        when(persistencePort.findById(id)).thenReturn(Optional.empty());

        when(jobClient.newFailCommand(job.getKey())).thenReturn(failStep);
        when(failStep.retries(0)).thenReturn(failStep);
        when(failStep.errorMessage(any())).thenReturn(failStep);
        when(failStep.send()).thenReturn(CompletableFuture.completedFuture(null));

        worker.handleRequestWalletData(job, jobClient);

        verify(notifyUseCase).notifyError(eq(id), any());
    }

    // -----------------------------
    // buildFinalPayload
    // -----------------------------
    @Test
    void testBuildFinalPayload() {
        ObjectNode result = worker.buildFinalPayload("XYZ");

        assertEquals("XYZ", result.get("vptoken").asText());
        assertEquals("value", result.get("field").asText());
    }

    // -----------------------------
    // getVpToken error
    // -----------------------------
    @Test
    void testGetVpToken_Error() {

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenThrow(new RuntimeException("fail"));

        assertThrows(RuntimeException.class, () -> worker.getVpToken("t", "r"));
    }

    // -----------------------------
    // parseVpToken error
    // -----------------------------
    @Test
    void testParseVpToken_Error() {

        when(webClient.post()).thenReturn(bodySpec);
        when(bodySpec.uri("/parse")).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenThrow(new RuntimeException("X"));

        assertThrows(RuntimeException.class, () -> worker.parseVpToken("AAA"));
    }
}
