package com.example.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
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
    private JobClient jobClient;

    @Mock
    private ActivatedJob job;

    // WebClient mocks
    @Mock private WebClient.RequestHeadersUriSpec<?> getSpec;
    @Mock private WebClient.RequestHeadersSpec<?> retrieveSpecGet;
    @Mock private WebClient.ResponseSpec responseSpecGet;

    @Mock private WebClient.RequestBodyUriSpec postSpec;
    @Mock private WebClient.RequestBodySpec bodySpec;
    @Mock private WebClient.RequestHeadersSpec<?> retrieveSpecPost;
    @Mock private WebClient.ResponseSpec responseSpecPost;

    private ObjectMapper objectMapper = new ObjectMapper();

    private RequestWalletDataWorker worker;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        when(props.authorizationPath()).thenReturn("/authorize");
        when(props.parseTokenPath()).thenReturn("/parse");
        when(props.templatePath()).thenReturn("/template.json");

        worker = Mockito.spy(new RequestWalletDataWorker(
                webClient,
                props,
                persistencePort,
                notifyCompletionUseCase,
                objectMapper
        ));

        // Fake template JSON loaded instead of real classpath file
        ObjectNode fakeTemplate = JsonNodeFactory.instance.objectNode();
        fakeTemplate.put("staticField", "value");
        doReturn(fakeTemplate).when(worker).loadTemplateJson();
    }

    // -------------------------------------------------------------
    //                       SUCCESS TEST
    // -------------------------------------------------------------
    @Test
    void testSuccess() throws Exception {

        UUID requestId = UUID.randomUUID();
        Map<String, Object> vars = Map.of("requestId", requestId.toString());

        when(job.getVariablesAsMap()).thenReturn(vars);
        when(job.getKey()).thenReturn(11L);

        // persistence
        AccessRequest req = mock(AccessRequest.class);
        when(req.getResponseCode()).thenReturn("RCODE");
        when(persistencePort.findById(requestId)).thenReturn(Optional.of(req));

        // WebClient GET
        when(webClient.get()).thenReturn(getSpec);
        when(getSpec.uri(any())).thenReturn(retrieveSpecGet);
        when(retrieveSpecGet.retrieve()).thenReturn(responseSpecGet);
        when(responseSpecGet.bodyToMono(String.class)).thenReturn(Mono.just("VP_TOKEN"));

        // WebClient POST
        when(webClient.post()).thenReturn(postSpec);
        when(postSpec.uri("/parse")).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any(ObjectNode.class))).thenReturn(retrieveSpecPost);
        when(retrieveSpecPost.retrieve()).thenReturn(responseSpecPost);

        Map<String,Object> parsedMap = Map.of("field", "parsedValue");
        when(responseSpecPost.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(parsedMap));

        // Mock JobClient newCompleteCommand()
        JobClient.NewCompleteCommandStep1 completeCmd =
                mock(JobClient.NewCompleteCommandStep1.class, RETURNS_DEEP_STUBS);

        when(jobClient.newCompleteCommand(11L)).thenReturn(completeCmd);
        when(completeCmd.variables(parsedMap)).thenReturn(completeCmd);
        when(completeCmd.send()).thenReturn(CompletableFuture.completedFuture(null));

        // Execute worker
        worker.handleRequestWalletData(job, jobClient);

        // verify OK
        verify(completeCmd).variables(parsedMap);
        verify(completeCmd).send();
        verify(notifyCompletionUseCase, never()).notifyError(any(), any());
    }


    // -------------------------------------------------------------
    //                       ERROR TEST
    // -------------------------------------------------------------
    @Test
    void testError() {

        UUID requestId = UUID.randomUUID();
        Map<String,Object> vars = Map.of("requestId", requestId.toString());

        when(job.getVariablesAsMap()).thenReturn(vars);
        when(job.getKey()).thenReturn(22L);

        AccessRequest req = mock(AccessRequest.class);
        when(req.getResponseCode()).thenReturn("RCODE");
        when(persistencePort.findById(requestId)).thenReturn(Optional.of(req));

        // Force WebClient.get() to throw an error
        when(webClient.get()).thenThrow(new RuntimeException("Simulated failure"));

        // Mock fail command chain
        JobClient.NewFailCommandStep1 failCmd =
                mock(JobClient.NewFailCommandStep1.class, RETURNS_DEEP_STUBS);

        when(jobClient.newFailCommand(22L)).thenReturn(failCmd);
        when(failCmd.retries(0)).thenReturn(failCmd);
        when(failCmd.errorMessage(any())).thenReturn(failCmd);
        when(failCmd.send()).thenReturn(CompletableFuture.completedFuture(null));

        // Execute
        worker.handleRequestWalletData(job, jobClient);

        // Validate error path
        verify(notifyCompletionUseCase).notifyError(eq(requestId), any());
        verify(failCmd).retries(0);
        verify(failCmd).errorMessage(any());
        verify(failCmd).send();
    }
}
