package exiledsector.skills.skilleffect.phase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhaseVoidUpkeepListenerTest {

    @Test
    void theCoveredFluxIsTheUpkeepTheShipNoLongerGenerates() {
        assertEquals(50f, PhaseVoidUpkeepListener.coveredFlux(50f, 50f), 0.001f);
        assertEquals(25f, PhaseVoidUpkeepListener.coveredFlux(75f, 25f), 0.001f);
    }

    @Test
    void nothingIsCoveredWithoutAShare() {
        assertEquals(0f, PhaseVoidUpkeepListener.coveredFlux(50f, 0f));
        assertEquals(0f, PhaseVoidUpkeepListener.coveredFlux(50f, 100f));
    }
}
