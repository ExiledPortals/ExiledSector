package exiledsector.charts.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasHashTest {

    @Test
    void unitRollsStayStrictlyBetweenZeroAndOne() {
        assertTrue(AtlasHash.unit(0L) > 0.0);
        assertTrue(AtlasHash.unit(-1L) < 1.0);
        assertTrue(AtlasHash.unit(Long.MIN_VALUE) < 1.0);
    }

    @Test
    void exactTiesGoToTheLowerIdWhateverTheOrder() {
        assertTrue(AtlasHash.beats(0.5, "alpha", 0.5, "beta"));
        assertFalse(AtlasHash.beats(0.5, "beta", 0.5, "alpha"));
        assertTrue(AtlasHash.beats(0.4, "beta", 0.5, "alpha"));
        assertTrue(AtlasHash.beats(0.5, "beta", Double.POSITIVE_INFINITY, null));
    }

    @Test
    void idSaltsDependOnlyOnTheId() {
        assertEquals(AtlasHash.idSalt("relay"), AtlasHash.idSalt("relay"));
        assertFalse(AtlasHash.idSalt("relay") == AtlasHash.idSalt("hunter"));
    }
}
