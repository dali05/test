@ExtendWith(MockitoExtension.class)
class RequestWalletDataWorkerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient idactoWebClient;

    private RequestWalletDataWorker worker;
    private IdactoProperties props;

    @BeforeEach
    void setUp() {
        props = new IdactoProperties();
        props.setAuthorizationPath("/auth/token");

        worker = Mockito.spy(new RequestWalletDataWorker(
                idactoWebClient,
                props,
                null,
                null,
                new ObjectMapper()
        ));

        // On évite que loadTemplateJson() charge un vrai fichier
        Mockito.doReturn(new ObjectMapper().createObjectNode())
                .when(worker)
                .loadTemplateJson();
    }

    @Test
    void getVpToken_shouldReturnToken_whenServiceResponds() {

        // GIVEN
        String expectedToken = "TOKEN-1234";

        Mockito.when(
                idactoWebClient
                        .get()
                        .uri(any(Function.class))
                        .retrieve()
                        .bodyToMono(String.class)
        ).thenReturn(Mono.just(expectedToken));

        // WHEN
        String result = worker.getVpToken("TX999", "200");

        // THEN
        assertEquals(expectedToken, result);
    }
}