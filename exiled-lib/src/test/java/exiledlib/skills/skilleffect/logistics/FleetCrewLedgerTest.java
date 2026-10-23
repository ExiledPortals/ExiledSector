package exiledlib.skills.skilleffect.logistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FleetCrewLedgerTest {

    @Test
    void roundingUpIgnoresFloatingPointNoise() {
        assertEquals(0, FleetCrewLedger.roundUp(0f));
        assertEquals(1, FleetCrewLedger.roundUp(0.2f));
        assertEquals(5, FleetCrewLedger.roundUp(5.00001f));
        assertEquals(6, FleetCrewLedger.roundUp(5.1f));
    }
}
