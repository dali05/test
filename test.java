# 1. Générer la clé privée du client
openssl genrsa -out client-key.pem 2048

# 2. Générer le certificat X.509 (clé publique + métadonnées)
openssl req -new -x509 -key client-key.pem -out client-cert.pem -days 365 \
  -subj "/CN=jwt-client"
<dependencies>
    <!-- REST -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- JWT (Nimbus pour vérifier la signature) -->
    <dependency>
        <groupId>com.nimbusds</groupId>
        <artifactId>nimbus-jose-jwt</artifactId>
        <version>9.37.3</version> <!-- ou version récente -->
    </dependency>

    <!-- JWT pour générer le access_token (facultatif, tu peux aussi utiliser Nimbus) -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>



  // src/main/java/com/example/auth/JwtClientPublicKeyProvider.java
package com.example.auth;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Component
public class JwtClientPublicKeyProvider {

    private final PublicKey publicKey;

    public JwtClientPublicKeyProvider() {
        try {
            ClassPathResource resource = new ClassPathResource("cert/client-cert.pem");
            try (InputStream in = resource.getInputStream()) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
                this.publicKey = cert.getPublicKey();
            }
        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger le certificat client", e);
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}


// src/main/java/com/example/auth/JwtAssertionValidator.java
package com.example.auth;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtAssertionValidator {

    private final JwtClientPublicKeyProvider keyProvider;

    public JwtAssertionValidator(JwtClientPublicKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    public void validateAssertion(String assertion, String expectedAudience, String expectedClientId) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(assertion);

        // 1. Vérifier la signature
        RSAPublicKey publicKey = (RSAPublicKey) keyProvider.getPublicKey();
        JWSVerifier verifier = new RSASSAVerifier(publicKey);
        if (!signedJWT.verify(verifier)) {
            throw new RuntimeException("Signature JWT invalide");
        }

        // 2. Vérifier les claims
        var claims = signedJWT.getJWTClaimsSet();

        String iss = claims.getIssuer();
        String sub = claims.getSubject();
        String aud = claims.getAudience().isEmpty() ? null : claims.getAudience().get(0);
        Date exp = claims.getExpirationTime();
        Date nbf = claims.getNotBeforeTime();
        Date now = Date.from(Instant.now());

        if (!expectedClientId.equals(iss) || !expectedClientId.equals(sub)) {
            throw new RuntimeException("iss/sub invalide");
        }

        if (!expectedAudience.equals(aud)) {
            throw new RuntimeException("aud invalide");
        }

        if (exp == null || exp.before(now)) {
            throw new RuntimeException("JWT expiré");
        }

        if (nbf != null && nbf.after(now)) {
            throw new RuntimeException("JWT pas encore valide (nbf)");
        }

        // ici tu peux aussi vérifier jti, scope, useCase, etc.
    }
}


// src/main/java/com/example/auth/TokenController.java
package com.example.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@RestController
public class TokenController {

    private final JwtAssertionValidator validator;

    // secret utilisé pour signer le access_token (coté serveur)
    // en prod -> à mettre dans un fichier config / vault
    private static final String ACCESS_TOKEN_SECRET = "change-me-super-secret";

    private static final String EXPECTED_CLIENT_ID = "myClientId"; // même que côté Node
    private static final String TOKEN_ENDPOINT = "http://localhost:8080/oauth/token";

    public TokenController(JwtAssertionValidator validator) {
        this.validator = validator;
    }

    @PostMapping(
            value = "/oauth/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam("assertion") String assertion,
            @RequestParam(value = "scope", required = false) String scope
    ) throws Exception {

        if (!"urn:ietf:params:oauth:grant-type:jwt-bearer".equals(grantType)) {
            throw new RuntimeException("grant_type invalide");
        }

        // 1. Valider le JWT envoyé par le client
        validator.validateAssertion(assertion, TOKEN_ENDPOINT, EXPECTED_CLIENT_ID);

        // 2. Générer un access_token (ici un JWT signé HS256 pour simplifier)
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(3600);

        String accessToken = Jwts.builder()
                .setSubject(EXPECTED_CLIENT_ID)
                .setIssuer("my-auth-server")
                .setAudience("my-resource-server") // ou API que tu veux protéger
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("scope", scope)
                .signWith(SignatureAlgorithm.HS256, ACCESS_TOKEN_SECRET.getBytes())
                .compact();

        return Map.of(
                "access_token", accessToken,
                "token_type", "Bearer",
                "expires_in", 3600
        );
    }
}
const authBody = {
  assertion: this.signAssertion(clientId, authTokenUrl, useCase),
  grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
  scope: scope,
};
