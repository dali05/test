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

    // ⚙️ Clé secrète pour vérifier le token (à adapter selon ton app)
    private static final String SECRET_KEY = "monSecret123";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Supprimer "Bearer "

            try {
                // Décoder le token sans validation pour afficher les infos
                DecodedJWT decodedJWT = JWT.decode(token);
                String algorithm = decodedJWT.getAlgorithm();
                System.out.println("✅ Algorithme du token : " + algorithm);

                // Valider le token (signature, expiration, etc.)
                Algorithm alg = Algorithm.HMAC256(SECRET_KEY);
                JWTVerifier verifier = JWT.require(alg).build();
                verifier.verify(token);

                System.out.println("✅ Token valide.");

            } catch (Exception e) {
                System.out.println("❌ Token invalide : " + e.getMessage());
            }

        } else {
            System.out.println("⚠️ Aucun token présent dans la requête.");
        }

        // 🔁 Toujours laisser passer la requête
        filterChain.doFilter(request, response);
    }
}