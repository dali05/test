spring:
  ssl:
    bundles:
      apigee:                    # nom du bundle SSL
        key:
          store: classpath:ssl/client-keystore.p12
          password: changeit
        trust:
          store: classpath:ssl/truststore.jks
          password: changeit


package com.bnpp.pf.walle.access.configs;

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
                .setSslBundle("apigee")                 // 🔐 active le mTLS
                .additionalInterceptors(jwtRequestInterceptor)
                .build();
    }
}