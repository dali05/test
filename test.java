package com.bnpp.pf.walle.access.process.adapter.in.workers;

import com.bnpp.pf.walle.access.domain.entity.AccessRequest;
import com.bnpp.pf.walle.access.process.app.port.in.NotifySyncCompletionUseCase;
import com.bnpp.pf.walle.access.process.app.port.out.AccessRequestPersistencePort;
import com.bnpp.pf.walle.access.process.config.idacto.IdactoProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.command.CompleteJobCommandStep1;
import io.camunda.zeebe.client.api.command.FailJobCommandStep1;
import io.camunda.zeebe.client.api.command.FinalCommandStep;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for RequestWalletDataWorker.
 */
@ExtendWith(MockitoExtension.class)
class RequestWalletDataWorkerTest {

    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec<?> uriSpec;
    @Mock
    private WebClient.RequestHeadersSpec<?> headersSpec;
    @Mock
    private WebClient.RequestBodySpec bodySpec;
    @Mock
    private WebClient.RequestBodyUriSpec bodyUriSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private IdactoProperties props;
    @Mock
    private AccessRequestPersistencePort persistence;
    @Mock
    private NotifySyncCompletionUseCase notifyUseCase;

    @Mock
    private ActivatedJob job;
    @Mock
    private JobClient jobClient;

    // Zeebe complete chain
    @Mock
    private CompleteJobCommandStep1 completeCommandStep1;
    @Mock
    private FinalCommandStep<Void> finalCompleteCommandStep;

    // Zeebe fail chain
    @Mock
    private FailJobCommandStep1 failCommandStep1;
    @Mock
    private FailJobCommandStep1.FailJobCommandStep2 failCommandStep2;
    @Mock
    private FinalCommandStep<Void> finalFailCommandStep;

    private RequestWalletDataWorker worker;
    private ObjectNode fakeTemplate;

    @BeforeEach
    void setUp() throws Exception {
        fakeTemplate = new ObjectMapper().createObjectNode();
        fakeTemplate.put("field", "value");

        worker = Mockito.spy(new RequestWalletDataWorker(
                webClient,
                props,
                persistence,
                notifyUseCase,
                new ObjectMapper()
        ));

        // le worker lira ce template
        doReturn(fakeTemplate).when(worker).loadTemplateJson();

        when(props.authorizationPath()).thenReturn("/auth");
        when(props.parseTokenPath()).thenReturn("/parse");
    }

    @Test
    void testSuccess() throws Exception {
        UUID id = UUID.randomUUID();

        when(job.getVariablesAsMap()).thenReturn(Map.of("requestId", id.toString()));

        AccessRequest ar = mock(AccessRequest.class);
        when(ar.getResponseCode()).thenReturn("K");
        when(persistence.findById(id)).thenReturn(Optional.of(ar));

        // --- WebClient GET pour récupérer le token ---
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(URI.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("TOKEN123"));

        // --- WebClient POST pour parser le token ---
        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri("/parse")).thenReturn(bodyUriSpec);
        when(bodyUriSpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodyUriSpec);
        when(bodyUriSpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(Map.of("k", "v")));

        // --- Zeebe complete chain ---
        when(jobClient.newCompleteCommand(job.getKey())).thenReturn(completeCommandStep1);
        when(completeCommandStep1.variables(any(Map.class))).thenReturn(finalCompleteCommandStep);

        @SuppressWarnings("unchecked")
        ZeebeFuture<Void> completeFuture =
                (ZeebeFuture<Void>) CompletableFuture.completedFuture(null);
        when(finalCompleteCommandStep.send()).thenReturn(completeFuture);

        // act
        worker.handleRequestWalletData(job, jobClient);

        // assert
        verify(completeCommandStep1).variables(Map.of("k", "v"));
        verify(jobClient).newCompleteCommand(job.getKey());
        verifyNoMoreInteractions(jobClient);
    }

    @Test
    void testNotFound() {
        UUID id = UUID.randomUUID();

        when(job.getVariablesAsMap()).thenReturn(Map.of("requestId", id.toString()));
        when(persistence.findById(id)).thenReturn(Optional.empty());

        // --- Zeebe fail chain ---
        when(jobClient.newFailCommand(job.getKey())).thenReturn(failCommandStep1);
        when(failCommandStep1.retries(0)).thenReturn(failCommandStep2);
        when(failCommandStep2.errorMessage(anyString())).thenReturn(finalFailCommandStep);

        @SuppressWarnings("unchecked")
        ZeebeFuture<Void> failFuture =
                (ZeebeFuture<Void>) CompletableFuture.completedFuture(null);
        when(finalFailCommandStep.send()).thenReturn(failFuture);

        // act
        worker.handleRequestWalletData(job, jobClient);

        // assert
        verify(notifyUseCase).notifyError(eq(id), any());
        verify(jobClient).newFailCommand(job.getKey());
    }

    @Test
    void testGetVpTokenError() {
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(URI.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenThrow(new RuntimeException("FAIL"));

        assertThrows(RuntimeException.class,
                () -> worker.getVpToken("a", "b"));
    }

    @Test
    void testParseVpTokenError() throws Exception {
        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri("/parse")).thenReturn(bodyUriSpec);
        when(bodyUriSpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodyUriSpec);
        when(bodyUriSpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenThrow(new RuntimeException("POST_FAIL"));

        assertThrows(RuntimeException.class,
                () -> worker.parseVpToken("XYZ"));
    }

    @Test
    void testBuildFinalPayload() {
        ObjectNode out = worker.buildFinalPayload("AAA");

        assertEquals("AAA", out.get("vptoken").asText());
        assertEquals("value", out.get("field").asText());
    }
}
