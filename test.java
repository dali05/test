package com.bnpp.pf.walle.access.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminClientTest {

    private AdminClient adminClient;
    private WebClient mockWebClient;
    private WebClient.RequestHeadersUriSpec<?> mockRequestUri;
    private WebClient.RequestHeadersSpec<?> mockRequestHeaders;
    private WebClient.ResponseSpec mockResponse;

    @BeforeEach
    void setUp() throws Exception {
        mockWebClient = mock(WebClient.class);
        mockRequestUri = mock(WebClient.RequestHeadersUriSpec.class);
        mockRequestHeaders = mock(WebClient.RequestHeadersSpec.class);
        mockResponse = mock(WebClient.ResponseSpec.class);

        adminClient = new AdminClient();

        // Injection du WebClient mocké
        var field = AdminClient.class.getDeclaredField("webClient");
        field.setAccessible(true);
        field.set(adminClient, mockWebClient);
    }

    /** ✅ Cas succès : getCallbackUrlWithCaseId() */
    @Test
    void testGetCallbackUrlWithCaseId_Success() {
        UUID id = UUID.randomUUID();

        when(mockWebClient.get()).thenReturn(mockRequestUri);
        when(mockRequestUri.uri("/case/{id}", id)).thenReturn((WebClient.RequestHeadersSpec<?>) mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("http://callback/case"));

        String result = adminClient.getCallbackUrlWithCaseId(id);

        assertEquals("http://callback/case", result);
        verify(mockWebClient).get();
        verify(mockRequestUri).uri("/case/{id}", id);
    }

    /** ✅ Cas succès : getCallbackUrlWithConfigId() */
    @Test
    void testGetCallbackUrlWithConfigId_Success() {
        UUID id = UUID.randomUUID();

        when(mockWebClient.get()).thenReturn(mockRequestUri);
        when(mockRequestUri.uri("/config/{id}", id)).thenReturn((WebClient.RequestHeadersSpec<?>) mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("http://callback/config"));

        String result = adminClient.getCallbackUrlWithConfigId(id);

        assertEquals("http://callback/config", result);
        verify(mockWebClient).get();
        verify(mockRequestUri).uri("/config/{id}", id);
    }

    /** ❌ Cas erreur : WebClientResponseException */
    @Test
    void testFetchCallbackUrl_WebClientResponseException() {
        UUID id = UUID.randomUUID();

        when(mockWebClient.get()).thenReturn(mockRequestUri);
        when(mockRequestUri.uri("/case/{id}", id)).thenReturn((WebClient.RequestHeadersSpec<?>) mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class))
                .thenThrow(WebClientResponseException.create(404, "Not Found", null, null, null));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminClient.getCallbackUrlWithCaseId(id));
        assertTrue(ex.getMessage().contains("Error fetching callback URL"));
    }

    /** ❌ Cas erreur générique */
    @Test
    void testFetchCallbackUrl_GenericException() {
        UUID id = UUID.randomUUID();

        when(mockWebClient.get()).thenReturn(mockRequestUri);
        when(mockRequestUri.uri("/config/{id}", id)).thenReturn((WebClient.RequestHeadersSpec<?>) mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenThrow(new RuntimeException("Unexpected error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminClient.getCallbackUrlWithConfigId(id));
        assertTrue(ex.getMessage().contains("Unexpected error fetching callback URL"));
    }
}
