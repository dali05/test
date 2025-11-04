package com.bnpp.pf.walle.access.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.reactive.function.client.*;
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

    @InjectMocks
    private AdminClient adminClient = new AdminClient();

    private UUID testId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testId = UUID.randomUUID();
    }

    @Test
    void getCallbackUrlWithCaseId_ShouldReturnValue() {
        String expectedResponse = "callback-url";

        // Mock WebClient behavior chain
        when(webClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) uriSpecMock);
        when(uriSpecMock.uri(anyString(), any(UUID.class))).thenReturn((WebClient.RequestHeadersSpec) headersSpecMock);
        when(headersSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.bodyToMono(String.class)).thenReturn(Mono.just(expectedResponse));

        // Create AdminClient using the mocked WebClient
        AdminClient tested = new AdminClient(webClient);

        String result = tested.getCallbackUrlWithCaseId(testId);
        assertEquals(expectedResponse, result);

        verify(webClient, times(1)).get();
        verify(uriSpecMock, times(1)).uri(anyString(), any(UUID.class));
        verify(headersSpecMock, times(1)).retrieve();
    }

    @Test
    void getCallbackUrlWithConfigId_ShouldThrow_WhenErrorOccurs() {
        when(webClient.get()).thenThrow(new RuntimeException("Network failure"));
        AdminClient tested = new AdminClient(webClient);
        assertThrows(RuntimeException.class, () -> tested.getCallbackUrlWithConfigId(testId));
    }
}
