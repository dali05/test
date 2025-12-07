@ExtendWith(MockitoExtension.class)
class RequestWalletDataWorkerTest {

    @Mock
    private WebClient idactoWebClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ObjectNode templatePayload;

    private RequestWalletDataWorker worker;

    @BeforeEach
    void setUp() {
        IdactoProperties props = new IdactoProperties();
        props.setParseTokenPath("/parse/token");

        worker = new RequestWalletDataWorker(
                idactoWebClient,
                props,
                null,
                null,
                objectMapper
        );
    }

    @Test
    void parseVpToken_shouldReturnMap_whenHttpCallIsSuccessful() throws Exception {

        // GIVEN
        String token = "abc123";

        ObjectNode finalPayload = Mockito.mock(ObjectNode.class);
        Mockito.when(objectMapper.createObjectNode()).thenReturn(finalPayload);

        Map<String, Object> responseMap = Map.of("status", "ok");

        // Mock WebClient chain
        Mockito.when(idactoWebClient.post()).thenReturn(requestBodyUriSpec);
        Mockito.when(requestBodyUriSpec.uri("/parse/token")).thenReturn(requestBodySpec);
        Mockito.when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        Mockito.when(requestBodySpec.bodyValue(finalPayload)).thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(responseMap));

        // WHEN
        Map<String, Object> result = worker.parseVpToken(token);

        // THEN
        assertNotNull(result);
        assertEquals("ok", result.get("status"));

        // Verify WebClient chain
        Mockito.verify(idactoWebClient).post();
        Mockito.verify(requestBodyUriSpec).uri("/parse/token");
        Mockito.verify(requestBodySpec).contentType(MediaType.APPLICATION_JSON);
        Mockito.verify(requestBodySpec).bodyValue(finalPayload);
        Mockito.verify(responseSpec).bodyToMono(Mockito.any(ParameterizedTypeReference.class));
    }
}