@BeforeEach
void setUp() {

    props = new IdactoProperties();
    props.setParseTokenPath("/parse/token");

    // Create worker first
    RequestWalletDataWorker raw = new RequestWalletDataWorker(
            idactoWebClient,
            props,
            null,
            null,
            objectMapper
    );

    // Spy AFTER injection
    worker = Mockito.spy(raw);

    // Prevent real HTTP call in constructor
    Mockito.doReturn(new ObjectMapper().createObjectNode())
           .when(worker).loadTemplateJson();
}