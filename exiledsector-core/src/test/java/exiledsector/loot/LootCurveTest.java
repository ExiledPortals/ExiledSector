package exiledsector.loot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootCurveTest {

    @Test
    void aLinearCurveKeepsEveryDeploymentPoint() {
        assertEquals(10000f, LootCurve.LINEAR.apply(10000f), 1e-3f);
        assertEquals(1f, LootCurve.LINEAR.factor(10000f), 1e-6f);
        assertEquals(0f, LootCurve.LINEAR.apply(-5f));
    }

    @Test
    void aDiminishingCurveRunsAtFullRateToTheKneeThenGrowsWithTheSquareRoot() {
        float knee = LootCurve.KNEE_DEPLOYMENT_POINTS;

        assertEquals(knee, LootCurve.DIMINISHING.apply(knee), 1e-3f);
        assertEquals(1f, LootCurve.DIMINISHING.factor(knee / 2f), 1e-6f);
        assertEquals(3f * knee, LootCurve.DIMINISHING.apply(4f * knee), 1e-2f);
        assertEquals(0.75f, LootCurve.DIMINISHING.factor(4f * knee), 1e-5f);
        assertTrue(LootCurve.DIMINISHING.apply(knee + 1f) > LootCurve.DIMINISHING.apply(knee));
        assertEquals(0f, LootCurve.DIMINISHING.factor(0f));
    }
}
