package exiledsector.loot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FleetLootRulesTest {

    private static final float LEVEL_SCALED_SHARE = 0.75f;

    @Test
    void onlyLevelScaledFleetsTakeTheLevelScaledShareAndOwnLedgerFleetsTakeItAll() {
        assertEquals(LEVEL_SCALED_SHARE, FleetLootRules.LEVEL_SCALED.itemShare(LEVEL_SCALED_SHARE), 1e-6f);
        assertEquals(1f, FleetLootRules.ownLedger(LootMultipliers.NONE, 1f).itemShare(LEVEL_SCALED_SHARE), 1e-6f);
        assertEquals(0f, FleetLootRules.FEEDSTOCK_ONLY.itemShare(LEVEL_SCALED_SHARE), 1e-6f);
        assertEquals(0f, FleetLootRules.SPENT.itemShare(LEVEL_SCALED_SHARE), 1e-6f);
    }

    @Test
    void onlyLevelScaledFleetsRollBattleCraftingItems() {
        assertTrue(FleetLootRules.Kind.LEVEL_SCALED.rollsBattleCraftingItems());
        assertFalse(FleetLootRules.Kind.OWN_LEDGER.rollsBattleCraftingItems());
        assertFalse(FleetLootRules.Kind.FEEDSTOCK_ONLY.rollsBattleCraftingItems());
        assertFalse(FleetLootRules.Kind.SPENT.rollsBattleCraftingItems());
    }

    @Test
    void missingPartsFallBackToLevelScaledWithoutMultipliers() {
        FleetLootRules rules = new FleetLootRules(null, null, -2f, false);

        assertSame(FleetLootRules.Kind.LEVEL_SCALED, rules.kind());
        assertSame(LootMultipliers.NONE, rules.multipliers());
        assertEquals(0f, rules.deploymentPointsMult(), 1e-6f);
        assertTrue(rules.overchargingHullItems().overchargesHullItems());
    }
}
