package com.bnpp.pf.walle.access.configs;

import lombok.RequiredArgsConstructor;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final JwtRequestInterceptor jwtRequestInterceptor;

    @Value("${apigee.ssl.key-store}")
    private String keyStorePath;

    @Value("${apigee.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${apigee.ssl.key-store-type}")
    private String keyStoreType;

    @Value("${apigee.ssl.trust-store}")
    private String trustStorePath;

    @Value("${apigee.ssl.trust-store-password}")
    private String trustStorePassword;

    @Value("${apigee.ssl.trust-store-type}")
    private String trustStoreType;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) throws Exception {
        // Charger le keystore client
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        try (InputStream keyStoreStream = getClass().getResourceAsStream(keyStorePath.replace("classpath:", "/"))) {
            keyStore.load(keyStoreStream, keyStorePassword.toCharArray());
        }

        // Charger le truststore (certificats de confiance)
        KeyStore trustStore = KeyStore.getInstance(trustStoreType);
        try (InputStream trustStoreStream = getClass().getResourceAsStream(trustStorePath.replace("classpath:", "/"))) {
            trustStore.load(trustStoreStream, trustStorePassword.toCharArray());
        }

        // Construire le contexte SSL pour le mTLS
        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, keyStorePassword.toCharArray()) // ton cert client
                .loadTrustMaterial(trustStore, null)                       // CA d’Apigee
                .build();

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);
        HttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return builder
                .requestFactory(() -> requestFactory)
                .additionalInterceptors(jwtRequestInterceptor)
                .build();
    }
}


@Component
@RequiredArgsConstructor
@Slf4j
public class ApigeeClient {

    private final RestTemplate restTemplate;

    public void notifyApigee(NotificationContext context, String apigeeUrl) {
        if (apigeeUrl == null || apigeeUrl.isBlank()) {
            log.warn("Apigee URL is missing, skipping notification for request ID: {}", context.requestId());
            return;
        }

        try {
            HttpEntity<NotificationContext> entity = new HttpEntity<>(context);
            ResponseEntity<String> response =
                    restTemplate.postForEntity(apigeeUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Notification sent successfully to {}", apigeeUrl);
            } else {
                log.warn("Notification failed: {} - {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error sending notification to Apigee at {} for requestId {}", apigeeUrl, context.requestId(), e);
        }
    }
}
