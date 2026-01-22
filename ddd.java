package com.example.walle.oid4vp.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WalletMetadataTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void jsonMapping_snakeCase_to_camelCase_works() throws Exception {
        String json = """
            {
              "authorization_endpoint": "https://wallet.europe.eu/authorization",
              "response_types_supported": ["vp_token"],
              "vp_formats_supported": {"dc+sd-jwt": {"sd-jwt_alg_values": ["ES256"]}}
            }
            """;

        WalletMetadata wm = objectMapper.readValue(json, WalletMetadata.class);

        assertEquals("https://wallet.europe.eu/authorization", wm.getAuthorizationEndpoint());
        assertEquals(List.of("vp_token"), wm.getResponseTypesSupported());
        assertNotNull(wm.getVpFormatsSupported());
        assertTrue(wm.getVpFormatsSupported().containsKey("dc+sd-jwt"));
    }

    @Test
    void validation_fails_when_required_fields_missing() {
        WalletMetadata wm = new WalletMetadata();
        // missing authorizationEndpoint + vpFormatsSupported

        var violations = validator.validate(wm);
        assertFalse(violations.isEmpty());

        // Ensure both messages exist
        String all = violations.toString();
        assertTrue(all.contains("authorization_endpoint") || all.contains("authorizationEndpoint"));
        assertTrue(all.contains("vp_formats_supported") || all.contains("vpFormatsSupported"));
    }

    @Test
    void getters_setters_cover() {
        WalletMetadata wm = new WalletMetadata();
        wm.setAuthorizationEndpoint("https://x");
        wm.setVpFormatsSupported(Map.of("dc+sd-jwt", Map.of("sd-jwt_alg_values", List.of("ES256"))));
        wm.setResponseTypesSupported(List.of("vp_token"));
        wm.setResponseModesSupported(List.of("form_post.jwt"));
        wm.setAlgValuesSupported(List.of("ES256"));

        assertEquals("https://x", wm.getAuthorizationEndpoint());
        assertNotNull(wm.getVpFormatsSupported());
        assertEquals(List.of("vp_token"), wm.getResponseTypesSupported());
        assertEquals(List.of("form_post.jwt"), wm.getResponseModesSupported());
        assertEquals(List.of("ES256"), wm.getAlgValuesSupported());
    }
}

package com.example.walle.oid4vp.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Oid4vpPropertiesTest {

    @Test
    void getters_setters_cover() {
        Oid4vpProperties p = new Oid4vpProperties();

        p.setClientId("client");
        p.setResponseMode("direct_post_jwt");
        p.setResponseType("vp_token");
        p.setResponseUri("https://rp/response");
        p.setKid("kid");
        p.setTrustChain(List.of("a", "b"));
        p.setTokenTtlSeconds(123);
        p.setEcPrivateKeyB64("base64");

        assertEquals("client", p.getClientId());
        assertEquals("direct_post_jwt", p.getResponseMode());
        assertEquals("vp_token", p.getResponseType());
        assertEquals("https://rp/response", p.getResponseUri());
        assertEquals("kid", p.getKid());
        assertEquals(List.of("a", "b"), p.getTrustChain());
        assertEquals(123, p.getTokenTtlSeconds());
        assertEquals("base64", p.getEcPrivateKeyB64());
    }
}

package com.example.walle.oid4vp.crypto;

import com.example.walle.oid4vp.config.Oid4vpProperties;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SigningKeyProviderTest {

    @Test
    void loadsEcPrivateKeyFromBase64Pkcs8() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256); // P-256
        KeyPair kp = kpg.generateKeyPair();

        byte[] pkcs8 = kp.getPrivate().getEncoded();
        String b64 = Base64.getEncoder().encodeToString(pkcs8);

        Oid4vpProperties props = new Oid4vpProperties();
        props.setEcPrivateKeyB64(b64);

        SigningKeyProvider provider = new SigningKeyProvider(props);
        ECPrivateKey key = provider.getEcPrivateKey();

        assertNotNull(key);
        assertArrayEquals(pkcs8, key.getEncoded());
    }

    @Test
    void throwsWhenMissingKey() {
        Oid4vpProperties props = new Oid4vpProperties();
        props.setEcPrivateKeyB64("   ");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new SigningKeyProvider(props));
        assertTrue(ex.getMessage().contains("Missing"));
    }

    @Test
    void throwsWhenInvalidBase64() {
        Oid4vpProperties props = new Oid4vpProperties();
        props.setEcPrivateKeyB64("not-base64");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new SigningKeyProvider(props));
        assertTrue(ex.getMessage().contains("Failed to load"));
    }
}


package com.example.walle.oid4vp.service;

import com.example.walle.oid4vp.config.Oid4vpProperties;
import com.example.walle.oid4vp.crypto.SigningKeyProvider;
import com.example.walle.oid4vp.model.WalletMetadata;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Oid4vpRequestServiceTest {

    private static KeyPair generateEcP256() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        return kpg.generateKeyPair();
    }

    private static Oid4vpProperties baseProps(String b64Pk) {
        Oid4vpProperties p = new Oid4vpProperties();
        p.setClientId("https://relying-party.example.org");
        p.setResponseMode("direct_post_jwt");
        p.setResponseType("vp_token");
        p.setResponseUri("https://relying-party.example.org/response_uri");
        p.setKid("9tjcAivhLMVUJ3AXWGZ_g");
        p.setTrustChain(List.of("a", "b", "c"));
        p.setTokenTtlSeconds(3600);
        p.setEcPrivateKeyB64(b64Pk);
        return p;
    }

    private static WalletMetadata walletMetadataWithAlgFirst(String alg) {
        WalletMetadata wm = new WalletMetadata();
        wm.setAuthorizationEndpoint("https://wallet.europe.eu/authorization");
        wm.setVpFormatsSupported(Map.of(
            "dc+sd-jwt", Map.of("sd-jwt_alg_values", List.of(alg, "ES384"))
        ));
        wm.setResponseTypesSupported(List.of("vp_token"));
        return wm;
    }

    @Test
    void createsSignedJwt_withHeaderAndPayload_andVerifiesSignature() throws Exception {
        KeyPair kp = generateEcP256();
        String b64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());

        Oid4vpProperties props = baseProps(b64);
        SigningKeyProvider skp = new SigningKeyProvider(props);

        Oid4vpRequestService svc = new Oid4vpRequestService(props, skp);

        WalletMetadata wm = walletMetadataWithAlgFirst("ES256");
        String jwtString = svc.createSignedRequestObject(wm, "walletNonce123");

        SignedJWT jwt = SignedJWT.parse(jwtString);

        // header
        assertEquals(JWSAlgorithm.ES256, jwt.getHeader().getAlgorithm());
        assertEquals(new JOSEObjectType("oauth-authz-req+jwt"), jwt.getHeader().getType());
        assertEquals("9tjcAivhLMVUJ3AXWGZ_g", jwt.getHeader().getKeyID());
        assertEquals(List.of("a", "b", "c"), jwt.getHeader().getCustomParam("trust_chain"));

        // payload essential
        var claims = jwt.getJWTClaimsSet();
        assertEquals(props.getClientId(), claims.getStringClaim("client_id"));
        assertEquals(props.getResponseMode(), claims.getStringClaim("response_mode"));
        assertEquals(props.getResponseType(), claims.getStringClaim("response_type"));
        assertEquals(props.getResponseUri(), claims.getStringClaim("response_uri"));

        assertEquals(props.getClientId(), claims.getStringClaim("iss"));
        assertEquals("post", claims.getStringClaim("request_uri_method"));
        assertEquals("walletNonce123", claims.getStringClaim("wallet_nonce"));

        assertNotNull(claims.getStringClaim("nonce"));
        assertNotNull(claims.getStringClaim("state"));
        assertTrue(claims.getLongClaim("iat") > 0);
        assertTrue(claims.getLongClaim("exp") > claims.getLongClaim("iat"));

        // dcql_query exists and has expected top-level keys
        @SuppressWarnings("unchecked")
        Map<String, Object> dcql = (Map<String, Object>) claims.getClaim("dcql_query");
        assertNotNull(dcql);
        assertTrue(dcql.containsKey("credentials"));

        // verify signature
        ECPublicKey pub = (ECPublicKey) kp.getPublic();
        assertTrue(jwt.verify(new ECDSAVerifier(pub)));
    }

    @Test
    void walletNonce_isOmitted_whenNullOrBlank() throws Exception {
        KeyPair kp = generateEcP256();
        String b64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        Oid4vpProperties props = baseProps(b64);
        SigningKeyProvider skp = new SigningKeyProvider(props);

        Oid4vpRequestService svc = new Oid4vpRequestService(props, skp);

        WalletMetadata wm = walletMetadataWithAlgFirst("ES256");
        String jwtString = svc.createSignedRequestObject(wm, null);

        SignedJWT jwt = SignedJWT.parse(jwtString);
        assertNull(jwt.getJWTClaimsSet().getClaim("wallet_nonce"));
    }

    @Test
    void throws_whenDcSdJwtMissing() throws Exception {
        KeyPair kp = generateEcP256();
        String b64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        Oid4vpProperties props = baseProps(b64);
        SigningKeyProvider skp = new SigningKeyProvider(props);

        Oid4vpRequestService svc = new Oid4vpRequestService(props, skp);

        WalletMetadata wm = new WalletMetadata();
        wm.setAuthorizationEndpoint("https://wallet.europe.eu/authorization");
        wm.setVpFormatsSupported(Map.of("jwt", Map.of()));
        wm.setResponseTypesSupported(List.of("vp_token"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> svc.createSignedRequestObject(wm, "x"));
        assertTrue(ex.getMessage().contains("dc+sd-jwt"));
    }

    @Test
    void throws_whenAlgValuesMissingOrEmpty() throws Exception {
        KeyPair kp = generateEcP256();
        String b64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        Oid4vpProperties props = baseProps(b64);
        SigningKeyProvider skp = new SigningKeyProvider(props);

        Oid4vpRequestService svc = new Oid4vpRequestService(props, skp);

        WalletMetadata wm = new WalletMetadata();
        wm.setAuthorizationEndpoint("https://wallet.europe.eu/authorization");
        wm.setVpFormatsSupported(Map.of("dc+sd-jwt", Map.of("sd-jwt_alg_values", List.of())));
        wm.setResponseTypesSupported(List.of("vp_token"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> svc.createSignedRequestObject(wm, "x"));
        assertTrue(ex.getMessage().contains("sd-jwt_alg_values"));
    }

    @Test
    void throws_whenAlgNotECFamily() throws Exception {
        KeyPair kp = generateEcP256();
        String b64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        Oid4vpProperties props = baseProps(b64);
        SigningKeyProvider skp = new SigningKeyProvider(props);

        Oid4vpRequestService svc = new Oid4vpRequestService(props, skp);

        WalletMetadata wm = walletMetadataWithAlgFirst("HS256"); // not EC family

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> svc.createSignedRequestObject(wm, "x"));
        assertTrue(ex.getMessage().contains("Unsupported alg"));
    }
}


package com.example.walle.oid4vp.api;

import com.example.walle.oid4vp.model.WalletMetadata;
import com.example.walle.oid4vp.service.Oid4vpRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(Oid4vpRequestController.class)
class Oid4vpRequestControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean Oid4vpRequestService requestService;

    @Test
    void returns400_whenRequestIdBlank() throws Exception {
        mockMvc.perform(post("/request/ ")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("wallet_metadata", "{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void returns400_whenWalletMetadataMissing() throws Exception {
        mockMvc.perform(post("/request/123")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("wallet_metadata is required"));
    }

    @Test
    void returns400_whenWalletNonceBlank() throws Exception {
        mockMvc.perform(post("/request/123")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("wallet_metadata", "{}")
                .param("wallet_nonce", "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("wallet_nonce must not be blank when provided"));
    }

    @Test
    void returns400_whenWalletMetadataInvalidJson() throws Exception {
        mockMvc.perform(post("/request/123")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("wallet_metadata", "{not-json}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("wallet_metadata must be valid JSON"));
    }

    @Test
    void returns400_whenValidationFails_missingRequiredFieldsInsideWalletMetadata() throws Exception {
        // valid JSON but missing required authorization_endpoint and vp_formats_supported
        String json = "{}";

        mockMvc.perform(post("/request/123")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("wallet_metadata", json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"))
            .andExpect(jsonPath("$.fields").exists());
    }

    @Test
    void returns400_whenResponseTypesSupportedInvalid() throws Exception {
        String json = """
            {
              "authorization_endpoint": "https://wallet.europe.eu/authorization",
              "response_types_supported": ["code"],
              "vp_formats_supported": {"dc+sd-jwt":{"sd-jwt_alg_values":["ES256"]}}
            }
            """;

        mockMvc.perform(post("/request/123")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("wallet_metadata", json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("wallet_metadata.response_types_supported must be [\"vp_token\"] when provided"));
    }

    @Test
    void returns400_whenServiceThrowsIllegalArgumentException() throws Exception {
        String json = """
            {
              "authorization_endpoint": "https://wallet.europe.eu/authorization",
              "response_types_supported": ["vp_token"],
              "vp_formats_supported": {"dc+sd-jwt":{"sd-jwt_alg_values":["ES256"]}}
            }
            """;

        when(requestService.createSignedRequestObject(any(WalletMetadata.class), any()))
            .thenThrow(new IllegalArgumentException("bad alg"));

        mockMvc.perform(post("/request/123")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("wallet_metadata", json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("bad alg"));
    }

    @Test
    void returns200_withJwt_andCorrectContentType() throws Exception {
        String json = """
            {
              "authorization_endpoint": "https://wallet.europe.eu/authorization",
              "response_types_supported": ["vp_token"],
              "vp_formats_supported": {"dc+sd-jwt":{"sd-jwt_alg_values":["ES256"]}}
            }
            """;

        when(requestService.createSignedRequestObject(any(WalletMetadata.class), any()))
            .thenReturn("signed.jwt.here");

        mockMvc.perform(post("/request/123")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.parseMediaType("application/oauth-authz-req+jwt"))
                .param("wallet_metadata", json)
                .param("wallet_nonce", "abc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/oauth-authz-req+jwt"))
            .andExpect(content().string("signed.jwt.here"));

        verify(requestService, times(1))
            .createSignedRequestObject(any(WalletMetadata.class), eq("abc"));
    }
}



