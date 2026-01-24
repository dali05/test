package com.bnpp.pf.walle.callback.controller;

import com.bnpp.pf.common.api.exception.InvalidRequestBodyException;
import com.bnpp.pf.common.api.exception.JsonConversionException;
import com.bnpp.pf.walle.callback.model.WalletMetadata;
import com.bnpp.pf.walle.callback.service.WalletRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletRequestControllerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WalletRequestService walletRequestService;

    private WalletRequestController controller;

    @BeforeEach
    void setUp() {
        controller = new WalletRequestController(objectMapper, walletRequestService);
    }

    @Test
    void playgroundRequest_shouldSaveResponseCode_andReturnOk() {
        UUID requestId = UUID.randomUUID();
        String responseCode = "00";

        ResponseEntity<Void> response = controller.playgroundRequest(requestId, responseCode);

        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(walletRequestService).saveResponseCode(requestId, responseCode);
        verifyNoMoreInteractions(walletRequestService);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void createRequestObject_shouldReturnToken_whenEverythingIsValid() throws Exception {
        String requestId = "req-123";
        String walletMetadataJson = "{\"some\":\"json\"}";
        String walletNonce = "nonce-abc";

        WalletMetadata parsed = mock(WalletMetadata.class);
        when(objectMapper.readValue(walletMetadataJson, WalletMetadata.class)).thenReturn(parsed);
        when(parsed.getVpFormatsSupported()).thenReturn(List.of("jwt_vp")); // non-null + non-empty
        when(walletRequestService.createSignedRequestObject(parsed, walletNonce, requestId)).thenReturn("jwt-token");

        ResponseEntity<Map<String, Object>> response =
                controller.createRequestObject(requestId, walletMetadataJson, walletNonce);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("jwt-token", response.getBody().get("token"));

        verify(objectMapper).readValue(walletMetadataJson, WalletMetadata.class);
        verify(walletRequestService).createSignedRequestObject(parsed, walletNonce, requestId);
        verifyNoMoreInteractions(walletRequestService);
    }

    @Test
    void createRequestObject_shouldThrowJsonConversionException_whenMetadataIsNotValidJson() throws Exception {
        String requestId = "req-123";
        String invalidJson = "NOT_JSON";
        String walletNonce = "nonce-abc";

        when(objectMapper.readValue(invalidJson, WalletMetadata.class))
                .thenThrow(new RuntimeException("boom"));

        JsonConversionException ex = assertThrows(
                JsonConversionException.class,
                () -> controller.createRequestObject(requestId, invalidJson, walletNonce)
        );

        assertEquals("wallet_metadata must be valid JSON", ex.getMessage());
        verify(objectMapper).readValue(invalidJson, WalletMetadata.class);
        verifyNoInteractions(walletRequestService);
    }

    @Test
    void createRequestObject_shouldReturnOkWithNoBody_whenParsedMetadataIsNull() throws Exception {
        String requestId = "req-123";
        String walletMetadataJson = "{\"some\":\"json\"}";
        String walletNonce = "nonce-abc";

        when(objectMapper.readValue(walletMetadataJson, WalletMetadata.class)).thenReturn(null);

        ResponseEntity<Map<String, Object>> response =
                controller.createRequestObject(requestId, walletMetadataJson, walletNonce);

        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getBody());

        verify(objectMapper).readValue(walletMetadataJson, WalletMetadata.class);
        verifyNoInteractions(walletRequestService);
    }

    @Test
    void createRequestObject_shouldThrowInvalidRequestBodyException_whenVpFormatsSupportedIsNull() throws Exception {
        String requestId = "req-123";
        String walletMetadataJson = "{\"some\":\"json\"}";
        String walletNonce = "nonce-abc";

        WalletMetadata parsed = mock(WalletMetadata.class);
        when(objectMapper.readValue(walletMetadataJson, WalletMetadata.class)).thenReturn(parsed);
        when(parsed.getVpFormatsSupported()).thenReturn(null); // couvre le côté "== null"

        InvalidRequestBodyException ex = assertThrows(
                InvalidRequestBodyException.class,
                () -> controller.createRequestObject(requestId, walletMetadataJson, walletNonce)
        );

        assertEquals("vp_formats_supported must not be null or empty", ex.getMessage());
        verify(objectMapper).readValue(walletMetadataJson, WalletMetadata.class);
        verifyNoInteractions(walletRequestService);
    }

    @Test
    void createRequestObject_shouldThrowInvalidRequestBodyException_whenVpFormatsSupportedIsEmpty() throws Exception {
        String requestId = "req-123";
        String walletMetadataJson = "{\"some\":\"json\"}";
        String walletNonce = "nonce-abc";

        WalletMetadata parsed = mock(WalletMetadata.class);
        when(objectMapper.readValue(walletMetadataJson, WalletMetadata.class)).thenReturn(parsed);
        when(parsed.getVpFormatsSupported()).thenReturn(List.of()); // couvre le côté ".isEmpty()"

        InvalidRequestBodyException ex = assertThrows(
                InvalidRequestBodyException.class,
                () -> controller.createRequestObject(requestId, walletMetadataJson, walletNonce)
        );

        assertEquals("vp_formats_supported must not be null or empty", ex.getMessage());
        verify(objectMapper).readValue(walletMetadataJson, WalletMetadata.class);
        verifyNoInteractions(walletRequestService);
    }

    @Test
    void createRequestObject_shouldRethrowIllegalArgumentException_whenServiceThrowsIt() throws Exception {
        String requestId = "req-123";
        String walletMetadataJson = "{\"some\":\"json\"}";
        String walletNonce = "nonce-abc";

        WalletMetadata parsed = mock(WalletMetadata.class);
        when(objectMapper.readValue(walletMetadataJson, WalletMetadata.class)).thenReturn(parsed);
        when(parsed.getVpFormatsSupported()).thenReturn(List.of("jwt_vp"));

        when(walletRequestService.createSignedRequestObject(parsed, walletNonce, requestId))
                .thenThrow(new IllegalArgumentException("bad nonce"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> controller.createRequestObject(requestId, walletMetadataJson, walletNonce)
        );

        assertEquals("bad nonce", ex.getMessage());

        verify(objectMapper).readValue(walletMetadataJson, WalletMetadata.class);
        verify(walletRequestService).createSignedRequestObject(parsed, walletNonce, requestId);
    }
}