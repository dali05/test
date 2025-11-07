package com.bnpp.pf.common.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecureLoggerTest {

    private SecureLogger secureLogger;
    private Logger mockLogger;

    @BeforeEach
    void setUp() {
        // Création d'une instance et injection du mock logger
        secureLogger = SecureLogger.getLogger(SecureLogger.class);
        mockLogger = Mockito.mock(Logger.class);
        ReflectionTestUtils.setField(secureLogger, "logger", mockLogger);
        ReflectionTestUtils.setField(secureLogger, "maskingEnabled", true);
    }

    @Test
    void testSanitize_ShouldMaskPasswordsAndTokens() {
        String message = "password=secret123 token=abcd1234 apikey=XYZ123";
        String result = secureLogger.sanitize(message);

        assertFalse(result.contains("secret123"));
        assertFalse(result.contains("abcd1234"));
        assertFalse(result.contains("XYZ123"));
        assertTrue(result.contains("[MASKED]"));
    }

    @Test
    void testSanitize_ShouldMaskEmails() {
        String msg = "contact me at test.user@example.com for info";
        String result = secureLogger.sanitize(msg);

        assertEquals("contact me at [EMAIL_MASKED] for info", result);
    }

    @Test
    void testSanitize_ShouldMaskCreditCardNumbers() {
        String msg = "My Visa: 4111111111111111 and MasterCard: 5500000000000004";
        String result = secureLogger.sanitize(msg);

        assertFalse(result.contains("4111111111111111"));
        assertTrue(result.contains("[CREDIT_CARD_MASKED]"));
    }

    @Test
    void testSanitize_ShouldMaskBearerAndJwtTokens() {
        String msg = "Authorization: Bearer abc.def.ghi and jwt=xyz.123.abc";
        String result = secureLogger.sanitize(msg);

        assertTrue(result.contains("[JWT_MASKED]"));
        assertTrue(result.contains("Bearer [TOKEN_MASKED]"));
    }

    @Test
    void testSanitize_ShouldMaskUuid() {
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        String msg = "Here is uuid=" + uuid;
        String result = secureLogger.sanitize(msg);

        assertFalse(result.contains(uuid));
        assertTrue(result.contains("[UUID_MASKED]"));
    }

    @Test
    void testSanitize_ShouldMaskSensitiveUrlQueryParams() {
        String msg = "https://test.com/api?pass=1234&user=john&token=abcd";
        String result = secureLogger.sanitize(msg);

        assertTrue(result.contains("pass=[MASKED]"));
        assertTrue(result.contains("token=[MASKED]"));
        assertTrue(result.contains("user=john"));
    }

    @Test
    void testSanitize_ShouldReturnNullWhenMessageIsNull() {
        assertNull(secureLogger.sanitize(null));
    }

    @Test
    void testSanitize_ShouldReturnOriginalWhenMaskingDisabled() {
        ReflectionTestUtils.setField(secureLogger, "maskingEnabled", false);
        String msg = "password=foo";
        String result = secureLogger.sanitize(msg);

        assertEquals(msg, result);
    }

    @Test
    void testIsSensitiveKey() throws Exception {
        boolean result1 = (boolean) ReflectionTestUtils.invokeMethod(secureLogger, "isSensitiveKey", "password");
        boolean result2 = (boolean) ReflectionTestUtils.invokeMethod(secureLogger, "isSensitiveKey", "user");
        assertTrue(result1);
        assertFalse(result2);
    }

    @Test
    void testDebug_ShouldSanitizeAndLog() {
        when(mockLogger.isDebugEnabled()).thenReturn(true);
        secureLogger.debug("Token is token=abc123");
        verify(mockLogger).debug(contains("[MASKED]"), any());
    }

    @Test
    void testInfo_ShouldSanitizeAndLog() {
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        secureLogger.info("password=1234");
        verify(mockLogger).info(contains("[MASKED]"), any());
    }

    @Test
    void testWarn_ShouldSanitizeAndLog() {
        when(mockLogger.isWarnEnabled()).thenReturn(true);
        secureLogger.warn("apikey=abcd");
        verify(mockLogger).warn(contains("[MASKED]"), any());
    }

    @Test
    void testError_ShouldSanitizeAndLog() {
        when(mockLogger.isErrorEnabled()).thenReturn(true);
        secureLogger.error("Bearer token abc.def.ghi");
        verify(mockLogger).error(contains("[MASKED]"), any());
    }
}