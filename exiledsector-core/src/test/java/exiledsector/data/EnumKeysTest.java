package exiledsector.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnumKeysTest {

    private enum Side { DEFENDER, ATTACKER, NEITHER }

    @Test
    void keysAreLowerCaseNames() {
        assertEquals("attacker", EnumKeys.key(Side.ATTACKER));
    }

    @Test
    void parsingIgnoresCaseAndSurroundingSpaceAndMissesToNull() {
        assertEquals(Side.DEFENDER, EnumKeys.parse(Side.class, " defender "));
        assertEquals(Side.NEITHER, EnumKeys.parse(Side.class, "NEITHER"));
        assertNull(EnumKeys.parse(Side.class, "both"));
        assertNull(EnumKeys.parse(Side.class, ""));
        assertNull(EnumKeys.parse(Side.class, null));
    }

    @Test
    void aFallbackCoversBlankAndUnknownText() {
        assertEquals(Side.NEITHER, EnumKeys.parseOr(Side.class, "", Side.NEITHER));
        assertEquals(Side.NEITHER, EnumKeys.parseOr(Side.class, null, Side.NEITHER));
        assertEquals(Side.ATTACKER, EnumKeys.parseOr(Side.class, "Attacker", Side.NEITHER));
    }
}
