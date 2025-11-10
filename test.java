package com.bnpp.pf.walle.access.configs;

import lombok.RequiredArgsConstructor;
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
    private final SslBundles sslBundles; // Injecté automatiquement par Spring

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // 1️⃣ Récupérer le bundle SSL "apigee" défini dans le YAML
        SslBundle apigeeBundle = sslBundles.getBundle("apigee");

        // 2️⃣ Créer une factory HTTP basée sur ce bundle
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(apigeeBundle.createHttpClient());

        // 3️⃣ Construire le RestTemplate
        return builder
                .requestFactory(() -> requestFactory)
                .additionalInterceptors(jwtRequestInterceptor)
                .build();
    }
}