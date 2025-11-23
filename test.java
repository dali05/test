
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration

package eu.example.walletwalle.model;

import java.util.List;
import java.util.Map;

public class WalletMetadata {

    private String authorization_endpoint;
    private List<String> response_types_supported;
    private List<String> response_modes_supported;

    // Par ex: { "dc+sd-jwt": { "sd_jwt_alg_values": ["ES256","ES384"] } }
    private Map<String, Object> vp_formats_supported;

    public String getAuthorization_endpoint() {
        return authorization_endpoint;
    }

    public void setAuthorization_endpoint(String authorization_endpoint) {
        this.authorization_endpoint = authorization_endpoint;
    }

    public List<String> getResponse_types_supported() {
        return response_types_supported;
    }

    public void setResponse_types_supported(List<String> response_types_supported) {
        this.response_types_supported = response_types_supported;
    }

    public List<String> getResponse_modes_supported() {
        return response_modes_supported;
    }

    public void setResponse_modes_supported(List<String> response_modes_supported) {
        this.response_modes_supported = response_modes_supported;
    }

    public Map<String, Object> getVp_formats_supported() {
        return vp_formats_supported;
    }

    public void setVp_formats_supported(Map<String, Object> vp_formats_supported) {
        this.vp_formats_supported = vp_formats_supported;
    }
}


package eu.example.walletwalle.model;

public class RequestToWalle {

    private WalletMetadata wallet_metadata;
    private String wallet_nonce;

    public WalletMetadata getWallet_metadata() {
        return wallet_metadata;
    }

    public void setWallet_metadata(WalletMetadata wallet_metadata) {
        this.wallet_metadata = wallet_metadata;
    }

    public String getWallet_nonce() {
        return wallet_nonce;
    }

    public void setWallet_nonce(String wallet_nonce) {
        this.wallet_nonce = wallet_nonce;
    }
}


package eu.example.walletwalle.model;

public class RequestObjectPayload {

    private String client_id;          // ex : "x509_san_dns:client.example.org"
    private String response_uri;       // ex : "https://client.example.org/post"
    private String response_type;      // "vp_token"
    private String response_mode;      // "direct_post.jwt"
    private Object dcql_query;         // à définir plus tard selon la spec
    private String nonce;
    private String wallet_nonce;
    private String state;

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public String getResponse_uri() {
        return response_uri;
    }

    public void setResponse_uri(String response_uri) {
        this.response_uri = response_uri;
    }

    public String getResponse_type() {
        return response_type;
    }

    public void setResponse_type(String response_type) {
        this.response_type = response_type;
    }

    public String getResponse_mode() {
        return response_mode;
    }

    public void setResponse_mode(String response_mode) {
        this.response_mode = response_mode;
    }

    public Object getDcql_query() {
        return dcql_query;
    }

    public void setDcql_query(Object dcql_query) {
        this.dcql_query = dcql_query;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getWallet_nonce() {
        return wallet_nonce;
    }

    public void setWallet_nonce(String wallet_nonce) {
        this.wallet_nonce = wallet_nonce;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}


package eu.example.walletwalle.model;

import java.util.Map;

public class AuthorizationResponsePayload {

    private String state;
    private Map<String, Object> vp_token;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Map<String, Object> getVp_token() {
        return vp_token;
    }

    public void setVp_token(Map<String, Object> vp_token) {
        this.vp_token = vp_token;
    }
}


package eu.example.walletwalle.service;

import eu.example.walletwalle.model.RequestObjectPayload;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    // Clé symétrique de démo – à remplacer par une vraie gestion de clés
    private final SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public String generateRequestObjectJwt(RequestObjectPayload payload) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("client_id", payload.getClient_id())
                .claim("response_uri", payload.getResponse_uri())
                .claim("response_type", payload.getResponse_type())
                .claim("response_mode", payload.getResponse_mode())
                .claim("dcql_query", payload.getDcql_query())
                .claim("nonce", payload.getNonce())
                .claim("wallet_nonce", payload.getWallet_nonce())
                .claim("state", payload.getState())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300))) // 5 minutes
                .signWith(secretKey)
                .compact();
    }

    public String parseJwtWithoutVerification(String jwt) {
        // Pour la démo, on ne vérifie pas la signature.
        // En production : Jwts.parserBuilder().setSigningKey(...).build().parseClaimsJws(jwt)
        return Jwts.parserBuilder()
                .setSigningKey(secretKey) // ici on “vérifie” avec la même clé
                .build()
                .parseClaimsJws(jwt)
                .getBody()
                .toString();
    }
}


package eu.example.walletwalle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.example.walletwalle.model.AuthorizationResponsePayload;
import eu.example.walletwalle.model.RequestObjectPayload;
import eu.example.walletwalle.model.RequestToWalle;
import eu.example.walletwalle.service.JwtService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping
public class WalletWalleController {

    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public WalletWalleController(ObjectMapper objectMapper, JwtService jwtService) {
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
    }

    // 1) POST /playground/request/{requestId}?response_code=...
    @PostMapping("/playground/request/{requestId}")
    public ResponseEntity<Void> playgroundRequest(
            @PathVariable String requestId,
            @RequestParam(name = "response_code", required = false) String responseCode) {

        // Ici tu peux simuler de la logique selon response_code si besoin
        if (responseCode != null && responseCode.equals("400")) {
            return ResponseEntity.badRequest().build();
        }

        if (responseCode != null && responseCode.equals("500")) {
            return ResponseEntity.status(500).build();
        }

        return ResponseEntity.ok().build();
    }

    // 2) POST /request/{requestId} (x-www-form-urlencoded)
    // On considère que le Wallet envoie un champ form "wallet_metadata" (JSON) et "wallet_nonce".
    @PostMapping(value = "/request/{requestId}",
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> createRequestObject(
            @PathVariable String requestId,
            @RequestBody MultiValueMap<String, String> formData) {

        try {
            String walletMetadataJson = formData.getFirst("wallet_metadata");
            String walletNonce = formData.getFirst("wallet_nonce");

            if (walletMetadataJson == null) {
                // On peut considérer que c'est un invalid request
                return ResponseEntity.badRequest().body("wallet_metadata is required");
            }

            RequestToWalle requestToWalle = new RequestToWalle();
            requestToWalle.setWallet_metadata(
                    objectMapper.readValue(walletMetadataJson,
                            eu.example.walletwalle.model.WalletMetadata.class));
            requestToWalle.setWallet_nonce(walletNonce);

            // Génération d’un Request Object (payload) conforme à ta spec
            RequestObjectPayload payload = new RequestObjectPayload();
            payload.setClient_id("x509_san_dns:client.example.org"); // ou "Wall-e"
            payload.setResponse_uri("https://client.example.org/post");
            payload.setResponse_type("vp_token");
            payload.setResponse_mode("direct_post.jwt");
            payload.setDcql_query(Map.of("query", "to be defined")); // placeholder
            payload.setNonce(generateNonce());
            payload.setWallet_nonce(requestToWalle.getWallet_nonce());
            payload.setState(UUID.randomUUID().toString());

            String jwt = jwtService.generateRequestObjectJwt(payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/oauth-authz-request+jwt"));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(jwt);

        } catch (Exception e) {
            // en cas d’erreur interne
            return ResponseEntity.status(500).body("Internal Server Error");
        }
    }

    // 3) POST /response/{requestId} (x-www-form-urlencoded)
    // form: response=<base64url(JWT)>
    @PostMapping(value = "/response/{requestId}",
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleAuthorizationResponse(
            @PathVariable String requestId,
            @RequestBody MultiValueMap<String, String> formData) {

        try {
            String encodedResponse = formData.getFirst("response");
            if (encodedResponse == null) {
                return ResponseEntity.badRequest().body("response is required");
            }

            // Base64url decode du JWT
            // Ici on suppose que "response" est déjà le JWT et pas encore re-encodé;
            // si c'est vraiment base64url(jwt), on doit d'abord décoder:
            // String jwt = new String(Base64.getUrlDecoder().decode(encodedResponse));
            String jwt = new String(Base64.getUrlDecoder().decode(encodedResponse));

            // On peut parser le JWT pour vérification / logging
            String claims = jwtService.parseJwtWithoutVerification(jwt);
            System.out.println("Received Authorization Response JWT claims: " + claims);

            // En prod : vérifier state, nonce, etc.

            return ResponseEntity.ok("OK");

        } catch (IllegalArgumentException e) {
            // problème d’encoding → invalid request
            return ResponseEntity.badRequest().body("Invalid request");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal Server Error");
        }
    }

    private String generateNonce() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}


package eu.example.walletwalle.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body("Invalid request");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAny(Exception ex) {
        // logger.error("Unexpected error", ex);
        return ResponseEntity.status(500).body("Internal Server Error");
    }
}
