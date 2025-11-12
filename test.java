package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Récupérer l'en-tête Authorization
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Supprimer "Bearer "
            try {
                // Décoder le token sans le valider
                DecodedJWT decodedJWT = JWT.decode(token);
                
                // Récupérer et afficher l’algorithme
                String algorithm = decodedJWT.getAlgorithm();
                System.out.println("✅ Algorithme du token : " + algorithm);

            } catch (Exception e) {
                System.out.println("❌ Token invalide ou illisible : " + e.getMessage());
            }
        } else {
            System.out.println("⚠️ Aucun token présent dans la requête.");
        }

        // Continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }
}