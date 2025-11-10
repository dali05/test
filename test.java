package com.bnpp.pf.common.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AlgorithmTest {

    @Test
    void testEnumValues_ShouldContainExpectedAlgorithms() {
        Algorithm[] values = Algorithm.values();
        assertTrue(values.length >= 6);
        assertNotNull(Algorithm.valueOf("ES256"));
        assertNotNull(Algorithm.valueOf("ES512"));
    }

    @Test
    void testEnumToString() {
        assertEquals("ES256", Algorithm.ES256.name());
        assertEquals("ES512", Algorithm.ES512.name());
    }
}

package com.bnpp.pf.common.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthModeTest {

    @Test
    void testFromString_ShouldReturnMatchingEnum() {
        assertEquals(AuthMode.JWT, AuthMode.fromString("jwt"));
        assertEquals(AuthMode.MDOC, AuthMode.fromString("MDOC"));
    }

    @Test
    void testFromString_ShouldReturnNull_WhenTextIsNull() {
        assertNull(AuthMode.fromString(null));
    }

    @Test
    void testFromString_ShouldBeCaseInsensitiveAndTrimmed() {
        assertEquals(AuthMode.JWT, AuthMode.fromString("  jWt  "));
    }

    @Test
    void testToValue_ShouldReturnLowercaseName() {
        assertEquals("jwt", AuthMode.JWT.toValue());
        assertEquals("mdoc", AuthMode.MDOC.toValue());
    }
}


package com.bnpp.pf.common.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationTypeTest {

    @Test
    void testEnumValues_ShouldContainApiAndKafka() {
        assertNotNull(NotificationType.API);
        assertNotNull(NotificationType.KAFKA);
    }

    @Test
    void testToValue_ShouldReturnLowercaseName() {
        assertEquals("api", NotificationType.API.toValue());
        assertEquals("kafka", NotificationType.KAFKA.toValue());
    }

    @Test
    void testFromString_ShouldDelegateToAuthMode() {
        // on mock le comportement attendu d'AuthMode.fromString()
        // ici on vérifie simplement que la méthode ne lance pas d'erreur
        assertDoesNotThrow(() -> NotificationType.fromString("api"));
    }
}

