package com.bnpp.pf.walle.access.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminClient {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:8082/api/v1/notif")
            .build();

    public String getCallbackUrlWithCaseId(UUID id) {
        return fetchCallbackUrl("/case/{id}", id);
    }

    public String getCallbackUrlWithConfigId(UUID id) {
        return fetchCallbackUrl("/config/{id}", id);
    }

    private String fetchCallbackUrl(String path, UUID id) {
        try {
            return webClient.get()
                    .uri(path, id)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Failed to fetch callback URL ({}): {}", id, e.getResponseBodyAsString());
            throw new RuntimeException("Error fetching callback URL: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching callback URL for ID {}", id, e);
            throw new RuntimeException("Unexpected error fetching callback URL", e);
        }
    }
}


package com.bnpp.pf.walle.access.service;

import com.bnpp.pf.walle.access.repository.AccessRequestRepository;
import com.bnpp.pf.walle.access.web.dto.NotificationRequestDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiNotificationServiceImpl implements NotificationService {

    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;
    private final AccessRequestRepository requestRepository;
    private final AdminClient adminClient;

    @Override
    public void sendNotification(NotificationRequestDto notif) {
        findCaseIdAndConfigIdById(notif.getRequestId())
                .map(this::extractApigeeUrl)
                .ifPresentOrElse(
                        apigeeUrl -> callApigee(notif, apigeeUrl),
                        () -> log.warn("No case/config found for request ID: {}", notif.getRequestId())
                );
    }

    private String extractApigeeUrl(CaseConfigId caseConfig) {
        try {
            String adminResponse = adminClient.getCallbackUrlWithCaseId(caseConfig.caseId());
            JsonNode node = new ObjectMapper().readTree(adminResponse);
            return node.path("data").asText(null);
        } catch (Exception e) {
            log.error("Failed to extract Apigee URL for caseId: {}", caseConfig.caseId(), e);
            throw new RuntimeException("Unable to extract Apigee URL", e);
        }
    }

    private void callApigee(NotificationRequestDto notif, String apigeeUrl) {
        if (apigeeUrl == null || apigeeUrl.isBlank()) {
            log.warn("Apigee URL is missing, skipping notification for request ID: {}", notif.getRequestId());
            return;
        }

        try {
            String jwt = jwtUtil.generateJwt();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(jwt);

            HttpEntity<NotificationRequestDto> entity = new HttpEntity<>(notif, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apigeeUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Notification sent successfully to {}", apigeeUrl);
            } else {
                log.warn("Notification failed: {} - {}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("Error sending notification to Apigee at {} for requestId {}", apigeeUrl, notif.getRequestId(), e);
        }
    }

    @Override
    public Optional<CaseConfigId> findCaseIdAndConfigIdById(UUID requestId) {
        return requestRepository.findCaseIdAndConfigIdById(requestId);
    }

    public record CaseConfigId(UUID caseId, UUID configId) {}
}

