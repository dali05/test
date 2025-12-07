package com.example.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.*;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RequestWalletDataWorkerTest {

    @Mock
    private WebClient idactoWebClient;

    @Mock
    private IdactoProperties props;

    @Mock
    private AccessRequestPersistencePort persistencePort;

    @Mock
    private NotifySyncCompletionUseCase notifyCompletionUseCase;

    @Mock
    private JobClient jobClient;

    @Mock
    private ActivatedJob job;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ObjectMapper objectMapper = new ObjectMapper();

    private RequestWalletDataWorker worker;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // mock props
        when(props.authorizationPath()).thenReturn("/authorize");
        when(props.parseTokenPath()).thenReturn("/parse");
        when(props.templatePath()).thenReturn("/template.json");

        // instantiate worker but bypass loadTemplateJson()
        worker = Mockito.spy(new RequestWalletDataWorker(
                idactoWebClient, props, persistencePort, notifyCompletionUseCase, objectMapper
        ));

        // inject fake template in spy
        ObjectNode fakeTemplate = JsonNodeFactory.instance.objectNode();
        fakeTemplate.put("staticField", "value");
        Mockito.doReturn(fakeTemplate).when(worker).loadTemplateJson();
    }


    // ---------------------------------------------------------
    //                 TEST SCENARIO SUCCESS
    // ---------------------------------------------------------
    @Test
    void shouldHandleRequestWalletDataSuccessfully() throws Exception {

        UUID requestId = UUID.randomUUID();

        Map<String,Object> vars = new HashMap<>();
        vars.put("requestId", requestId.toString());

        AccessRequest accessRequest = mock(AccessRequest.class);
        when(accessRequest.getResponseCode()).thenReturn("RCODE");

        when(job.getVariablesAsMap()).thenReturn(vars);
        when(job.getKey()).thenReturn(10L);

        // persistence port
        when(persistencePort.findById(requestId)).thenReturn(Optional.of(accessRequest));

        // mock WebClient GET → return vpToken
        when(idactoWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("VP_TOKEN"));

        // mock WebClient POST → return parsed map
        Map<String,Object> parsedMap = Map.of("field", "parsedValue");

        when(idactoWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/parse")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(ObjectNode.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Mockito.<ParameterizedTypeReference<Map<String, Object>>>any()))
                .thenReturn(Mono.just(parsedMap));

        // Mock completion command
        JobClient.FinalCommandStep finalCmd = mock(JobClient.FinalCommandStep.class);
        when(jobClient.newCompleteCommand(10L)).thenReturn(mock(JobClient.FailCommandStep.class, RETURNS_DEEP_STUBS));
        when(jobClient.newCompleteCommand(10L).variables(parsedMap).send()).thenReturn(CompletableFuture.completedFuture(null));

        // execute
        worker.handleRequestWalletData(job, jobClient);

        // verify: job completed
        verify(jobClient.newCompleteCommand(10L).variables(parsedMap)).send();
        verify(notifyCompletionUseCase, never()).notifyError(any(), any());
    }


    // ---------------------------------------------------------
    //                 TEST SCENARIO ERROR
    // ---------------------------------------------------------
    @Test
    void shouldHandleErrorDuringProcessing() {

        UUID requestId = UUID.randomUUID();
        Map<String,Object> vars = Map.of("requestId", requestId.toString());

        when(job.getVariablesAsMap()).thenReturn(vars);
        when(job.getKey()).thenReturn(20L);

        AccessRequest accessRequest = mock(AccessRequest.class);
        when(accessRequest.getResponseCode()).thenReturn("RCODE");
        when(persistencePort.findById(requestId)).thenReturn(Optional.of(accessRequest));

        // force WebClient.get() to throw an exception
        when(idactoWebClient.get()).thenThrow(new RuntimeException("Simulated error"));

        // mock fail command
        when(jobClient.newFailCommand(20L)).thenReturn(mock(JobClient.FailCommandStep.class, RETURNS_DEEP_STUBS));
        when(jobClient.newFailCommand(20L).retries(0).errorMessage(any()).send())
                .thenReturn(CompletableFuture.completedFuture(null));

        // run
        worker.handleRequestWalletData(job, jobClient);

        // verify failure flow
        verify(notifyCompletionUseCase).notifyError(eq(requestId), any(Exception.class));
        verify(jobClient.newFailCommand(20L).retries(0)).errorMessage(any());
    }
}
