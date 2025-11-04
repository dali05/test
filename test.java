
package com.bnpp.pf.walle.access.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // On injecte une clé secrète factice via Reflection
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "MySuperSecretKeyForJwt12345");
    }

    @Test
    void testGenerateJwt_Success() throws Exception {
        String token = jwtUtil.generateJwt();

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "Le JWT doit avoir 3 parties");

        // Vérifie qu'il peut être parsé et signé correctement
        SignedJWT signedJWT = SignedJWT.parse(token);
        assertEquals("wall-e", signedJWT.getJWTClaimsSet().getSubject());
        assertNotNull(signedJWT.getJWTClaimsSet().getExpirationTime());
    }

    @Test
    void testGenerateJwt_FailsWhenSecretIsInvalid() {
        JwtUtil invalidJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(invalidJwtUtil, "jwtSecret", "123"); // secret trop court pour HMAC-SHA256

        assertThrows(JOSEException.class, invalidJwtUtil::generateJwt);
    }
}
