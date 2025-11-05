package com.bnpp.pf.walle.access.config;

import com.bnpp.pf.walle.access.service.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtRequestInterceptor implements ClientHttpRequestInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public ClientHttpResponse intercept(
            org.springframework.http.HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        String jwt = jwtUtil.generateJwt();
        HttpHeaders headers = request.getHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwt);

        return execution.execute(request, body);
    }
}


package com.bnpp.pf.walle.access.config;

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
                .additionalInterceptors(jwtRequestInterceptor)
                .build();
    }
}