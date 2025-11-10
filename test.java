package com.bnpp.pf.walle.access.configs;

import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactoryRegistry;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final JwtRequestInterceptor jwtRequestInterceptor;
    private final SslBundles sslBundles;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // 1️⃣ Récupère le bundle SSL "apigee"
        SslBundle apigeeBundle = sslBundles.getBundle("apigee");

        // 2️⃣ Crée le SSLConnectionSocketFactory avec le SSLContext du bundle
        SSLConnectionSocketFactory sslSocketFactory =
                SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(apigeeBundle.createSslContext())
                        .build();

        // 3️⃣ Enregistre les protocoles http/https
        ConnectionSocketFactoryRegistry socketFactoryRegistry = RegistryBuilder
                .<org.apache.hc.client5.http.socket.ConnectionSocketFactory>create()
                .register("https", sslSocketFactory)
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .build();

        // 4️⃣ Connection Manager pour HttpClient
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        // 5️⃣ Crée le HttpClient avec le manager TLS
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        // 6️⃣ Crée la factory Spring pour RestTemplate
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        // 7️⃣ Construit le RestTemplate
        return builder
                .requestFactory(() -> requestFactory)
                .additionalInterceptors(jwtRequestInterceptor)
                .build();
    }
}