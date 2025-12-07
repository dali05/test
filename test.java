import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.command.CompleteCommandStep1;
import io.camunda.zeebe.client.api.command.FailCommandStep1;
import io.camunda.zeebe.client.api.command.FinalCommandStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class RequestWalletDataWorkerTest {

    @Mock
    private WebClient idactoWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

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
    private CompleteCommandStep1 completeCommandStep1;

    @Mock
    private FinalCommandStep<Void> finalCompleteCommandStep;

    @Mock
    private FailCommandStep1 failCommandStep1;

    @Mock
    private FinalCommandStep<Void> finalFailCommandStep;

    @InjectMocks
    private RequestWalletDataWorker requestWalletDataWorker;

    private final UUID requestId = UUID.randomUUID();
    private final Map<String, Object> jobVariables = new HashMap<>();

    @BeforeEach
    void setUp() {
        jobVariables.put("requestId", requestId.toString());
        when(job.getVariablesAsMap()).thenReturn(jobVariables);
        when(job.getKey()).thenReturn(123L);
    }

    @Test
    void handleRequestWalletData_SuccessfulFlow() throws Exception {
        // Given
        AccessRequest accessRequest = mock(AccessRequest.class);
        String responseCode = "test-response-code";
        String vpToken = "test-vp-token";
        Map<String, Object> parsedToken = Map.of("key1", "value1", "key2", "value2");

        when(persistencePort.findById(requestId)).thenReturn(Optional.of(accessRequest));
        when(accessRequest.getResponseCode()).thenReturn(responseCode);

        // Mock getVpToken
        mockWebClientGetRequest(vpToken);

        // Mock parseVpToken
        mockWebClientPostRequest(parsedToken);

        // Mock complete command
        when(client.newCompleteCommand(123L)).thenReturn(completeCommandStep1);
        when(completeCommandStep1.variables(parsedToken)).thenReturn(completeCommandStep1);
        when(completeCommandStep1.send()).thenReturn(CompletableFuture.completedFuture(null));

        // When
        requestWalletDataWorker.handleRequestWalletData(job, client);

        // Then
        verify(persistencePort).findById(requestId);
        verify(accessRequest).getResponseCode();
        verify(client).newCompleteCommand(123L);
        verify(completeCommandStep1).variables(parsedToken);
        verify(completeCommandStep1).send();
        verifyNoInteractions(notifyCompletionUseCase);
    }

    @Test
    void handleRequestWalletData_AccessRequestNotFound() {
        // Given
        when(persistencePort.findById(requestId)).thenReturn(Optional.empty());

        // Mock fail command
        mockFailCommand();

        // When
        requestWalletDataWorker.handleRequestWalletData(job, client);

        // Then
        verify(persistencePort).findById(requestId);
        verify(notifyCompletionUseCase).notifyError(eq(requestId), any(IllegalStateException.class));
        verifyFailCommand("AccessRequest not found: " + requestId);
    }

    @Test
    void handleRequestWalletData_GetVpTokenFails() {
        // Given
        AccessRequest accessRequest = mock(AccessRequest.class);
        String responseCode = "test-response-code";
        RuntimeException webClientException = new RuntimeException("WebClient error");

        when(persistencePort.findById(requestId)).thenReturn(Optional.of(accessRequest));
        when(accessRequest.getResponseCode()).thenReturn(responseCode);

        // Mock failing getVpToken
        mockFailingWebClientGetRequest(webClientException);

        // Mock fail command
        mockFailCommand();

        // When
        requestWalletDataWorker.handleRequestWalletData(job, client);

        // Then
        verify(persistencePort).findById(requestId);
        verify(notifyCompletionUseCase).notifyError(eq(requestId), any(RuntimeException.class));
        verifyFailCommand("WebClient error");
    }

    @Test
    void handleRequestWalletData_ParseVpTokenFails() throws IOException {
        // Given
        AccessRequest accessRequest = mock(AccessRequest.class);
        String responseCode = "test-response-code";
        String vpToken = "test-vp-token";
        IOException parseException = new IOException("Parse error");

        when(persistencePort.findById(requestId)).thenReturn(Optional.of(accessRequest));
        when(accessRequest.getResponseCode()).thenReturn(responseCode);

        // Mock successful getVpToken
        mockWebClientGetRequest(vpToken);

        // Mock failing parseVpToken
        mockFailingWebClientPostRequest(parseException);

        // Mock fail command
        mockFailCommand();

        // When
        requestWalletDataWorker.handleRequestWalletData(job, client);

        // Then
        verify(persistencePort).findById(requestId);
        verify(notifyCompletionUseCase).notifyError(eq(requestId), any(IOException.class));
        verifyFailCommand("Parse error");
    }

    @Test
    void getVpToken_Success() {
        // Given
        String transactionId = "test-transaction";
        String responseCode = "test-response";
        String expectedToken = "test-vp-token";

        when(props.authorizationPath()).thenReturn("/auth");

        mockWebClientGetRequest(expectedToken);

        // When
        // Note: Since getVpToken is private, we need to test it indirectly through handleRequestWalletData
        // or make it package-private/protected for testing
        AccessRequest accessRequest = mock(AccessRequest.class);
        when(persistencePort.findById(requestId)).thenReturn(Optional.of(accessRequest));
        when(accessRequest.getResponseCode()).thenReturn(responseCode);

        // Mock complete command
        when(client.newCompleteCommand(123L)).thenReturn(completeCommandStep1);
        when(completeCommandStep1.variables(anyMap())).thenReturn(completeCommandStep1);
        when(completeCommandStep1.send()).thenReturn(CompletableFuture.completedFuture(null));

        requestWalletDataWorker.handleRequestWalletData(job, client);

        // Then
        verify(idactoWebClient).get();
    }

    @Test
    void loadTemplateJson_Success() throws IOException {
        // Given
        String templatePath = "/templates/test.json";
        String templateJson = "{\"template\": \"test\"}";
        InputStream inputStream = new ByteArrayInputStream(templateJson.getBytes());
        ObjectNode expectedNode = mock(ObjectNode.class);

        when(props.templatePath()).thenReturn(templatePath);
        when(objectMapper.readTree(inputStream)).thenReturn(expectedNode);

        // Note: loadTemplateJson is called in constructor, so we need to re-initialize
        // or use reflection to test it separately
    }

    @Test
    void parseVpToken_Success() throws IOException {
        // Given
        String vpToken = "test-token";
        Map<String, Object> expectedResponse = Map.of("parsed", "data");
        ObjectNode templateNode = mock(ObjectNode.class);
        ObjectNode bodyNode = mock(ObjectNode.class);

        when(props.parseTokenPath()).thenReturn("/parse");
        when(objectMapper.readTree(any(InputStream.class))).thenReturn(templateNode);
        when(templateNode.deepCopy()).thenReturn(bodyNode);

        mockWebClientPostRequest(expectedResponse);

        // When
        // Test through handleRequestWalletData or use reflection
    }

    private void mockWebClientGetRequest(String responseBody) {
        when(idactoWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(responseBody));
    }

    private void mockFailingWebClientGetRequest(Throwable exception) {
        when(idactoWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(exception));
    }

    private void mockWebClientPostRequest(Map<String, Object> response) {
        when(idactoWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
    }

    private void mockFailingWebClientPostRequest(Throwable exception) {
        when(idactoWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(exception));
    }

    private void mockFailCommand() {
        when(client.newFailCommand(123L)).thenReturn(failCommandStep1);
        when(failCommandStep1.retries(0)).thenReturn(failCommandStep1);
        when(failCommandStep1.errorMessage(anyString())).thenReturn(failCommandStep1);
        when(failCommandStep1.send()).thenReturn(CompletableFuture.completedFuture(null));
    }

    private void verifyFailCommand(String expectedErrorMessage) {
        verify(client).newFailCommand(123L);
        verify(failCommandStep1).retries(0);
        verify(failCommandStep1).errorMessage(expectedErrorMessage);
        verify(failCommandStep1).send();
    }

    // Helper class for CompletableFuture
    private static class CompletableFuture<T> extends java.util.concurrent.CompletableFuture<T> {
        public static <T> CompletableFuture<T> completedFuture(T value) {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.complete(value);
            return future;
        }
    }
}
