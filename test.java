import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

class RequestWalletDataWorkerTest {

    @Mock
    private WebClient webClient;

    @Mock
    private IdactoProperties props;

    @Mock
    private AccessRequestPersistencePort persistencePort;

    @Mock
    private NotifySyncCompletionUseCase notifyCompletionUseCase;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ActivatedJob job;

    @Mock
    private JobClient client;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private RequestWalletDataWorker worker;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        // Simuler props
        when(props.authorizationPath()).thenReturn("/auth");
        when(props.parseTokenPath()).thenReturn("/parse");
        when(props.templatePath()).thenReturn("/template.json");

        // Simuler template JSON
        ObjectNode template = new ObjectMapper().createObjectNode();
        when(objectMapper.readTree(any(InputStream.class))).thenReturn(template);

        // Réinjecter worker avec templatePayload correct
        worker = new RequestWalletDataWorker(webClient, props, persistencePort, notifyCompletionUseCase, objectMapper);
    }

    @Test
    void testHandleRequestWalletData_success() {
        UUID requestId = UUID.randomUUID();
        Map<String, Object> vars = Map.of("requestId", requestId.toString());

        AccessRequest accessRequest = new AccessRequest();
        accessRequest.setResponseCode("200");

        when(job.getVariablesAsMap()).thenReturn(vars);
        when(job.getKey()).thenReturn(123L);
        when(persistencePort.findById(requestId)).thenReturn(Optional.of(accessRequest));

        // Simuler appel GET pour getVpToken
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("vpToken"));

        // Simuler appel POST pour parseVpToken
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        Map<String, Object> parsedMap = Map.of("parsedKey", "parsedValue");
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(parsedMap));

        // Simuler client.complete
        JobClient.CompleteJobCommandStep1 completeCommand = mock(JobClient.CompleteJobCommandStep1.class);
        when(client.newCompleteCommand(123L)).thenReturn(completeCommand);
        when(completeCommand.variables(parsedMap)).thenReturn(mock(JobClient.CompleteJobCommandStep2.class));
        when(completeCommand.variables(parsedMap).send()).thenReturn(CompletableFuture.completedFuture(null));

        // Exécution
        worker.handleRequestWalletData(job, client);

        // Vérification
        verify(client).newCompleteCommand(123L);
        verify(persistencePort).findById(requestId);
        verify(webClient).get();
        verify(webClient).post();
    }

    @Test
    void testHandleRequestWalletData_error() {
        UUID requestId = UUID.randomUUID();
        Map<String, Object> vars = Map.of("requestId", requestId.toString());

        when(job.getVariablesAsMap()).thenReturn(vars);
        when(job.getKey()).thenReturn(123L);
        when(persistencePort.findById(requestId)).thenReturn(Optional.empty()); // provoque exception

        // Simuler client.fail
        JobClient.FailJobCommandStep1 failCommand = mock(JobClient.FailJobCommandStep1.class);
        when(client.newFailCommand(123L)).thenReturn(failCommand);
        when(failCommand.retries(0)).thenReturn(failCommand);
        when(failCommand.errorMessage(anyString())).thenReturn(failCommand);
        when(failCommand.send()).thenReturn(CompletableFuture.completedFuture(null));

        worker.handleRequestWalletData(job, client);

        verify(notifyCompletionUseCase).notifyError(eq(requestId), any(Exception.class));
        verify(client).newFailCommand(123L);
    }
}
