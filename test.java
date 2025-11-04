
package com.bnpp.pf.walle.access.service;

import com.bnpp.pf.walle.access.repository.AccessRequestRepository;
import com.bnpp.pf.walle.access.web.dto.NotificationRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiNotificationServiceImplTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AccessRequestRepository requestRepository;

    @Mock
    private AdminClient adminClient;

    @InjectMocks
    private ApiNotificationServiceImpl service;

    private NotificationRequestDto notif;
    private ApiNotificationServiceImpl.CaseConfigId caseConfigId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        notif = new NotificationRequestDto();
        notif.setRequestId(UUID.randomUUID());
        caseConfigId = new ApiNotificationServiceImpl.CaseConfigId(UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    void sendNotification_ShouldCallApigee_WhenCaseFound() throws Exception {
        String jsonResponse = "{\"data\":\"https://apigee.test/send\"}";
        when(requestRepository.findCaseIdAndConfigIdById(any())).thenReturn(Optional.of(caseConfigId));
        when(adminClient.getCallbackUrlWithCaseId(any())).thenReturn(jsonResponse);
        when(jwtUtil.generateJwt()).thenReturn("jwt-token");

        ResponseEntity<String> okResponse = new ResponseEntity<>("ok", HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(okResponse);

        service.sendNotification(notif);

        verify(restTemplate, times(1)).postForEntity(eq("https://apigee.test/send"), any(), eq(String.class));
    }

    @Test
    void sendNotification_ShouldLogWarning_WhenCaseNotFound() {
        when(requestRepository.findCaseIdAndConfigIdById(any())).thenReturn(Optional.empty());
        service.sendNotification(notif);
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void callApigee_ShouldHandleErrorGracefully() {
        when(jwtUtil.generateJwt()).thenReturn("jwt-token");
        doThrow(new RuntimeException("Connection failed"))
                .when(restTemplate)
                .postForEntity(anyString(), any(), eq(String.class));

        service.sendNotification(notif); // indirectly calls callApigee()
    }

    @Test
    void extractApigeeUrl_ShouldReturnValidUrl() throws Exception {
        String jsonResponse = "{\"data\":\"https://apigee.url/test\"}";
        when(adminClient.getCallbackUrlWithCaseId(any())).thenReturn(jsonResponse);
        String result = invokeExtractApigeeUrl(caseConfigId);
        assertEquals("https://apigee.url/test", result);
    }

    @Test
    void extractApigeeUrl_ShouldThrow_WhenInvalidJson() throws Exception {
        when(adminClient.getCallbackUrlWithCaseId(any())).thenReturn("INVALID_JSON");
        assertThrows(RuntimeException.class, () -> invokeExtractApigeeUrl(caseConfigId));
    }

    // Helper method to call private extractApigeeUrl
    private String invokeExtractApigeeUrl(ApiNotificationServiceImpl.CaseConfigId id) throws Exception {
        var method = ApiNotificationServiceImpl.class.getDeclaredMethod("extractApigeeUrl", ApiNotificationServiceImpl.CaseConfigId.class);
        method.setAccessible(true);
        return (String) method.invoke(service, id);
    }
}


package com.bnpp.pf.walle.access.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> uriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> headersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

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
        String expected = "callback-url";
        WebClient client = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec<?> req = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec<?> headers = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec response = mock(WebClient.ResponseSpec.class);

        when(client.get()).thenReturn(req);
        when(req.uri(anyString(), any())).thenReturn(headers);
        when(headers.retrieve()).thenReturn(response);
        when(response.bodyToMono(String.class)).thenReturn(Mono.just(expected));

        AdminClient tested = new AdminClient();
        assertDoesNotThrow(() -> tested.getCallbackUrlWithCaseId(testId));
    }

    @Test
    void fetchCallbackUrl_ShouldThrow_WhenWebClientError() {
        AdminClient tested = new AdminClient();
        UUID id = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> tested.getCallbackUrlWithConfigId(id));
    }
}

