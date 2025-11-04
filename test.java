package com.bnpp.pf.walle.access.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminClientTest {

    private AdminClient adminClient;
    private WebClient mockWebClient;
    private WebClient.RequestHeadersUriSpec<?> mockRequest;
    private WebClient.RequestHeadersSpec<?> mockRequestHeaders;
    private WebClient.ResponseSpec mockResponse;

    @BeforeEach
    void setUp() {
        mockWebClient = mock(WebClient.class);
        mockRequest = mock(WebClient.RequestHeadersUriSpec.class);
        mockRequestHeaders = mock(WebClient.RequestHeadersSpec.class);
        mockResponse = mock(WebClient.ResponseSpec.class);

        // On crée un adminClient, mais on injecte notre WebClient mocké via réflexion
        adminClient = new AdminClient();
        // remplacer le WebClient final via réflexion
        try {
            var field = AdminClient.class.getDeclaredField("webClient");
            field.setAccessible(true);
            field.set(adminClient, mockWebClient);
        } catch (Exception e) {
            fail("Erreur d’injection du mock WebClient");
        }
    }

    /** ✅ Cas succès : getCallbackUrlWithCaseId() */
    @Test
    void testGetCallbackUrlWithCaseId_Success() {
        UUID id = UUID.randomUUID();
        when(mockWebClient.get()).thenReturn(mockRequest);
        when(mockRequest.uri("/case/{id}", id)).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("http://callback/case"));

        String result = adminClient.getCallbackUrlWithCaseId(id);

        assertEquals("http://callback/case", result);
        verify(mockWebClient).get();
        verify(mockRequest).uri("/case/{id}", id);
    }

    /** ✅ Cas succès : getCallbackUrlWithConfigId() */
    @Test
    void testGetCallbackUrlWithConfigId_Success() {
        UUID id = UUID.randomUUID();
        when(mockWebClient.get()).thenReturn(mockRequest);
        when(mockRequest.uri("/config/{id}", id)).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("http://callback/config"));

        String result = adminClient.getCallbackUrlWithConfigId(id);

        assertEquals("http://callback/config", result);
        verify(mockWebClient).get();
        verify(mockRequest).uri("/config/{id}", id);
    }

    /** ❌ Cas erreur HTTP : WebClientResponseException */
    @Test
    void testFetchCallbackUrl_WebClientResponseException() {
        UUID id = UUID.randomUUID();
        when(mockWebClient.get()).thenReturn(mockRequest);
        when(mockRequest.uri("/case/{id}", id)).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class))
                .thenThrow(WebClientResponseException.create(404, "Not Found", null, null, null));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminClient.getCallbackUrlWithCaseId(id));

        assertTrue(ex.getMessage().contains("Error fetching callback URL"));
    }

    /** ❌ Cas erreur générique : Exception */
    @Test
    void testFetchCallbackUrl_GenericException() {
        UUID id = UUID.randomUUID();
        when(mockWebClient.get()).thenReturn(mockRequest);
        when(mockRequest.uri("/config/{id}", id)).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenThrow(new RuntimeException("Unexpected"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminClient.getCallbackUrlWithConfigId(id));

        assertTrue(ex.getMessage().contains("Unexpected error fetching callback URL"));
    }
}
