package com.bnpp.pf.walle.access.config;

import com.bnpp.pf.walle.access.service.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtRequestInterceptor implements ClientHttpRequestInterceptor {

    private final JwtUtil jwtUtil;

    // Nom du header "spécial" pour ignorer l’ajout automatique
    private static final String SKIP_JWT_HEADER = "X-Skip-JWT";

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {

        HttpHeaders headers = request.getHeaders();

        // Si le header "X-Skip-JWT" est présent, ne pas ajouter le JWT
        if (!headers.containsKey(SKIP_JWT_HEADER)) {
            String jwt = jwtUtil.generateJwt();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(jwt);
        } else {
            // Supprime ce header interne pour qu'il ne parte pas vers le serveur
            headers.remove(SKIP_JWT_HEADER);
        }

        return execution.execute(request, body);
    }
}