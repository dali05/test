package com.bnpp.pf.common.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaseNotFoundExceptionTest {

    @Test
    void testConstructor_SetsCorrectValues() {
        String resourceName = "test-case";

        CaseNotFoundException exception = new CaseNotFoundException(resourceName);

        assertNotNull(exception);
        assertTrue(exception instanceof ApiException);
        assertEquals(resourceName, exception.getResourceName());
        assertEquals(404, exception.getStatusCode());
        assertEquals("RES-404", exception.getErrorCode());
        assertEquals(ErrorType.RESOURCE_NOT_FOUND, exception.getErrorType());
    }

    @Test
    void testConstructor_WithNullResourceName() {
        CaseNotFoundException exception = new CaseNotFoundException(null);
        assertNull(exception.getResourceName());
    }
}

package com.bnpp.pf.common.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaseNotFoundExceptionTest {

    @Test
    void testConstructor_SetsCorrectValues() {
        String resourceName = "test-case";

        CaseNotFoundException exception = new CaseNotFoundException(resourceName);

        assertNotNull(exception);
        assertTrue(exception instanceof ApiException);
        assertEquals(resourceName, exception.getResourceName());
        assertEquals(404, exception.getStatusCode());
        assertEquals("RES-404", exception.getErrorCode());
        assertEquals(ErrorType.RESOURCE_NOT_FOUND, exception.getErrorType());
    }

    @Test
    void testConstructor_WithNullResourceName() {
        CaseNotFoundException exception = new CaseNotFoundException(null);
        assertNull(exception.getResourceName());
    }
}
