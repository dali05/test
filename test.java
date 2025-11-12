<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>4.4.0</version>
</dependency>


package com.example.demo.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    // Clé secrète utilisée pour signer et vérifier les tokens (doit être la même que celle utilisée à la création)
    private static final String SECRET = "MaSuperCleSecrete123!";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("⚠️ Aucun token présent.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token manquant\"}");
            return;
        }

        String token = authHeader.substring(7); // Supprimer "Bearer "

        try {
            // Créer l'algorithme avec la clé secrète
            Algorithm algorithm = Algorithm.HMAC256(SECRET);

            // Créer un vérificateur
            JWTVerifier verifier = JWT.require(algorithm).build();

            // Vérifier le token (vérifie signature + expiration)
            DecodedJWT decodedJWT = verifier.verify(token);

            // Si on arrive ici, le token est valide
            System.out.println("✅ Token valide !");
            System.out.println("Algorithme : " + decodedJWT.getAlgorithm());
            System.out.println("Sujet : " + decodedJWT.getSubject());
            System.out.println("Émetteur : " + decodedJWT.getIssuer());

            // Continuer la requête
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // Signature invalide, token expiré, etc.
            System.out.println("❌ Token invalide : " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token invalide ou expiré\"}");
        }
    }
}