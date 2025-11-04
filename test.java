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
    private WebClient.RequestHeadersUriSpec<?> mockRequestUriSpec;
    private WebClient.RequestHeadersSpec<?> mockRequestHeadersSpec;
    private WebClient.ResponseSpec mockResponseSpec;

    @BeforeEach
    void setUp() throws Exception {
        mockWebClient = mock(WebClient.class);
        mockRequestUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        mockRequestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        mockResponseSpec = mock(WebClient.ResponseSpec.class);

        adminClient = new AdminClient();

        // injection du mock dans le champ private final via réflexion
        var field = AdminClient.class.getDeclaredField("webClient");
        field.setAccessible(true);
        field.set(adminClient, mockWebClient);
    }

    /** ✅ Cas succès : /case/{id} */
    @Test
    void testGetCallbackUrlWithCaseId_Success() {
        UUID id = UUID.randomUUID();

        when(mockWebClient.get()).thenReturn(mockRequestUriSpec);
        when(mockRequestUriSpec.uri(eq("/case/{id}"), eq(id)))
                .thenReturn((WebClient.RequestHeadersSpec<?>) mockRequestHeadersSpec);
        when(mockRequestHeadersSpec.retrieve()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just("http://callback/case"));

        String result = adminClient.getCallbackUrlWithCaseId(id);

        assertEquals("http://callback/case", result);
        verify(mockWebClient).get();
        verify(mockRequestUriSpec).uri("/case/{id}", id);
    }

    /** ✅ Cas succès : /config/{id} */
    @Test
    void testGetCallbackUrlWithConfigId_Success() {
        UUID id = UUID.randomUUID();

        when(mockWebClient.get()).thenReturn(mockRequestUriSpec);
        when(mockRequestUriSpec.uri(eq("/config/{id}"), eq(id)))
                .thenReturn((WebClient.RequestHeadersSpec<?>) mockRequestHeadersSpec);
        when(mockRequestHeadersSpec.retrieve()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just("http://callback/config"));

        String result = adminClient.getCallbackUrlWithConfigId(id);

        assertEquals("http://callback/config", result);
        verify(mockWebClient).get();
        verify(mockRequestUriSpec).uri("/config/{id}", id);
    }

    /** ❌ Cas erreur HTTP */
    @Test
    void testFetchCallbackUrl_WebClientResponseException() {
        UUID id = UUID.randomUUID();

        when(mockWebClient.get()).thenReturn(mockRequestUriSpec);
        when(mockRequestUriSpec.uri(eq("/case/{id}"), eq(id)))
                .thenReturn((WebClient.RequestHeadersSpec<?>) mockRequestHeadersSpec);
        when(mockRequestHeadersSpec.retrieve()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.bodyToMono(String.class))
                .thenThrow(WebClientResponseException.create(404, "Not Found", null, null, null));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminClient.getCallbackUrlWithCaseId(id));

        assertTrue(ex.getMessage().contains("Error fetching callback URL"));
    }

    /** ❌ Cas erreur générique */
    @Test
    void testFetchCallbackUrl_GenericException() {
        UUID id = UUID.randomUUID();

        when(mockWebClient.get()).thenReturn(mockRequestUriSpec);
        when(mockRequestUriSpec.uri(eq("/config/{id}"), eq(id)))
                .thenReturn((WebClient.RequestHeadersSpec<?>) mockRequestHeadersSpec);
        when(mockRequestHeadersSpec.retrieve())
                .thenThrow(new RuntimeException("Connection refused"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminClient.getCallbackUrlWithConfigId(id));

        assertTrue(ex.getMessage().contains("Unexpected error fetching callback URL"));
    }
}
