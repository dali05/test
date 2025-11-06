—– FILE: notification/app/port/in/SendNotificationUseCase.java
package com.bnpp.pf.walle.access.notification.app.port.in;

import com.bnpp.pf.walle.access.notification.app.model.NotificationRequestDto;

/**
	•	Port IN – defines the use case contract for sending a notification.
*/
public interface SendNotificationUseCase {
void sendNotification(NotificationRequestDto notif);
}

—– FILE: notification/app/port/out/AccessRequestRepositoryPort.java
package com.bnpp.pf.walle.access.notification.app.port.out;

import com.bnpp.pf.walle.access.notification.app.model.CaseConfigId;

import java.util.Optional;
import java.util.UUID;

/**
	•	Port OUT – defines the contract to retrieve case/config information.
*/
public interface AccessRequestRepositoryPort {
Optional findCaseIdAndConfigIdById(UUID requestId);
}

—– FILE: notification/app/port/out/AdminClientPort.java
package com.bnpp.pf.walle.access.notification.app.port.out;

import java.util.UUID;

/**
	•	Port OUT – defines the contract to retrieve callback URL from Admin API.
*/
public interface AdminClientPort {
String getCallbackUrlWithCaseId(UUID caseId);
}

—– FILE: notification/app/port/out/ApigeeNotifierPort.java
package com.bnpp.pf.walle.access.notification.app.port.out;

import com.bnpp.pf.walle.access.notification.app.model.NotificationRequestDto;

/**
	•	Port OUT – defines the contract for sending notifications to Apigee.
*/
public interface ApigeeNotifierPort {
void notifyApigee(NotificationRequestDto notif, String apigeeUrl);
}

—– FILE: notification/app/service/SendNotificationService.java
package com.bnpp.pf.walle.access.notification.app.service;

import com.bnpp.pf.walle.access.notification.app.model.CaseConfigId;
import com.bnpp.pf.walle.access.notification.app.model.NotificationRequestDto;
import com.bnpp.pf.walle.access.notification.app.port.in.SendNotificationUseCase;
import com.bnpp.pf.walle.access.notification.app.port.out.AccessRequestRepositoryPort;
import com.bnpp.pf.walle.access.notification.app.port.out.AdminClientPort;
import com.bnpp.pf.walle.access.notification.app.port.out.ApigeeNotifierPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
	•	Application Service – implements business logic for sending notifications.
*/
public class SendNotificationService implements SendNotificationUseCase {
private final AccessRequestRepositoryPort repositoryPort;
private final AdminClientPort adminClientPort;
private final ApigeeNotifierPort apigeeNotifierPort;
public SendNotificationService(AccessRequestRepositoryPort repositoryPort,
AdminClientPort adminClientPort,
ApigeeNotifierPort apigeeNotifierPort) {
this.repositoryPort = repositoryPort;
this.adminClientPort = adminClientPort;
this.apigeeNotifierPort = apigeeNotifierPort;
}
@Override
public void sendNotification(NotificationRequestDto notif) {
findCaseIdAndConfigIdById(notif.getRequestId())
.map(this::extractApigeeUrl)
.ifPresentOrElse(
apigeeUrl -> apigeeNotifierPort.notifyApigee(notif, apigeeUrl),
() -> System.out.printf(“No case/config found for request ID: %s%n”, notif.getRequestId())
);
}
private Optional findCaseIdAndConfigIdById(java.util.UUID requestId) {
return repositoryPort.findCaseIdAndConfigIdById(requestId);
}
private String extractApigeeUrl(CaseConfigId caseConfig) {
try {
String adminResponse = adminClientPort.getCallbackUrlWithCaseId(caseConfig.caseId());
JsonNode node = new ObjectMapper().readTree(adminResponse);
return node.path(“data”).asText(null);
} catch (Exception e) {
throw new RuntimeException(“Unable to extract Apigee URL”, e);
}
}
}

—– FILE: notification/app/model/CaseConfigId.java
package com.bnpp.pf.walle.access.notification.app.model;

import java.util.UUID;

/**
	•	Domain model – represents the link between case and configuration.
*/
public record CaseConfigId(UUID caseId, UUID configId) {}

—– FILE: notification/app/model/NotificationRequestDto.java
package com.bnpp.pf.walle.access.notification.app.model;

import java.util.UUID;

/**
	•	Domain model – represents the incoming notification payload.
*/
public record NotificationRequestDto(UUID requestId, String payload) {}

—– FILE: notification/adapter/out/AccessRequestRepositoryAdapter.java
package com.bnpp.pf.walle.access.notification.adapter.out;

import com.bnpp.pf.walle.access.notification.app.model.CaseConfigId;
import com.bnpp.pf.walle.access.notification.app.port.out.AccessRequestRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
	•	Adapter OUT – implements repository access via JPA or another persistence technology.
*/
@Repository
public class AccessRequestRepositoryAdapter implements AccessRequestRepositoryPort {
private final AccessRequestRepository delegate;
public AccessRequestRepositoryAdapter(AccessRequestRepository delegate) {
this.delegate = delegate;
}
@Override
public Optional findCaseIdAndConfigIdById(UUID requestId) {
return delegate.findCaseIdAndConfigIdById(requestId);
}
}

—– FILE: notification/adapter/out/AdminClientAdapter.java
package com.bnpp.pf.walle.access.notification.adapter.out;

import com.bnpp.pf.walle.access.notification.app.port.out.AdminClientPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
	•	Adapter OUT – implements the call to the Admin API.
*/
@Component
public class AdminClientAdapter implements AdminClientPort {
private final AdminClient delegate;
public AdminClientAdapter(AdminClient delegate) {
this.delegate = delegate;
}
@Override
public String getCallbackUrlWithCaseId(UUID caseId) {
return delegate.getCallbackUrlWithCaseId(caseId);
}
}

—– FILE: notification/adapter/out/ApigeeNotifierAdapter.java
package com.bnpp.pf.walle.access.notification.adapter.out;

import com.bnpp.pf.walle.access.notification.app.model.NotificationRequestDto;
import com.bnpp.pf.walle.access.notification.app.port.out.ApigeeNotifierPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
	•	Adapter OUT – performs the real HTTP call to Apigee.
*/
@Slf4j
@Component
public class ApigeeNotifierAdapter implements ApigeeNotifierPort {
private final RestTemplate restTemplate;
public ApigeeNotifierAdapter(RestTemplate restTemplate) {
this.restTemplate = restTemplate;
}
@Override
public void notifyApigee(NotificationRequestDto notif, String apigeeUrl) {
if (apigeeUrl == null || apigeeUrl.isBlank()) {
log.warn(“Apigee URL is missing, skipping notification for request ID: {}”, notif.requestId());
return;
}