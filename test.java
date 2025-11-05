package com.bnpp.pf.walle.access.app.notification;

import com.bnpp.pf.walle.access.app.notification.model.NotificationRequestDto;

public interface NotificationService {
    void sendNotification(NotificationRequestDto notif);
}


package com.bnpp.pf.walle.access.app.notification.model;

import java.util.UUID;

public record CaseConfigId(UUID caseId, UUID configId) {}

package com.bnpp.pf.walle.access.app.notification.port.out;

import com.bnpp.pf.walle.access.app.notification.model.CaseConfigId;

import java.util.Optional;
import java.util.UUID;

public interface AccessRequestRepositoryPort {
    Optional<CaseConfigId> findCaseIdAndConfigIdById(UUID requestId);
}


package com.bnpp.pf.walle.access.app.notification.port.out;

import java.util.UUID;

public interface AdminClientPort {
    String getCallbackUrl(UUID caseId);
}


package com.bnpp.pf.walle.access.app.notification.port.out;

import com.bnpp.pf.walle.access.app.notification.model.NotificationRequestDto;

public interface ApigeeNotifierPort {
    void sendToApigee(NotificationRequestDto notif, String apigeeUrl);
}

package com.bnpp.pf.walle.access.app.notification;

import com.bnpp.pf.walle.access.app.notification.model.CaseConfigId;
import com.bnpp.pf.walle.access.app.notification.model.NotificationRequestDto;
import com.bnpp.pf.walle.access.app.notification.port.out.AccessRequestRepositoryPort;
import com.bnpp.pf.walle.access.app.notification.port.out.AdminClientPort;
import com.bnpp.pf.walle.access.app.notification.port.out.ApigeeNotifierPort;

import java.util.Optional;

public class NotificationServiceImpl implements NotificationService {

    private final AccessRequestRepositoryPort requestRepository;
    private final AdminClientPort adminClient;
    private final ApigeeNotifierPort apigeeNotifier;

    public NotificationServiceImpl(AccessRequestRepositoryPort requestRepository,
                                   AdminClientPort adminClient,
                                   ApigeeNotifierPort apigeeNotifier) {
        this.requestRepository = requestRepository;
        this.adminClient = adminClient;
        this.apigeeNotifier = apigeeNotifier;
    }

    @Override
    public void sendNotification(NotificationRequestDto notif) {
        Optional<CaseConfigId> configOpt = requestRepository.findCaseIdAndConfigIdById(notif.getRequestId());

        if (configOpt.isEmpty()) {
            System.out.printf("No case/config found for request ID: %s%n", notif.getRequestId());
            return;
        }

        CaseConfigId config = configOpt.get();
        String apigeeUrl = adminClient.getCallbackUrl(config.caseId());
        apigeeNotifier.sendToApigee(notif, apigeeUrl);
    }
}


package com.bnpp.pf.walle.access.adapter.out;

import com.bnpp.pf.walle.access.app.notification.model.CaseConfigId;
import com.bnpp.pf.walle.access.app.notification.port.out.AccessRequestRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AccessRequestRepositoryAdapter implements AccessRequestRepositoryPort {

    private final AccessRequestRepository jpaRepository;

    @Override
    public Optional<CaseConfigId> findCaseIdAndConfigIdById(UUID requestId) {
        return jpaRepository.findCaseIdAndConfigIdById(requestId);
    }
}


package com.bnpp.pf.walle.access.adapter.out;

import com.bnpp.pf.walle.access.app.notification.port.out.AdminClientPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminClientAdapter implements AdminClientPort {

    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getCallbackUrl(UUID caseId) {
        try {
            String response = adminClient.getCallbackUrlWithCaseId(caseId);
            JsonNode node = objectMapper.readTree(response);
            return node.path("data").asText(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to extract Apigee URL", e);
        }
    }
}

package com.bnpp.pf.walle.access.adapter.out;

import com.bnpp.pf.walle.access.app.notification.model.NotificationRequestDto;
import com.bnpp.pf.walle.access.app.notification.port.out.ApigeeNotifierPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApigeeNotifierAdapter implements ApigeeNotifierPort {

    private final RestTemplate restTemplate;

    @Override
    public void sendToApigee(NotificationRequestDto notif, String apigeeUrl) {
        if (apigeeUrl == null || apigeeUrl.isBlank()) {
            log.warn("Apigee URL is missing, skipping notification for request ID: {}", notif.getRequestId());
            return;
        }

        try {
            HttpEntity<NotificationRequestDto> entity = new HttpEntity<>(notif);
            ResponseEntity<String> response = restTemplate.postForEntity(apigeeUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Notification sent successfully to {}", apigeeUrl);
            } else {
                log.warn("Notification failed: {} - {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error sending notification to Apigee: {}", e.getMessage(), e);
        }
    }
}


package com.bnpp.pf.walle.access.config;

import com.bnpp.pf.walle.access.app.notification.NotificationService;
import com.bnpp.pf.walle.access.app.notification.NotificationServiceImpl;
import com.bnpp.pf.walle.access.app.notification.port.out.AccessRequestRepositoryPort;
import com.bnpp.pf.walle.access.app.notification.port.out.AdminClientPort;
import com.bnpp.pf.walle.access.app.notification.port.out.ApigeeNotifierPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfig {

    @Bean
    public NotificationService notificationService(AccessRequestRepositoryPort repo,
                                                   AdminClientPort adminClient,
                                                   ApigeeNotifierPort notifier) {
        return new NotificationServiceImpl(repo, adminClient, notifier);
    }
}








