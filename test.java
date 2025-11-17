<dependencies>
    <!-- HTTP client -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- JSON support -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>

    <!-- JWT signing (Java JWT from Auth0) -->
    <dependency>
        <groupId>com.auth0</groupId>
        <artifactId>java-jwt</artifactId>
        <version>4.4.0</version>
    </dependency>

    <!-- Apache HTTP client (or use WebClient if you prefer) -->
    <dependency>
        <groupId>org.apache.httpcomponents.client5</groupId>
        <artifactId>httpclient5</artifactId>
        <version>5.2.1</version>
    </dependency>
</dependencies>
package com.example.client;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.interfaces.RSAPrivateKey;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtAssertionGenerator {

    private RSAPrivateKey loadPrivateKey() throws Exception {
        String key = Files.readString(Paths.get("src/main/resources/cert/client-key.pem"))
                .replaceAll("-----\\w+ PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = java.util.Base64.getDecoder().decode(key);

        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    public String generate(String clientId, String tokenUrl) throws Exception {
        Instant now = Instant.now();
        RSAPrivateKey privateKey = loadPrivateKey();

        Algorithm algorithm = Algorithm.RSA256(null, privateKey);

        return JWT.create()
                .withIssuer(clientId)
                .withSubject(clientId)
                .withAudience(tokenUrl)
                .withIssuedAt(Date.from(now.minusSeconds(30)))
                .withExpiresAt(Date.from(now.plusSeconds(3600)))
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithm);
    }
}
package com.example.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.fluent.*;
import org.springframework.stereotype.Component;

@Component
public class OAuthClient {

    private final ObjectMapper mapper = new ObjectMapper();

    public String requestAccessToken(String assertion, String tokenUrl, String scope) throws Exception {

        String response = Executor.newInstance()
                .execute(Request.post(tokenUrl)
                        .bodyForm(
                                Form.form()
                                        .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                                        .add("assertion", assertion)
                                        .add("scope", scope)
                                        .build()
                        )
                )
                .returnContent()
                .asString();

        JsonNode json = mapper.readTree(response);
        return json.get("access_token").asText();
    }
}
package com.example.client;

import org.springframework.stereotype.Service;

@Service
public class JwtClient {

    private final JwtAssertionGenerator assertionGenerator;
    private final OAuthClient oauthClient;

    public JwtClient(JwtAssertionGenerator assertionGenerator, OAuthClient oauthClient) {
        this.assertionGenerator = assertionGenerator;
        this.oauthClient = oauthClient;
    }

    public String getAccessToken(String clientId, String tokenUrl, String scope) throws Exception {
        String assertion = assertionGenerator.generate(clientId, tokenUrl);
        return oauthClient.requestAccessToken(assertion, tokenUrl, scope);
    }
}
package com.example.client;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TestRunner implements CommandLineRunner {

    private final JwtClient jwtClient;

    public TestRunner(JwtClient jwtClient) {
        this.jwtClient = jwtClient;
    }

    @Override
    public void run(String... args) throws Exception {
        String token = jwtClient.getAccessToken(
                "myClientId",
                "http://localhost:8080/oauth/token",
                "read write"
        );

        System.out.println("ACCESS TOKEN = " + token);
    }
}
