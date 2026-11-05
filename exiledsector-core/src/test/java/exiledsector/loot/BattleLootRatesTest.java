package exiledsector.loot;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BattleLootRatesTest {

    @AfterEach
    void tearDown() {
        BattleLootRates.clear();
    }

    private static JSONObject row(String drop, String rate, String curve, String regularShare) throws Exception {
        return new JSONObject().put("drop", drop).put("per_100_dp", rate).put("curve", curve).put("regular_share", regularShare);
    }

    @Test
    void theTableReadsEachRowAndSkipsUnknownCurves() throws Exception {
        BattleLootRates.register(new JSONArray()
                .put(row("swarm", "0.3", "linear", "0.5"))
                .put(row("relic", "0.01", "Diminishing", "1"))
                .put(row("odd", "1", "exponential", "1"))
                .put(row("negative", "-2", "", "")));

        assertEquals(new BattleLootRates.Rate("swarm", 0.3f, LootCurve.LINEAR, 0.5f), BattleLootRates.rate("swarm"));
        assertEquals(LootCurve.DIMINISHING, BattleLootRates.rate("relic").curve());
        assertNull(BattleLootRates.rate("odd"));
        assertEquals(new BattleLootRates.Rate("negative", 0f, LootCurve.LINEAR, 0f), BattleLootRates.rate("negative"));
    }

    @Test
    void aRateExpectsItsShareOfTheLootDeploymentPointsCurvedByTheWholeBattle() {
        BattleLootRates.Rate linear = new BattleLootRates.Rate("swarm", 0.3f, LootCurve.LINEAR, 1f);
        BattleLootRates.Rate diminishing = new BattleLootRates.Rate("relic", 1f, LootCurve.DIMINISHING, 1f);

        assertEquals(3f, linear.expected(1000f, 1000f), 1e-4f);
        assertEquals(30f, diminishing.expected(4000f, 4f * LootCurve.KNEE_DEPLOYMENT_POINTS), 1e-3f);
        assertEquals(20f, diminishing.expected(2000f, 2000f), 1e-3f);
    }

    @Test
    void difficultyCountsFromOneUpToThreeTimes() {
        assertEquals(1f, BattleLootRates.difficulty(0.5f));
        assertEquals(2f, BattleLootRates.difficulty(2f));
        assertEquals(BattleLootRates.DIFFICULTY_CAP, BattleLootRates.difficulty(6f));
        assertEquals(1f, BattleLootRates.difficulty(Float.NaN));
    }

    @Test
    void eachRowScalesBattleDifficultyByItsOwnDial() throws Exception {
        BattleLootRates.register(new JSONArray()
                .put(row("swarm", "1", "linear", "1").put("difficulty_scaling", "0"))
                .put(row("relic", "1", "linear", "1").put("difficulty_scaling", "0.5"))
                .put(row("hull", "1", "linear", "1")));

        assertEquals(1f, BattleLootRates.rate("swarm").difficulty(3f), 1e-4f);
        assertEquals(1.5f, BattleLootRates.rate("relic").difficulty(2f), 1e-4f);
        assertEquals(2f, BattleLootRates.rate("hull").difficulty(2f), 1e-4f);
        assertEquals(BattleLootRates.DIFFICULTY_CAP, BattleLootRates.rate("hull").difficulty(6f), 1e-4f);
    }
}
