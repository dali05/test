package com.bnpp.pf.walle.access.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET = "MY_SUPER_SECRET_KEY"; // ⚠️ à sécuriser (Vault ou env var)

    /**
     * Génère un JWT avec l’algorithme passé en paramètre.
     */
    public String generateJwt(String algorithmName) {
        SignatureAlgorithm algorithm = resolveAlgorithm(algorithmName);

        return Jwts.builder()
                .setSubject("walle-access-service")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000)) // 15 minutes
                .signWith(algorithm, SECRET.getBytes())
                .compact();
    }

    private SignatureAlgorithm resolveAlgorithm(String name) {
        try {
            if (name == null || name.isBlank()) {
                name = "HS256"; // valeur par défaut
            }
            return SignatureAlgorithm.forName(name.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported JWT algorithm: " + name, e);
        }
    }
}

package com.bnpp.pf.walle.access.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtRequestInterceptor implements ClientHttpRequestInterceptor {

    private final JwtUtil jwtUtil;

    /**
     * Algorithme dynamique (modifiable à la volée depuis un service)
     */
    private static final ThreadLocal<String> currentAlgorithm = new ThreadLocal<>();

    public static void setAlgorithm(String algorithm) {
        currentAlgorithm.set(algorithm);
    }

    public static void clearAlgorithm() {
        currentAlgorithm.remove();
    }

    @Override
    public ClientHttpResponse intercept(org.springframework.http.HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {

        // Récupère l’algo courant (ou valeur par défaut)
        String algorithm = currentAlgorithm.get() != null ? currentAlgorithm.get() : "HS256";

        String jwt = jwtUtil.generateJwt(algorithm);
        HttpHeaders headers = request.getHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwt);

        return execution.execute(request, body);
    }
}

package com.bnpp.pf.walle.access.config;

import com.bnpp.pf.walle.access.security.JwtRequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final JwtRequestInterceptor jwtRequestInterceptor;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .additionalInterceptors(jwtRequestInterceptor)
                .build();
    }
}

package com.bnpp.pf.walle.access.service;

import com.bnpp.pf.walle.access.repository.AccessRequestRepository;
import com.bnpp.pf.walle.access.security.JwtRequestInterceptor;
import com.bnpp.pf.walle.access.web.dto.NotificationRequestDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiNotificationServiceImpl implements NotificationService {

    private final RestTemplate restTemplate;
    private final AdminClient adminClient;
    private final AccessRequestRepository requestRepository;

    @Override
    public void sendNotification(NotificationRequestDto notif) {
        findCaseIdAndConfigIdById(notif.getRequestId())
                .ifPresentOrElse(
                        caseConfig -> {
                            String apigeeUrl = extractApigeeUrl(caseConfig.caseId());
                            callApigee(notif, apigeeUrl, notif.getAlgorithm()); // 🔹 algo dynamique depuis DTO
                        },
                        () -> log.warn("No case/config found for request ID: {}", notif.getRequestId())
                );
    }

    private String extractApigeeUrl(UUID caseId) {
        try {
            String adminResponse = adminClient.getCallbackUrlWithCaseId(caseId);
            JsonNode node = new ObjectMapper().readTree(adminResponse);
            return node.path("data").asText(null);
        } catch (Exception e) {
            log.error("Failed to extract Apigee URL for caseId: {}", caseId, e);
            throw new RuntimeException("Unable to extract Apigee URL", e);
        }
    }

    private void callApigee(NotificationRequestDto notif, String apigeeUrl, String algorithm) {
        if (apigeeUrl == null || apigeeUrl.isBlank()) {
            log.warn("Apigee URL is missing, skipping notification for request ID: {}", notif.getRequestId());
            return;
        }

        try {
            // 🔹 Active l’algo pour cette requête
            JwtRequestInterceptor.setAlgorithm(algorithm);

            HttpEntity<NotificationRequestDto> entity = new HttpEntity<>(notif);
            ResponseEntity<String> response = restTemplate.postForEntity(apigeeUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Notification sent successfully to {}", apigeeUrl);
            } else {
                log.warn("Notification failed: {} - {}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("Error sending notification to Apigee at {} for requestId {}", apigeeUrl, notif.getRequestId(), e);
        } finally {
            // 🔹 Nettoie le ThreadLocal pour ne pas polluer d’autres appels
            JwtRequestInterceptor.clearAlgorithm();
        }
    }

    @Override
    public Optional<CaseConfigId> findCaseIdAndConfigIdById(UUID requestId) {
        return requestRepository.findCaseIdAndConfigIdById(requestId);
    }

    public record CaseConfigId(UUID caseId, UUID configId) {}
}

