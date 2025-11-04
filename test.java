package com.bnpp.pf.walle.access.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AdminClient avec 100% de couverture
 * 
 * Couverture complète :
 * - Tous les cas de succès
 * - Toutes les exceptions (WebClientResponseException et génériques)
 * - Tous les blocs catch
 * - Toutes les branches conditionnelles
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminClient - Tests unitaires complets")
class AdminClientTest {

    private AdminClient adminClient;
    
    @Mock
    private WebClient mockWebClient;
    
    @Mock
    private WebClient.RequestHeadersUriSpec<?> mockRequestUri;
    
    @Mock
    private WebClient.RequestHeadersSpec<?> mockRequestHeaders;
    
    @Mock
    private WebClient.ResponseSpec mockResponse;

    @BeforeEach
    void setUp() throws Exception {
        adminClient = new AdminClient();

        // Injection du WebClient mocké via reflection
        Field field = AdminClient.class.getDeclaredField("webClient");
        field.setAccessible(true);
        field.set(adminClient, mockWebClient);
    }

    // ========================================
    // Tests pour getCallbackUrlWithCaseId()
    // ========================================

    @Test
    @DisplayName("✅ getCallbackUrlWithCaseId - Succès nominal")
    void testGetCallbackUrlWithCaseId_Success() {
        // Given
        UUID id = UUID.randomUUID();
        String expectedUrl = "http://callback/case/12345";

        when(mockWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) mockRequestUri);
        when(mockRequestUri.uri(anyString(), any(UUID.class))).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just(expectedUrl));

        // When
        String result = adminClient.getCallbackUrlWithCaseId(id);

        // Then
        assertNotNull(result);
        assertEquals(expectedUrl, result);
        verify(mockWebClient).get();
        verify(mockRequestUri).uri("/case/{id}", id);
        verify(mockRequestHeaders).retrieve();
        verify(mockResponse).bodyToMono(String.class);
    }

    @Test
    @DisplayName("❌ getCallbackUrlWithCaseId - WebClientResponseException 404")
    void testGetCallbackUrlWithCaseId_WebClientResponseException_404() {
        // Given
        UUID id = UUID.randomUUID();
        WebClientResponseException exception = WebClientResponseException.create(
            404, 
            "Not Found", 
            null, 
            null, 
            null
        );

        when(mockWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) mockRequestUri);
        when(mockRequestUri.uri(anyString(), any(UUID.class))).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.error(exception));

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, 
            () -> adminClient.getCallbackUrlWithCaseId(id));
        
        assertTrue(thrown.getMessage().contains("Error fetching callback URL for case"));
        assertTrue(thrown.getMessage().contains("404"));
        assertEquals(exception, thrown.getCause());
    }

    @Test
    @DisplayName("❌ getCallbackUrlWithCaseId - WebClientResponseException 500")
    void testGetCallbackUrlWithCaseId_WebClientResponseException_500() {
        // Given
        UUID id = UUID.randomUUID();
        WebClientResponseException exception = WebClientResponseException.create(
            500, 
            "Internal Server Error", 
            null, 
            null, 
            null
        );

        when(mockWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) mockRequestUri);
        when(mockRequestUri.uri(anyString(), any(UUID.class))).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.error(exception));

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, 
            () -> adminClient.getCallbackUrlWithCaseId(id));
        
        assertTrue(thrown.getMessage().contains("Error fetching callback URL for case"));
        assertTrue(thrown.getMessage().contains("500"));
    }

    @Test
    @DisplayName("❌ getCallbackUrlWithCaseId - Exception générique")
    void testGetCallbackUrlWithCaseId_GenericException() {
        // Given
        UUID id = UUID.randomUUID();
        RuntimeException genericException = new RuntimeException("Database connection failed");

        when(mockWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) mockRequestUri);
        when(mockRequestUri.uri(anyString(), any(UUID.class))).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenThrow(genericException);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, 
            () -> adminClient.getCallbackUrlWithCaseId(id));
        
        assertTrue(thrown.getMessage().contains("Unexpected error fetching callback URL for case"));
        assertEquals(genericException, thrown.getCause());
    }

    @Test
    @DisplayName("❌ getCallbackUrlWithCaseId - NullPointerException")
    void testGetCallbackUrlWithCaseId_NullPointerException() {
        // Given
        UUID id = UUID.randomUUID();
        NullPointerException npe = new NullPointerException("WebClient is null");

        when(mockWebClient.get()).thenThrow(npe);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, 
            () -> adminClient.getCallbackUrlWithCaseId(id));
        
        assertTrue(thrown.getMessage().contains("Unexpected error fetching callback URL for case"));
    }

    // ========================================
    // Tests pour getCallbackUrlWithConfigId()
    // ========================================

    @Test
    @DisplayName("✅ getCallbackUrlWithConfigId - Succès nominal")
    void testGetCallbackUrlWithConfigId_Success() {
        // Given
        UUID id = UUID.randomUUID();
        String expectedUrl = "http://callback/config/67890";

        when(mockWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) mockRequestUri);
        when(mockRequestUri.uri(anyString(), any(UUID.class))).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just(expectedUrl));

        // When
        String result = adminClient.getCallbackUrlWithConfigId(id);

        // Then
        assertNotNull(result);
        assertEquals(expectedUrl, result);
        verify(mockWebClient).get();
        verify(mockRequestUri).uri("/config/{id}", id);
        verify(mockRequestHeaders).retrieve();
        verify(mockResponse).bodyToMono(String.class);
    }

    @Test
    @DisplayName("❌ getCallbackUrlWithConfigId - WebClientResponseException 401")
    void testGetCallbackUrlWithConfigId_WebClientResponseException_401() {
        // Given
        UUID id = UUID.randomUUID();
        WebClientResponseException exception = WebClientResponseException.create(
            401, 
            "Unauthorized", 
            null, 
            null, 
            null
        );

        when(mockWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) mockRequestUri);
        when(mockRequestUri.uri(anyString(), any(UUID.class))).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.error(exception));

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, 
            () -> adminClient.getCallbackUrlWithConfigId(id));
        
        assertTrue(thrown.getMessage().contains("Error fetching callback URL for config"));
        assertTrue(thrown.getMessage().contains("401"));
    }

    @Test
    @DisplayName("❌ getCallbackUrlWithConfigId - WebClientResponseException 503")
    void testGetCallbackUrlWithConfigId_WebClientResponseException_503() {
        // Given
        UUID id = UUID.randomUUID();
        WebClientResponseException exception = WebClientResponseException.create(
            503, 
            "Service Unavailable", 
            null, 
            null, 
            null
        );

        when(mockWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) mockRequestUri);
        when(mockRequestUri.uri(anyString(), any(UUID.class))).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.error(exception));

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, 
            () -> adminClient.getCallbackUrlWithConfigId(id));
        
        assertTrue(thrown.getMessage().contains("Error fetching callback URL for config"));
        assertTrue(thrown.getMessage().contains("503"));
    }

    @Test
    @DisplayName("❌ getCallbackUrlWithConfigId - Exception générique dans retrieve()")
    void testGetCallbackUrlWithConfigId_GenericException_InRetrieve() {
        // Given
        UUID id = UUID.randomUUID();
        RuntimeException genericException = new RuntimeException("Network timeout");

        when(mockWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) mockRequestUri);
        when(mockRequestUri.uri(anyString(), any(UUID.class))).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenThrow(genericException);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, 
            () -> adminClient.getCallbackUrlWithConfigId(id));
        
        assertTrue(thrown.getMessage().contains("Unexpected error fetching callback URL for config"));
        assertEquals(genericException, thrown.getCause());
    }

    @Test
    @DisplayName("❌ getCallbackUrlWithConfigId - Exception générique dans bodyToMono()")
    void testGetCallbackUrlWithConfigId_GenericException_InBodyToMono() {
        // Given
        UUID id = UUID.randomUUID();
        IllegalStateException illegalStateException = new IllegalStateException("Invalid state");

        when(mockWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) mockRequestUri);
        when(mockRequestUri.uri(anyString(), any(UUID.class))).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class)).thenThrow(illegalStateException);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, 
            () -> adminClient.getCallbackUrlWithConfigId(id));
        
        assertTrue(thrown.getMessage().contains("Unexpected error fetching callback URL for config"));
    }

    // ========================================
    // Tests de cas limites
    // ========================================

    @Test
    @DisplayName("✅ getCallbackUrlWithCaseId - URL vide retournée")
    void testGetCallbackUrlWithCaseId_EmptyUrlReturned() {
        // Given
        UUID id = UUID.randomUUID();
        String emptyUrl = "";

        when(mockWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) mockRequestUri);
        when(mockRequestUri.uri(anyString(), any(UUID.class))).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just(emptyUrl));

        // When
        String result = adminClient.getCallbackUrlWithCaseId(id);

        // Then
        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    @DisplayName("✅ getCallbackUrlWithConfigId - URL avec caractères spéciaux")
    void testGetCallbackUrlWithConfigId_SpecialCharactersInUrl() {
        // Given
        UUID id = UUID.randomUUID();
        String urlWithSpecialChars = "http://callback/config?param=value&special=é@#";

        when(mockWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) mockRequestUri);
        when(mockRequestUri.uri(anyString(), any(UUID.class))).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just(urlWithSpecialChars));

        // When
        String result = adminClient.getCallbackUrlWithConfigId(id);

        // Then
        assertEquals(urlWithSpecialChars, result);
    }

    @Test
    @DisplayName("✅ Vérification que les deux méthodes utilisent des URIs différentes")
    void testDifferentUrisForDifferentMethods() {
        // Given
        UUID id = UUID.randomUUID();

        when(mockWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) mockRequestUri);
        when(mockRequestUri.uri(anyString(), any(UUID.class))).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class))
            .thenReturn(Mono.just("url1"))
            .thenReturn(Mono.just("url2"));

        // When
        adminClient.getCallbackUrlWithCaseId(id);
        adminClient.getCallbackUrlWithConfigId(id);

        // Then
        verify(mockRequestUri).uri("/case/{id}", id);
        verify(mockRequestUri).uri("/config/{id}", id);
    }

    @Test
    @DisplayName("✅ Test avec UUID null ne lève pas d'exception dans le mock")
    void testWithNullUUID() {
        // Given
        UUID id = null;

        when(mockWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) mockRequestUri);
        when(mockRequestUri.uri(anyString(), any())).thenReturn(mockRequestHeaders);
        when(mockRequestHeaders.retrieve()).thenReturn(mockResponse);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("url"));

        // When
        String result = adminClient.getCallbackUrlWithCaseId(id);

        // Then
        assertNotNull(result);
        verify(mockRequestUri).uri("/case/{id}", id);
    }

    @Test
    @DisplayName("✅ Test du constructeur par défaut")
    void testDefaultConstructor() throws Exception {
        // Given
        AdminClient freshClient = new AdminClient();
        
        // When
        Field field = AdminClient.class.getDeclaredField("webClient");
        field.setAccessible(true);
        WebClient webClient = (WebClient) field.get(freshClient);
        
        // Then
        assertNotNull(webClient);
    }
}
