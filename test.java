package com.bnpp.pf.walle.access.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> uriSpecMock;

    @Mock
    private WebClient.RequestHeadersSpec<?> headersSpecMock;

    @Mock
    private WebClient.ResponseSpec responseSpecMock;

    private AdminClient adminClient;

    private UUID testId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adminClient = new AdminClient(webClient);
        testId = UUID.randomUUID();
    }

    @Test
    void getCallbackUrlWithCaseId_ShouldReturnValue() {
        String expectedResponse = "callback-url";

        // ✅ Chaîne de mocks corrigée avec casts explicites
        when(webClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) uriSpecMock);
        when(uriSpecMock.uri(anyString(), any(UUID.class)))
                .thenReturn((WebClient.RequestHeadersSpec) headersSpecMock);
        when(headersSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.bodyToMono(String.class)).thenReturn(Mono.just(expectedResponse));

        String result = adminClient.getCallbackUrlWithCaseId(testId);

        assertEquals(expectedResponse, result);
        verify(webClient).get();
        verify(uriSpecMock).uri(anyString(), any(UUID.class));
        verify(headersSpecMock).retrieve();
    }

    @Test
    void getCallbackUrlWithConfigId_ShouldThrow_WhenErrorOccurs() {
        when(webClient.get()).thenThrow(new RuntimeException("Network failure"));

        assertThrows(RuntimeException.class, () -> adminClient.getCallbackUrlWithConfigId(testId));
    }
}
