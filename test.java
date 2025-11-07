package com.bnpp.pf.common.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecureLoggerTest {

    private Logger mockLogger;
    private SecureLogger secureLogger;

    @BeforeEach
    void setup() throws Exception {
        mockLogger = mock(Logger.class);
        secureLogger = SecureLogger.getLogger(SecureLogger.class);

        // Injection du mock logger dans le SecureLogger
        var field = SecureLogger.class.getDeclaredField("logger");
        field.setAccessible(true);
        field.set(secureLogger, mockLogger);
    }

    @Test
    void testDebugMethods() {
        when(mockLogger.isDebugEnabled()).thenReturn(true);

        secureLogger.debug("Debug message");
        verify(mockLogger).debug(contains("Debug"));

        secureLogger.debug("Debug format {}", "value");
        verify(mockLogger).debug(contains("format"), any());

        secureLogger.debug("Debug array {}", new Object[]{"v1", "v2"});
        verify(mockLogger, atLeastOnce()).debug(anyString(), (Object[]) any());

        Exception e = new RuntimeException("test");
        secureLogger.debug("Debug exception", e);
        verify(mockLogger).debug(contains("Debug exception"), eq(e));
    }

    @Test
    void testInfoMethods() {
        when(mockLogger.isInfoEnabled()).thenReturn(true);

        secureLogger.info("Info message");
        verify(mockLogger).info(contains("Info"));

        secureLogger.info("Format {}", "val");
        verify(mockLogger).info(contains("Format"), any());

        secureLogger.info("Format {}", new Object[]{"a"});
        verify(mockLogger, atLeastOnce()).info(anyString(), (Object[]) any());

        Exception e = new RuntimeException("boom");
        secureLogger.info("Info exception", e);
        verify(mockLogger).info(contains("Info exception"), eq(e));
    }

    @Test
    void testWarnMethods() {
        when(mockLogger.isWarnEnabled()).thenReturn(true);

        secureLogger.warn("Warning");
        verify(mockLogger).warn(contains("Warning"));

        secureLogger.warn("Warn {}", "arg");
        verify(mockLogger).warn(anyString(), any());

        secureLogger.warn("Warn {}", new Object[]{"x"});
        verify(mockLogger, atLeastOnce()).warn(anyString(), (Object[]) any());

        Exception e = new RuntimeException("oops");
        secureLogger.warn("Warn exception", e);
        verify(mockLogger).warn(contains("Warn exception"), eq(e));
    }

    @Test
    void testErrorMethods() {
        when(mockLogger.isErrorEnabled()).thenReturn(true);

        secureLogger.error("Error message");
        verify(mockLogger).error(contains("Error"));

        secureLogger.error("Error {}", "arg");
        verify(mockLogger).error(anyString(), any());

        secureLogger.error("Error {}", new Object[]{"a"});
        verify(mockLogger, atLeastOnce()).error(anyString(), (Object[]) any());

        Exception e = new RuntimeException("failure");
        secureLogger.error("Error exception", e);
        verify(mockLogger).error(contains("Error exception"), eq(e));
    }

    @Test
    void testTraceMethods() {
        when(mockLogger.isTraceEnabled()).thenReturn(true);

        secureLogger.trace("Trace message");
        verify(mockLogger).trace(contains("Trace"));

        secureLogger.trace("Trace {}", "val");
        verify(mockLogger).trace(anyString(), any());

        secureLogger.trace("Trace {}", new Object[]{"x"});
        verify(mockLogger, atLeastOnce()).trace(anyString(), (Object[]) any());

        Exception e = new RuntimeException("trace");
        secureLogger.trace("Trace exception", e);
        verify(mockLogger).trace(contains("Trace exception"), eq(e));
    }

    @Test
    void testIsXEnabledMethods() {
        when(mockLogger.isDebugEnabled()).thenReturn(true);
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(mockLogger.isWarnEnabled()).thenReturn(true);
        when(mockLogger.isErrorEnabled()).thenReturn(true);
        when(mockLogger.isTraceEnabled()).thenReturn(true);

        assertTrue(secureLogger.isDebugEnabled());
        assertTrue(secureLogger.isInfoEnabled());
        assertTrue(secureLogger.isWarnEnabled());
        assertTrue(secureLogger.isErrorEnabled());
        assertTrue(secureLogger.isTraceEnabled());

        verify(mockLogger).isDebugEnabled();
        verify(mockLogger).isInfoEnabled();
        verify(mockLogger).isWarnEnabled();
        verify(mockLogger).isErrorEnabled();
        verify(mockLogger).isTraceEnabled();
    }

    @Test
    void testSanitizeForTest() {
        String raw = "password=1234 token=abcd@example.com";
        String result = secureLogger._sanitizeForTest(raw);
        assertFalse(result.contains("1234"));
        assertTrue(result.contains("[MASKED]") || result.contains("[EMAIL_MASKED]"));
    }
}