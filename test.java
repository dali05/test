import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

class RequestWalletDataWorkerTest {

    @Mock
    private WebClient webClient;

    @Mock
    private RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private ResponseSpec responseSpec;

    @Mock
    private IdactoProperties props;

    @Mock
    private AccessRequestPersistencePort persistencePort;

    @Mock
    private NotifySyncCompletionUseCase notifyCompletionUseCase;

    @Mock
    private ActivatedJob job;

    @Mock
    private JobClient client;

    @InjectMocks
    private RequestWalletDataWorker worker;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        // Simuler props
        when(props.authorizationPath()).thenReturn("/auth");
        when(props.parseTokenPath()).thenReturn("/parse");
        when(props.templatePath()).thenReturn("/template.json");

        // Simuler template JSON
        ObjectNode template = objectMapper.createObjectNode();
        template.put("base", "value");
        // Remplacer le champ templatePayload par un mock
        worker = new RequestWalletDataWorker(webClient, props, persistencePort, notifyCompletionUseCase, objectMapper) {
            @Override
            protected ObjectNode loadTemplateJson() {
                return template;
            }
        };
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

        // Simuler WebClient GET pour getVpToken
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(org.mockito.Mockito.mock(org.springframework.web.reactive.function.client.WebClient.ResponseSpec.class));
        when(responseSpec.bodyToMono(String.class).block()).thenReturn("mockToken");

        // Simuler WebClient POST pour parseVpToken
        when(webClient.post()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.contentType(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        Map<String, Object> parsedMap = Map.of("parsed", "ok");
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(org.mockito.Mockito.mock(org.springframework.web.reactive.function.client.WebClient.ResponseSpec.class));
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)).block()).thenReturn(parsedMap);

        // Simuler client.complete
        var completeCmd = mock(JobClient.CompleteJobCommandStep1.class, RETURNS_DEEP_STUBS);
        when(client.newCompleteCommand(anyLong())).thenReturn(completeCmd);
        when(completeCmd.variables(any())).thenReturn(completeCmd);
        when(completeCmd.send()).thenReturn(CompletableFuture.completedFuture(null));

        worker.handleRequestWalletData(job, client);

        verify(client).newCompleteCommand(123L);
        verify(completeCmd).variables(parsedMap);
    }

    @Test
    void testHandleRequestWalletData_failure() {
        UUID requestId = UUID.randomUUID();
        Map<String, Object> vars = Map.of("requestId", requestId.toString());

        when(job.getVariablesAsMap()).thenReturn(vars);
        when(job.getKey()).thenReturn(456L);
        when(persistencePort.findById(requestId)).thenReturn(Optional.empty()); // provoque exception

        var failCmd = mock(JobClient.FailJobCommandStep1.class, RETURNS_DEEP_STUBS);
        when(client.newFailCommand(anyLong())).thenReturn(failCmd);
        when(failCmd.retries(anyInt())).thenReturn(failCmd);
        when(failCmd.errorMessage(anyString())).thenReturn(failCmd);
        when(failCmd.send()).thenReturn(CompletableFuture.completedFuture(null));

        worker.handleRequestWalletData(job, client);

        verify(notifyCompletionUseCase).notifyError(eq(requestId), any(Exception.class));
        verify(client).newFailCommand(456L);
    }
}
