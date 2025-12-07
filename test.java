package com.bnpp.pf.walle.access.process.adapter.in.workers;

import com.bnpp.pf.common.logging.SecureLogger;
import com.bnpp.pf.walle.access.domain.entity.AccessRequest;
import com.bnpp.pf.walle.access.process.app.port.in.NotifySyncCompletionUseCase;
import com.bnpp.pf.walle.access.process.app.port.out.AccessRequestPersistencePort;
import com.bnpp.pf.walle.access.process.config.idacto.IdactoProperties;

import io.camunda.zeebe.client.api.command.FinalCommandStep;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestWalletDataWorkerTest {

    @Mock WebClient webClient;
    @Mock WebClient.RequestHeadersUriSpec<?> uriSpec;
    @Mock WebClient.RequestHeadersSpec<?> headersSpec;
    @Mock WebClient.RequestBodySpec bodySpec;
    @Mock WebClient.ResponseSpec responseSpec;

    @Mock IdactoProperties props;
    @Mock AccessRequestPersistencePort persistencePort;
    @Mock NotifySyncCompletionUseCase notifyUseCase;
    @Mock ObjectMapper mapper;

    @Mock ActivatedJob job;
    @Mock JobClient jobClient;

    @Mock FinalCommandStep<?> completeStep;
    @Mock FinalCommandStep<?> failStep;

    RequestWalletDataWorker worker;

    ObjectNode fakeTemplate;

    @BeforeEach
    void setup() throws Exception {

        // Fake JSON template
        fakeTemplate = new ObjectMapper().createObjectNode();
        fakeTemplate.put("field", "value");

        // Mock loadTemplateJson by spying the worker
        worker = Mockito.spy(new RequestWalletDataWorker(
                webClient,
                props,
                persistencePort,
                notifyUseCase,
                new ObjectMapper()
        ));

        doReturn(fakeTemplate).when(worker).loadTemplateJson();

        // Mock props
        when(props.authorizationPath()).thenReturn("/auth");
        when(props.parseTokenPath()).thenReturn("/parse");
    }

    // -------------------------------
    // SUCCESS CASE
    // -------------------------------
    @Test
    void testHandleRequestWalletData_Success() throws Exception {

        UUID id = UUID.randomUUID();

        // Mock job variables
        when(job.getVariablesAsMap()).thenReturn(Map.of("requestId", id.toString()));

        AccessRequest ar = mock(AccessRequest.class);
        when(ar.getResponseCode()).thenReturn("200");
        when(persistencePort.findById(id)).thenReturn(Optional.of(ar));

        // Mock WebClient GET (getVpToken)
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("TOKEN-ABC"));

        // Mock WebClient POST (parseVpToken)
        when(webClient.post()).thenReturn(bodySpec);
        when(bodySpec.uri("/parse")).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(Map.of("k", "v")));

        // Mock Zeebe success
        when(jobClient.newCompleteCommand(job.getKey())).thenReturn(completeStep);
        when(completeStep.variables(any())).thenReturn(completeStep);
        when(completeStep.send()).thenReturn(CompletableFuture.completedFuture(null));

        worker.handleRequestWalletData(job, jobClient);

        verify(jobClient).newCompleteCommand(job.getKey());
        verify(completeStep).variables(Map.of("k", "v"));
    }

    // -------------------------------
    // FAIL: AccessRequest not found
    // -------------------------------
    @Test
    void testHandleRequestWalletData_NotFound() {

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

    // -------------------------------
    // FAIL: parseVpToken throws
    // -------------------------------
    @Test
    void testHandleRequestWalletData_ParseError() throws Exception {

        UUID id = UUID.randomUUID();

        when(job.getVariablesAsMap()).thenReturn(Map.of("requestId", id.toString()));

        AccessRequest ar = mock(AccessRequest.class);
        when(ar.getResponseCode()).thenReturn("200");
        when(persistencePort.findById(id)).thenReturn(Optional.of(ar));

        // Mock VP token OK
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("TOKEN"));

        // parseVpToken throws
        doThrow(new RuntimeException("parse error")).when(worker).parseVpToken("TOKEN");

        when(jobClient.newFailCommand(job.getKey())).thenReturn(failStep);
        when(failStep.retries(0)).thenReturn(failStep);
        when(failStep.errorMessage(any())).thenReturn(failStep);
        when(failStep.send()).thenReturn(CompletableFuture.completedFuture(null));

        worker.handleRequestWalletData(job, jobClient);

        verify(notifyUseCase).notifyError(eq(id), any());
    }

    // -------------------------------
    // UNIT TEST: buildFinalPayload
    // -------------------------------
    @Test
    void testBuildFinalPayload() {

        ObjectNode result = worker.buildFinalPayload("XYZ");

        assertEquals("XYZ", result.get("vptoken").asText());
        assertEquals("value", result.get("field").asText());
    }

    // -------------------------------
    // UNIT TEST: getVpToken error
    // -------------------------------
    @Test
    void testGetVpToken_Error() {

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenThrow(new RuntimeException("fail"));

        assertThrows(RuntimeException.class, () -> worker.getVpToken("t", "r"));
    }

    // -------------------------------
    // UNIT TEST: parseVpToken error
    // -------------------------------
    @Test
    void testParseVpToken_Error() {

        when(webClient.post()).thenReturn(bodySpec);
        when(bodySpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenThrow(new RuntimeException("err"));

        assertThrows(RuntimeException.class, () -> worker.parseVpToken("AAA"));
    }
}
