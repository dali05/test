package com.bnpp.pf.walle.access.configs;

import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
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
        // 1️⃣ Récupérer le bundle SSL "apigee"
        SslBundle apigeeBundle = sslBundles.getBundle("apigee");

        // 2️⃣ Construire un HttpClient à partir du SSLContext du bundle
        SSLConnectionSocketFactory socketFactory =
                new SSLConnectionSocketFactory(apigeeBundle.createSslContext());

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        // 3️⃣ Construire le RestTemplate
        return builder
                .requestFactory(() -> requestFactory)
                .additionalInterceptors(jwtRequestInterceptor)
                .build();
    }
}