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

import com.bnpp.pf.walle.access.process.adapter.out.apigee.interceptor.JwtRequestInterceptor;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLContexts;
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
        // 1️⃣ Charger les keystore et truststore
        KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword, keyStoreType);
        KeyStore trustStore = loadKeyStore(trustStorePath, trustStorePassword, trustStoreType);

        // 2️⃣ Construire le contexte SSL
        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
                .loadTrustMaterial(trustStore, null)
                .build();

        // 3️⃣ Construire le socket factory TLS 1.2 / 1.3
        SSLConnectionSocketFactory sslSocketFactory =
                new SSLConnectionSocketFactory(sslContext, new String[]{"TLSv1.2", "TLSv1.3"}, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        // 4️⃣ Construire le HttpClient mTLS
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setSSLSocketFactory(sslSocketFactory)
                                .build()
                )
                .build();

        // 5️⃣ Lier à Spring RestTemplate
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return builder
                .requestFactory(() -> requestFactory)
                .additionalInterceptors(jwtRequestInterceptor)
                .build();
    }

    private KeyStore loadKeyStore(Resource resource, String password, String type) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type);
        try (var inputStream = resource.getInputStream()) {
            keyStore.load(inputStream, password.toCharArray());
        }
        return keyStore;
    }
}