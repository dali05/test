package com.bnpp.pf.walle.access.configs;

import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyStore;

@Configuration
@RequiredArgsConstructor
public class MtlsRestTemplateConfig {

    @Value("${apigee.mtls.keystore.path}")
    private Resource keyStorePath;

    @Value("${apigee.mtls.keystore.password}")
    private String keyStorePassword;

    @Value("${apigee.mtls.keystore.type}")
    private String keyStoreType;

    @Value("${apigee.mtls.truststore.path}")
    private Resource trustStorePath;

    @Value("${apigee.mtls.truststore.password}")
    private String trustStorePassword;

    @Value("${apigee.mtls.truststore.type}")
    private String trustStoreType;

    private final JwtRequestInterceptor jwtRequestInterceptor;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) throws Exception {

        // 1️⃣ Charger les keystores
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        try (var ks = keyStorePath.getInputStream()) {
            keyStore.load(ks, keyStorePassword.toCharArray());
        }

        KeyStore trustStore = KeyStore.getInstance(trustStoreType);
        try (var ts = trustStorePath.getInputStream()) {
            trustStore.load(ts, trustStorePassword.toCharArray());
        }

        // 2️⃣ Construire le contexte SSL
        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
                .loadTrustMaterial(trustStore, (TrustStrategy) null)
                .build();

        // 3️⃣ Configurer la stratégie TLS
        var tlsStrategy = ClientTlsStrategyBuilder.create()
                .setSslContext(sslContext)
                .build();

        // 4️⃣ Créer le connection manager
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setTlsStrategy(tlsStrategy)
                        .build();

        // 5️⃣ Construire le client HTTP
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        // 6️⃣ Adapter pour RestTemplate
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return builder
                .requestFactory(() -> factory)
                .additionalInterceptors(jwtRequestInterceptor)
                .build();
    }
}