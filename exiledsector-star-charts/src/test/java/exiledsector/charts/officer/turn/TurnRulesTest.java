package exiledsector.charts.officer.turn;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TurnRulesTest {

    private static final TurnRules RULES = OfficerTurnTables.DEFAULTS.withIntentWeights(Map.of(
            "admiral", new IntentWeights(2f, 4f, 3f, 2f, 1f),
            "reckless", new IntentWeights(1f, 2f, 3f, 0.25f, 0.25f)));

    @Test
    void equalRanksWinHalfTheTimeAndEachRankBetweenThemShiftsTheChance() {
        assertEquals(50f, RULES.attackerChancePercent(2, 2, false));
        assertEquals(75f, RULES.attackerChancePercent(4, 0, false));
        assertEquals(25f, RULES.attackerChancePercent(0, 4, false));
    }

    @Test
    void aDefenceLowersTheAttackersChanceWhichStaysBetweenNoneAndCertain() {
        assertEquals(40f, RULES.attackerChancePercent(2, 2, true));
        TurnRules lopsided = new TurnRules(4, 6f, 95f, 50f, 10f, 1, 3, 2, 4, 50, 6, Map.of());
        assertEquals(100f, lopsided.attackerChancePercent(4, 0, false));
        assertEquals(0f, lopsided.attackerChancePercent(0, 4, true));
    }

    @Test
    void rankAndTraitWeightsMultiplyAndAnUnknownIdWeighsOne() {
        assertEquals(9f, RULES.intentWeight("admiral", "reckless", IntentKind.CHALLENGE));
        assertEquals(4f, RULES.intentWeight("admiral", "unknown", IntentKind.PUSH));
        assertEquals(1f, RULES.intentWeight(null, null, IntentKind.HOLD));
    }

    @Test
    void largerTerritoriesExpandLessOften() {
        assertEquals(2f, RULES.expandWeight("admiral", "steady", 0));
        assertEquals(1f, RULES.expandWeight("admiral", "steady", 6));
    }

    @Test
    void lossesHalveRenownAndHoldingsEarnPerFourSystems() {
        assertEquals(3, RULES.renownAfterLoss(7));
        assertEquals(0, RULES.renownAfterLoss(0));
        assertEquals(2, RULES.holdingsRenown(9));
        assertEquals(0, new TurnRules(4, 6f, 50f, 6.25f, 10f, 1, 3, 2, 0, 50, 6, Map.of()).holdingsRenown(9));
    }
}
