package exiledsector.effects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcBonusScaleTest {

    private static final float TOLERANCE = 0.0001f;
    private static final NpcBonusScale.Config DEFAULTS = new NpcBonusScale.Config(15, 60, 3f);

    @Test
    void theMultiplierRisesInAStraightLineBetweenTheLowAndHighTreePower() {
        assertEquals(1f, NpcBonusScale.multiplier(10f, DEFAULTS), TOLERANCE);
        assertEquals(1f, NpcBonusScale.multiplier(15f, DEFAULTS), TOLERANCE);
        assertEquals(2f, NpcBonusScale.multiplier(37.5f, DEFAULTS), TOLERANCE);
        assertEquals(3f, NpcBonusScale.multiplier(60f, DEFAULTS), TOLERANCE);
        assertEquals(3f, NpcBonusScale.multiplier(80f, DEFAULTS), TOLERANCE);
    }

    @Test
    void theFleetsLevelCapStopsTheMultiplierFromGrowing() {
        assertEquals(1f, NpcBonusScale.cappedMultiplier(60f, 15, DEFAULTS), TOLERANCE);
        assertEquals(1.44f, NpcBonusScale.cappedMultiplier(60f, 25, DEFAULTS), TOLERANCE);
        assertEquals(1.44f, NpcBonusScale.cappedMultiplier(25f, 25, DEFAULTS), TOLERANCE);
        assertEquals(1.22f, NpcBonusScale.cappedMultiplier(20f, 25, DEFAULTS), TOLERANCE);
        assertEquals(3f, NpcBonusScale.cappedMultiplier(80f, Integer.MAX_VALUE, DEFAULTS), TOLERANCE);
    }

    @Test
    void aMaximumOfOneOrAnEmptyPowerSpanBehavesSafely() {
        assertEquals(1f, NpcBonusScale.multiplier(60f, new NpcBonusScale.Config(15, 60, 1f)), TOLERANCE);
        assertEquals(1f, NpcBonusScale.multiplier(29f, new NpcBonusScale.Config(30, 30, 3f)), TOLERANCE);
        assertEquals(3f, NpcBonusScale.multiplier(30f, new NpcBonusScale.Config(30, 30, 3f)), TOLERANCE);
    }
}
