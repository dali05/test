
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>

  <dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
    <version>9.40</version>
  </dependency>
</dependencies>

walle:
  oid4vp:
    client-id: "https://relying-party.example.org"
    response-mode: "direct_post_jwt"
    response-type: "vp_token"
    response-uri: "https://relying-party.example.org/response_uri"

    kid: "9tjcAivhLMVUJ3AXWGZ_g"
    trust-chain:
      - "MIICajCCAdOgAWIBAgIC....awz"
      - "MIICajCCAdOgAWIBAgIC....a23"
      - "MIICajCCAdOgAWIBAgIC....sf2"

    token-ttl-seconds: 3600

    # Base64 d'une clé privée EC PKCS8 DER (depuis env)
    ec-private-key-b64: "${WALLE_EC_PRIVATE_KEY_B64:}"

package com.example.walle.oid4vp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "walle.oid4vp")
public class Oid4vpProperties {

    private String clientId;
    private String responseMode;
    private String responseType;
    private String responseUri;

    private String kid;
    private List<String> trustChain;

    private long tokenTtlSeconds = 3600;
    private String ecPrivateKeyB64;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getResponseMode() { return responseMode; }
    public void setResponseMode(String responseMode) { this.responseMode = responseMode; }

    public String getResponseType() { return responseType; }
    public void setResponseType(String responseType) { this.responseType = responseType; }

    public String getResponseUri() { return responseUri; }
    public void setResponseUri(String responseUri) { this.responseUri = responseUri; }

    public String getKid() { return kid; }
    public void setKid(String kid) { this.kid = kid; }

    public List<String> getTrustChain() { return trustChain; }
    public void setTrustChain(List<String> trustChain) { this.trustChain = trustChain; }

    public long getTokenTtlSeconds() { return tokenTtlSeconds; }
    public void setTokenTtlSeconds(long tokenTtlSeconds) { this.tokenTtlSeconds = tokenTtlSeconds; }

    public String getEcPrivateKeyB64() { return ecPrivateKeyB64; }
    public void setEcPrivateKeyB64(String ecPrivateKeyB64) { this.ecPrivateKeyB64 = ecPrivateKeyB64; }
}



package com.example.walle.oid4vp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Oid4vpProperties.class)
public class AppConfig {}



package com.example.walle.oid4vp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.Map;

public class WalletMetadata {

    @JsonProperty("authorization_endpoint")
    @NotBlank(message = "wallet_metadata.authorization_endpoint is required")
    @Pattern(regexp = "https?://.+", message = "wallet_metadata.authorization_endpoint must be a valid http(s) URL")
    private String authorizationEndpoint;

    @JsonProperty("vp_formats_supported")
    @NotEmpty(message = "wallet_metadata.vp_formats_supported is required and must not be empty")
    private Map<String, Object> vpFormatsSupported;

    @JsonProperty("response_types_supported")
    private List<String> responseTypesSupported;

    @JsonProperty("response_modes_supported")
    private List<String> responseModesSupported;

    @JsonProperty("alg_values_supported")
    private List<String> algValuesSupported;

    public String getAuthorizationEndpoint() { return authorizationEndpoint; }
    public void setAuthorizationEndpoint(String authorizationEndpoint) { this.authorizationEndpoint = authorizationEndpoint; }

    public Map<String, Object> getVpFormatsSupported() { return vpFormatsSupported; }
    public void setVpFormatsSupported(Map<String, Object> vpFormatsSupported) { this.vpFormatsSupported = vpFormatsSupported; }

    public List<String> getResponseTypesSupported() { return responseTypesSupported; }
    public void setResponseTypesSupported(List<String> responseTypesSupported) { this.responseTypesSupported = responseTypesSupported; }

    public List<String> getResponseModesSupported() { return responseModesSupported; }
    public void setResponseModesSupported(List<String> responseModesSupported) { this.responseModesSupported = responseModesSupported; }

    public List<String> getAlgValuesSupported() { return algValuesSupported; }
    public void setAlgValuesSupported(List<String> algValuesSupported) { this.algValuesSupported = algValuesSupported; }
}




package com.example.walle.oid4vp.crypto;

import com.example.walle.oid4vp.config.Oid4vpProperties;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Component
public class SigningKeyProvider {

    private final ECPrivateKey ecPrivateKey;

    public SigningKeyProvider(Oid4vpProperties props) {
        String b64 = props.getEcPrivateKeyB64();
        if (b64 == null || b64.isBlank()) {
            throw new IllegalStateException("Missing walle.oid4vp.ec-private-key-b64 (env WALLE_EC_PRIVATE_KEY_B64)");
        }
        this.ecPrivateKey = loadPkcs8EcPrivateKey(b64);
    }

    public ECPrivateKey getEcPrivateKey() {
        return ecPrivateKey;
    }

    private static ECPrivateKey loadPkcs8EcPrivateKey(String base64Pkcs8Der) {
        try {
            byte[] der = Base64.getDecoder().decode(base64Pkcs8Der);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            KeyFactory kf = KeyFactory.getInstance("EC");
            return (ECPrivateKey) kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load EC private key (PKCS8 base64)", e);
        }
    }
}



package com.example.walle.oid4vp.service;

import com.example.walle.oid4vp.config.Oid4vpProperties;
import com.example.walle.oid4vp.crypto.SigningKeyProvider;
import com.example.walle.oid4vp.model.WalletMetadata;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class Oid4vpRequestService {

    private final Oid4vpProperties props;
    private final SigningKeyProvider signingKeyProvider;

    public Oid4vpRequestService(Oid4vpProperties props, SigningKeyProvider signingKeyProvider) {
        this.props = props;
        this.signingKeyProvider = signingKeyProvider;
    }

    public String createSignedRequestObject(WalletMetadata walletMetadata, String walletNonce) {
        // alg = premier élément de sd-jwt_alg_values
        JWSAlgorithm jwsAlgorithm = selectAlg(walletMetadata);

        // values générées
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getTokenTtlSeconds());

        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .claim("client_id", props.getClientId())
                .claim("response_mode", props.getResponseMode())
                .claim("response_type", props.getResponseType())
                .claim("dcql_query", fixedDcqlQuery())
                .claim("response_uri", props.getResponseUri())
                .claim("nonce", nonce)
                .claim("state", state)
                .claim("iss", props.getClientId())
                .claim("iat", now.getEpochSecond())
                .claim("exp", exp.getEpochSecond())
                .claim("request_uri_method", "post");

        if (walletNonce != null && !walletNonce.isBlank()) {
            claims.claim("wallet_nonce", walletNonce);
        }

        JWSHeader header = new JWSHeader.Builder(jwsAlgorithm)
                .type(new JOSEObjectType("oauth-authz-req+jwt"))
                .keyID(props.getKid())
                .customParam("trust_chain", props.getTrustChain())
                .build();

        SignedJWT jwt = new SignedJWT(header, claims.build());

        try {
            jwt.sign(new ECDSASigner(signingKeyProvider.getEcPrivateKey()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign Request Object JWT", e);
        }
    }

    @SuppressWarnings("unchecked")
    private JWSAlgorithm selectAlg(WalletMetadata walletMetadata) {
        Map<String, Object> vpFormats = walletMetadata.getVpFormatsSupported();

        Object dcSdJwtObj = vpFormats.get("dc+sd-jwt");
        if (!(dcSdJwtObj instanceof Map)) {
            throw new IllegalArgumentException("vp_formats_supported must contain 'dc+sd-jwt' object");
        }

        Map<String, Object> dcSdJwt = (Map<String, Object>) dcSdJwtObj;

        Object algValuesObj = dcSdJwt.get("sd-jwt_alg_values");
        if (!(algValuesObj instanceof List)) {
            throw new IllegalArgumentException("vp_formats_supported['dc+sd-jwt'].sd-jwt_alg_values must be an array");
        }

        List<?> algValues = (List<?>) algValuesObj;
        if (algValues.isEmpty() || !(algValues.get(0) instanceof String) || ((String) algValues.get(0)).isBlank()) {
            throw new IllegalArgumentException("sd-jwt_alg_values must contain at least one non-empty string");
        }

        String firstAlg = (String) algValues.get(0);
        JWSAlgorithm jwsAlg = JWSAlgorithm.parse(firstAlg);

        if (!JWSAlgorithm.Family.EC.contains(jwsAlg)) {
            throw new IllegalArgumentException("Unsupported alg in sd-jwt_alg_values[0]: " + firstAlg);
        }
        return jwsAlg;
    }

    private Map<String, Object> fixedDcqlQuery() {
        Map<String, Object> pid = new LinkedHashMap<>();
        pid.put("id", "personal id data");
        pid.put("format", "dc+sd-jwt");

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("vct_values", List.of("https://pidprovider.example.org/v1.0/personidentificationdata"));
        pid.put("meta", meta);

        pid.put("claims", List.of(
                Map.of("path", List.of("given_name")),
                Map.of("path", List.of("family_name")),
                Map.of("path", List.of("personal_administrative_number"))
        ));

        Map<String, Object> wua = new LinkedHashMap<>();
        wua.put("id", "wallet unit attestation");
        wua.put("format", "jwt");
        wua.put("claims", List.of(
                Map.of("path", List.of("iss")),
                Map.of("path", List.of("iat")),
                Map.of("path", List.of("cnf"))
        ));

        Map<String, Object> dcql = new LinkedHashMap<>();
        dcql.put("credentials", List.of(pid, wua));
        return dcql;
    }
}



package com.example.walle.oid4vp.api;

import com.example.walle.oid4vp.model.WalletMetadata;
import com.example.walle.oid4vp.service.Oid4vpRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/request")
public class Oid4vpRequestController {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Oid4vpRequestService requestService;

    public Oid4vpRequestController(ObjectMapper objectMapper, Validator validator, Oid4vpRequestService requestService) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.requestService = requestService;
    }

    @PostMapping(
            path = "/{requestId}",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "application/oauth-authz-req+jwt"
    )
    public ResponseEntity<?> createRequestObject(
            @PathVariable String requestId,
            @RequestParam("wallet_metadata") String walletMetadata, // snake_case HTTP -> camelCase Java
            @RequestParam(value = "wallet_nonce", required = false) String walletNonce
    ) {
        if (requestId == null || requestId.isBlank()) {
            return badRequest("requestId is required");
        }
        if (walletMetadata == null || walletMetadata.isBlank()) {
            return badRequest("wallet_metadata is required");
        }
        if (walletNonce != null && walletNonce.isBlank()) {
            return badRequest("wallet_nonce must not be blank when provided");
        }

        // Parse wallet_metadata JSON
        WalletMetadata parsedMetadata;
        try {
            parsedMetadata = objectMapper.readValue(walletMetadata, WalletMetadata.class);
        } catch (Exception e) {
            return badRequest("wallet_metadata must be valid JSON");
        }

        // Bean Validation (authorizationEndpoint + vpFormatsSupported)
        Set<ConstraintViolation<WalletMetadata>> violations = validator.validate(parsedMetadata);
        if (!violations.isEmpty()) {
            return badRequest(violationsToFields(violations));
        }

        // Jira rule: if present => MUST be vp_token
        List<String> responseTypes = parsedMetadata.getResponseTypesSupported();
        if (responseTypes != null && !responseTypes.isEmpty()) {
            if (!(responseTypes.size() == 1 && "vp_token".equals(responseTypes.get(0)))) {
                return badRequest("wallet_metadata.response_types_supported must be [\"vp_token\"] when provided");
            }
        }

        // Build + sign JWT
        final String jwt;
        try {
            jwt = requestService.createSignedRequestObject(parsedMetadata, walletNonce);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        }

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType("application/oauth-authz-req+jwt"))
                .body(jwt);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "validation_error");
        body.put("message", message);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<Map<String, Object>> badRequest(Map<String, String> fields) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "validation_error");
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    private Map<String, String> violationsToFields(Set<? extends ConstraintViolation<?>> violations) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : violations) {
            fields.put(String.valueOf(v.getPropertyPath()), v.getMessage());
        }
        return fields;
    }
}



