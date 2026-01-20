# Wall-e — Endpoint OpenID4VP `POST /request/{requestId}` (avec validations)

Ce document décrit **uniquement ce qui est demandé dans la Jira** : exposer un endpoint backend qui **génère et retourne une Authorization Request signée (JWT)** pour qu’un wallet puisse présenter une identité (PID).

---

## 1) Ce que la Jira demande (en 1 phrase)

Implémenter **`POST /request/{requestId}`** côté Wall-e, qui :
- reçoit un appel du wallet,
- **valide les champs obligatoires**,
- **génère une Authorization Request OpenID4VP**,
- **la signe en JWT (ES256)**,
- renvoie **le JWT signé** avec le bon `Content-Type`.

> Le callback `/response_uri` est **préparé** via le champ `response_uri` dans le JWT, mais **le traitement de la réponse (vp_token)** est généralement hors scope de cette Jira (sauf si spécifié ailleurs).

---

## 2) Ton endpoint à implémenter

### ✅ Endpoint
**`POST /request/{requestId}`**  
- `Content-Type` (entrée) : `application/x-www-form-urlencoded`  
- `Content-Type` (sortie) : `application/oauth-authz-req+jwt`

### ✅ Qui appelle ?
➡️ **Le wallet**.

### ✅ Ce que tu reçois
- Path : `requestId`
- Body form-urlencoded : champs envoyés par le wallet (métadonnées + capacités).

### ✅ Champs minimum validés (obligatoires)
- `vp_formats_supported` : **obligatoire** (non vide)
- `authorization_endpoint` : **obligatoire** (URL http/https)

### Champs optionnels
- `wallet_nonce` : recommandé (si présent, doit être non vide)
- `wallet_metadata`, `response_types_supported`, `response_modes_supported`, `alg_values_supported`, etc.

### ✅ Traitement à faire
1. Générer `state` (unique)
2. Générer `nonce` (fort, >= 32 caractères recommandé)
3. Construire le **Request Object** (payload OpenID4VP), incluant :
   - `client_id`
   - `response_type = vp_token`
   - `response_mode = direct_post_jwt`
   - `dcql_query` (PID demandée + WUA si besoin)
   - `response_uri` (ton callback Wall-e)
   - `state`, `nonce`, et `wallet_nonce` si fourni
   - `iat`, `exp`
   - `request_uri_method = post`
4. Signer en **JWT ES256**
5. (Optionnel mais conseillé) **stocker** `requestId ↔ state ↔ nonce ↔ wallet_nonce` pour valider plus tard.

### ✅ Ce que tu renvoies
- **HTTP 200**
- `Content-Type: application/oauth-authz-req+jwt`
- Body = **le JWT signé** (string)

### Erreurs attendues
- **400** si champs obligatoires manquants / invalides

---

## 3) Implémentation Spring Boot (copier-coller)

### 3.1 Dépendances Maven

```xml
<!-- pom.xml -->
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>

  <!-- Nimbus JOSE + JWT -->
  <dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
    <version>9.40</version>
  </dependency>
</dependencies>
```

---

### 3.2 DTO form-urlencoded + validations

```java
package com.example.walle.oid4vp.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class WalletRequestForm {

    @NotBlank(message = "vp_formats_supported is required")
    private String vp_formats_supported;

    @NotBlank(message = "authorization_endpoint is required")
    @Pattern(
        regexp = "https?://.+",
        message = "authorization_endpoint must be a valid http(s) URL"
    )
    private String authorization_endpoint;

    // OPTIONAL
    private String wallet_metadata;
    private String response_types_supported;
    private String response_modes_supported;
    private String alg_values_supported;

    // RECOMMENDED (optionnel). Si présent -> controller vérifie non vide.
    private String wallet_nonce;

    public String getVp_formats_supported() { return vp_formats_supported; }
    public void setVp_formats_supported(String v) { this.vp_formats_supported = v; }

    public String getAuthorization_endpoint() { return authorization_endpoint; }
    public void setAuthorization_endpoint(String v) { this.authorization_endpoint = v; }

    public String getWallet_metadata() { return wallet_metadata; }
    public void setWallet_metadata(String v) { this.wallet_metadata = v; }

    public String getResponse_types_supported() { return response_types_supported; }
    public void setResponse_types_supported(String v) { this.response_types_supported = v; }

    public String getResponse_modes_supported() { return response_modes_supported; }
    public void setResponse_modes_supported(String v) { this.response_modes_supported = v; }

    public String getAlg_values_supported() { return alg_values_supported; }
    public void setAlg_values_supported(String v) { this.alg_values_supported = v; }

    public String getWallet_nonce() { return wallet_nonce; }
    public void setWallet_nonce(String v) { this.wallet_nonce = v; }
}
```

---

### 3.3 Controller `POST /request/{requestId}`

```java
package com.example.walle.oid4vp.api;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/request")
public class Oid4vpRequestController {

    private final Oid4vpRequestService service;

    public Oid4vpRequestController(Oid4vpRequestService service) {
        this.service = service;
    }

    @PostMapping(
        path = "/{requestId}",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = "application/oauth-authz-req+jwt"
    )
    public ResponseEntity<String> createSignedRequestObject(
            @PathVariable("requestId") String requestId,
            @Valid WalletRequestForm form
    ) {
        // Validation minimale requestId
        if (requestId == null || requestId.isBlank()) {
            return ResponseEntity.badRequest().body("requestId is required");
        }

        // wallet_nonce = optionnel mais si présent -> non vide
        if (form.getWallet_nonce() != null && form.getWallet_nonce().isBlank()) {
            return ResponseEntity.badRequest().body("wallet_nonce must not be blank when provided");
        }

        String jwt = service.buildAndSignRequestObject(requestId, form);

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType("application/oauth-authz-req+jwt"))
                .body(jwt);
    }
}
```

---

### 3.4 Service : construction + signature ES256

> ⚠️ La partie clé privée (`ECPrivateKey signingKey`) est à brancher sur votre KMS/keystore/PEM.
> Ici on suppose que Spring injecte un `ECPrivateKey`.

```java
package com.example.walle.oid4vp.api;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.*;

@Service
public class Oid4vpRequestService {

    private final ECPrivateKey signingKey;

    // à configurer
    private final String keyId = "your-kid-here";
    private final String clientId = "https://relying-party.example.org";
    private final String responseUri = "https://relying-party.example.org/response_uri";

    public Oid4vpRequestService(ECPrivateKey signingKey) {
        this.signingKey = signingKey;
    }

    public String buildAndSignRequestObject(String requestId, WalletRequestForm form) {
        String state = UUID.randomUUID().toString();

        // nonce (simple). À remplacer par un générateur crypto-safe si besoin.
        String nonce = UUID.randomUUID().toString().replace("-", "") +
                       UUID.randomUUID().toString().replace("-", "");

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(60 * 10); // ex: 10 minutes

        Map<String, Object> dcqlQuery = buildDcqlQueryExample();

        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .claim("client_id", clientId)
                .claim("response_mode", "direct_post_jwt")
                .claim("response_type", "vp_token")
                .claim("dcql_query", dcqlQuery)
                .claim("response_uri", responseUri)
                .claim("nonce", nonce)
                .claim("state", state)
                .claim("iss", clientId)
                .claim("iat", now.getEpochSecond())
                .claim("exp", exp.getEpochSecond())
                .claim("request_uri_method", "post");

        // wallet_nonce : inclure seulement si fourni
        if (form.getWallet_nonce() != null && !form.getWallet_nonce().isBlank()) {
            claims.claim("wallet_nonce", form.getWallet_nonce());
        }

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(new JOSEObjectType("oauth-authz-req+jwt"))
                .keyID(keyId)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claims.build());

        try {
            JWSSigner signer = new ECDSASigner(signingKey);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign request object JWT", e);
        }
    }

    private Map<String, Object> buildDcqlQueryExample() {
        Map<String, Object> pidCredential = new LinkedHashMap<>();
        pidCredential.put("id", "personal id data");
        pidCredential.put("format", "dc+sd-jwt");

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("vct_values", List.of("https://pidprovider.example.org/v1.0/personidentificationdata"));
        pidCredential.put("meta", meta);

        pidCredential.put("claims", List.of(
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
        dcql.put("credentials", List.of(pidCredential, wua));

        return dcql;
    }
}
```

---

### 3.5 Gestion propre des erreurs de validation (400)

```java
package com.example.walle.oid4vp.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "validation_error");

        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                fields.put(err.getField(), err.getDefaultMessage())
        );
        body.put("fields", fields);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "server_error");
        body.put("message", ex.getMessage());
        return ResponseEntity.internalServerError().body(body);
    }
}
```

---

## 4) Exemple de test rapide (curl)

```bash
curl -X POST "http://localhost:8080/request/REQ-123"   -H "Content-Type: application/x-www-form-urlencoded"   --data-urlencode "vp_formats_supported={...}"   --data-urlencode "authorization_endpoint=https://wallet.example.org/authorize"   --data-urlencode "wallet_nonce=abc123"
```

Réponse attendue :
- `200 OK`
- `Content-Type: application/oauth-authz-req+jwt`
- Body : `eyJ...` (JWT signé)

---

## 5) Notes importantes (pratiques)

- **Clé ES256** : il faut une clé EC P-256 côté serveur (private key) + `kid`.
- **Nonce** : idéalement généré via un RNG crypto-safe (ex: `SecureRandom`).
- **Persistance** : recommandé de stocker `state/nonce/wallet_nonce` liés à `requestId` pour le callback futur (même si hors scope).

---
