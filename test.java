package com.bnpp.pf.walle.access.configs;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import javax.net.ssl.SSLContext;
import java.security.KeyStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;

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
        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(loadKeyStore(keyStorePath, keyStorePassword, keyStoreType), keyStorePassword.toCharArray())
                .loadTrustMaterial(loadKeyStore(trustStorePath, trustStorePassword, trustStoreType), null)
                .build();

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return builder
                .requestFactory(() -> requestFactory)
                .additionalInterceptors(jwtRequestInterceptor)
                .build();
    }

    private KeyStore loadKeyStore(Resource resource, String password, String type) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type);
        try (var stream = resource.getInputStream()) {
            keyStore.load(stream, password.toCharArray());
        }
        return keyStore;
    }
}