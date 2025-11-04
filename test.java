
package com.bnpp.pf.walle.access.config;

import com.bnpp.pf.walle.access.service.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://localhost:8082/api/v1")
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .filter(addJwtFilter()) // Ajout automatique du JWT
                .build();
    }

    private ExchangeFilterFunction addJwtFilter() {
        return (request, next) -> {
            String jwt = jwtUtil.generateJwt();
            return next.exchange(
                    ClientRequest.from(request)
                            .header("Authorization", "Bearer " + jwt)
                            .build()
            );
        };
    }
}
