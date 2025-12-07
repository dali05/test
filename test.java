package com.bnpp.pf.walle.access.process.adapter.in.workers;

import com.bnpp.pf.walle.access.domain.entity.AccessRequest;
import com.bnpp.pf.walle.access.process.app.port.in.NotifySyncCompletionUseCase;
import com.bnpp.pf.walle.access.process.app.port.out.AccessRequestPersistencePort;
import com.bnpp.pf.walle.access.process.config.idacto.IdactoProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.camunda.zeebe.client.api.command.FinalCommandStep;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestWalletDataWorkerTest {

    WebClient webClient;
    WebClient.RequestHeadersUriSpec uriSpec;
    WebClient.RequestHeadersSpec headersSpec;
    WebClient.RequestBodySpec bodySpec;
    WebClient.ResponseSpec responseSpec;

    IdactoProperties props;
    AccessRequestPersistencePort persistence;
    NotifySyncCompletionUseCase notifyUseCase;

    ActivatedJob job;
    JobClient jobClient;
    FinalCommandStep completeStep;
    FinalCommandStep failStep;

    RequestWalletDataWorker worker;
    ObjectNode fakeTemplate;

    @BeforeEach
    void setup() throws Exception {
        webClient = mock(WebClient.class);
        uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        headersSpec = mock(WebClient.RequestHeadersSpec.class);
        bodySpec = mock(WebClient.RequestBodySpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        props = mock(IdactoProperties.class);
        persistence = mock(AccessRequestPersistencePort.class);
        notifyUseCase = mock(NotifySyncCompletionUseCase.class);

        job = mock(ActivatedJob.class);
        jobClient = mock(JobClient.class);
        completeStep = mock(FinalCommandStep.class);
        failStep = mock(FinalCommandStep.class);

        fakeTemplate = new ObjectMapper().createObjectNode();
        fakeTemplate.put("field", "value");

        worker = Mockito.spy(new RequestWalletDataWorker(
                webClient,
                props,
                persistence,
                notifyUseCase,
                new ObjectMapper()
        ));

        // Mock loadTemplateJson()
        doReturn(fakeTemplate).when(worker).loadTemplateJson();

        when(props.authorizationPath()).thenReturn("/auth");
        when(props.parseTokenPath()).thenReturn("/parse");
    }

    // -----------------------------
    // SUCCESS CASE
    // -----------------------------
    @Test
    void testSuccess() throws Exception {

        UUID id = UUID.randomUUID();
        when(job.getVariablesAsMap()).thenReturn(Map.of("requestId", id.toString()));

        AccessRequest ar = mock(AccessRequest.class);
        when(ar.getResponseCode()).thenReturn("OK");
        when(persistence.findById(id)).thenReturn(java.util.Optional.of(ar));

        // Mock GET
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(reactor.core.publisher.Mono.just("TOKEN123"));

        // Mock POST
        when(webClient.post()).thenReturn(bodySpec);
        when(bodySpec.uri("/parse")).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(reactor.core.publisher.Mono.just(Map.of("k", "v")));

        // ZE EBE
        when(jobClient.newCompleteCommand(job.getKey())).thenReturn(completeStep);
        when(completeStep.variables(any())).thenReturn(completeStep);
        when(completeStep.send()).thenReturn(CompletableFuture.completedFuture(null));

        worker.handleRequestWalletData(job, jobClient);

        verify(completeStep).variables(Map.of("k", "v"));
        verify(jobClient).newCompleteCommand(job.getKey());
    }

    // -----------------------------
    // AccessRequest NOT FOUND
    // -----------------------------
    @Test
    void testNotFound() {

        UUID id = UUID.randomUUID();

        when(job.getVariablesAsMap()).thenReturn(Map.of("requestId", id.toString()));
        when(persistence.findById(id)).thenReturn(java.util.Optional.empty());

        when(jobClient.newFailCommand(job.getKey())).thenReturn(failStep);
        when(failStep.retries(0)).thenReturn(failStep);
        when(failStep.errorMessage(any())).thenReturn(failStep);
        when(failStep.send()).thenReturn(CompletableFuture.completedFuture(null));

        worker.handleRequestWalletData(job, jobClient);

        verify(notifyUseCase).notifyError(eq(id), any());
    }

    // -----------------------------
    // WebClient GET ERROR
    // -----------------------------
    @Test
    void testGetVpTokenError() {

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenThrow(new RuntimeException("FAIL"));

        assertThrows(RuntimeException.class, () -> worker.getVpToken("a", "b"));
    }

    // -----------------------------
    // WebClient POST ERROR
    // -----------------------------
    @Test
    void testParseVpTokenError() throws Exception {

        when(webClient.post()).thenReturn(bodySpec);
        when(bodySpec.uri("/parse")).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenThrow(new RuntimeException("POST_FAIL"));

        assertThrows(RuntimeException.class, () -> worker.parseVpToken("XYZ"));
    }

    // -----------------------------
    // buildFinalPayload
    // -----------------------------
    @Test
    void testBuildFinalPayload() {
        ObjectNode out = worker.buildFinalPayload("AAA");

        assertEquals("AAA", out.get("vptoken").asText());
        assertEquals("value", out.get("field").asText());
    }
}