package com.bnpp.pf.common.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.mockito.Mockito.*;

class DbWhoAmITest {

    private DataSource dataSource;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private DbWhoAmI dbWhoAmI;

    @BeforeEach
    void setup() throws Exception {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(Statement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn("test_user");
        when(resultSet.getString(2)).thenReturn("public");
        when(resultSet.getString(3)).thenReturn("search_path");

        dbWhoAmI = new DbWhoAmI(dataSource);
    }

    @Test
    void testWho_ShouldQueryDatabaseSuccessfully() throws Exception {
        dbWhoAmI.who();

        verify(dataSource).getConnection();
        verify(connection).createStatement();
        verify(statement).executeQuery(contains("select"));
        verify(resultSet).next();
        verify(resultSet).getString(1);
        verify(resultSet).getString(2);
        verify(resultSet).getString(3);
    }

    @Test
    void testWho_ShouldHandleExceptionGracefully() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB error"));

        DbWhoAmI failing = new DbWhoAmI(dataSource);
        try {
            failing.who();
        } catch (Exception e) {
            assert(e.getMessage().contains("DB error"));
        }
    }
}


package com.bnpp.pf.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.MDC;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MdcEnricherFilterTest {

    private MdcEnricherFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setup() {
        filter = new MdcEnricherFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    void testDoFilterInternal_WithHeaders_ShouldEnrichAndClearMdc() throws ServletException, IOException {
        when(request.getHeader("X-User-Id")).thenReturn("user123");
        when(request.getHeader("X-Usecase")).thenReturn("TEST_USECASE");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");

        filter.doFilterInternal(request, response, chain);

        // Vérifie que le filtre a bien été exécuté
        verify(chain).doFilter(request, response);

        // MDC doit être vidé à la fin
        assertNull(MDC.get("userId"));
        assertNull(MDC.get("http.path"));
    }

    @Test
    void testDoFilterInternal_WithoutHeaders_ShouldStillPutHttpInfo() throws ServletException, IOException {
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getHeader("X-Usecase")).thenReturn(null);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/no-headers");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(MDC.get("userId"));
        assertNull(MDC.get("usecase"));
        assertNull(MDC.get("http.path")); // car le clear() est appelé à la fin
    }

    @Test
    void testDoFilterInternal_ShouldClearMdcOnException() throws IOException, ServletException {
        when(request.getHeader("X-User-Id")).thenReturn("fail");
        when(request.getHeader("X-Usecase")).thenReturn("EXCEPTION");
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/throw");

        doThrow(new ServletException("boom")).when(chain).doFilter(any(), any());

        assertThrows(ServletException.class, () -> filter.doFilterInternal(request, response, chain));

        // Après exception, MDC doit toujours être clear
        assertTrue(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty());
    }
}
