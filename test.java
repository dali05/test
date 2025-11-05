package com.example.demo.filter;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwt;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtCheckFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ Aucun token JWT trouvé dans la requête");
        } else {
            try {
                String token = authHeader.substring(7); // Enlever "Bearer "
                
                // On lit juste l’en-tête du JWT sans valider la signature
                Jwt<Header, ?> jwt = Jwts.parserBuilder()
                        .build()
                        .parse(token);

                String alg = jwt.getHeader().getAlgorithm();
                System.out.println("✅ Token présent. Algorithme utilisé : " + alg);

            } catch (Exception e) {
                System.out.println("⚠️ Erreur lors du parsing du JWT : " + e.getMessage());
            }
        }

        // Continue la chaîne des filtres
        filterChain.doFilter(request, response);
    }
}



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
