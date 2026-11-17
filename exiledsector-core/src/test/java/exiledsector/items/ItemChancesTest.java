package exiledsector.items;

import exiledsector.loot.DropType;
import exiledsector.skills.skilleffect.logistics.FoundItemQuantity;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ItemChancesTest {

    private static final float EPSILON = 1e-5f;

    @Test
    void aNodesPercentReachesItselfAndEveryDropTypeBelowItAndNothingElse() {
        for (DropType node : DropType.values()) {
            ItemChances chances = ItemChances.increasedBy(node, 100f);
            for (DropType type : DropType.values()) {
                float expected = type.chain().contains(node) ? 2f : 1f;
                assertEquals(expected, chances.quantity(type), EPSILON, node + " on " + type);
            }
        }
    }

    @Test
    void eachDropTypeSumsItsWholeChainIntoOnePercent() {
        ItemChances chances = new ItemChances(Map.of(DropType.ITEM, 10f, DropType.HULL_ITEM, 20f, DropType.REACTOR, 30f,
                DropType.COLONY_ITEM, 40f), Map.of(), 1f);

        assertEquals(10f, chances.increasedPercent(DropType.ITEM), EPSILON);
        assertEquals(30f, chances.increasedPercent(DropType.HULL_ITEM), EPSILON);
        assertEquals(60f, chances.increasedPercent(DropType.REACTOR), EPSILON);
        assertEquals(30f, chances.increasedPercent(DropType.BRIDGE), EPSILON);
        assertEquals(50f, chances.increasedPercent(DropType.COLONY_ITEM), EPSILON);
        assertEquals(10f, chances.increasedPercent(DropType.SUBROUTINE), EPSILON);
        assertEquals(1.6f, chances.quantity(DropType.REACTOR), EPSILON);
    }

    @Test
    void thePlayersStatsAddAtTheirOwnNodes() {
        ItemChances chances = ItemChances.increasedBy(DropType.ITEM, 10f).plusPercent(DropType.FEEDSTOCK, 10f);

        try (MockedStatic<FoundItemQuantity> found = Mockito.mockStatic(FoundItemQuantity.class)) {
            found.when(() -> FoundItemQuantity.nodePercent(DropType.ITEM)).thenReturn(20f);
            found.when(() -> FoundItemQuantity.nodePercent(DropType.HULL_ITEM)).thenReturn(30f);
            found.when(() -> FoundItemQuantity.nodePercent(DropType.FEEDSTOCK)).thenReturn(40f);

            assertEquals(1.8f, chances.quantity(DropType.FEEDSTOCK), EPSILON);
            assertEquals(1.6f, chances.quantity(DropType.FLIGHT_DECK), EPSILON);
            assertEquals(1.3f, chances.quantity(DropType.SUBROUTINE), EPSILON);
            assertEquals(1.3f, chances.quantity(DropType.STAR_CHART), EPSILON);
            assertEquals(1.3f, chances.quantity(DropType.COLONY_ITEM), EPSILON);
            assertEquals(1.3f, chances.quantity(DropType.WEAPON), EPSILON);
        }
    }

    @Test
    void aSectionsExtraIsWhatItAddsOverHullItems() {
        ItemChances chances = new ItemChances(Map.of(DropType.ITEM, 50f, DropType.BRIDGE, 30f), Map.of(), 0.5f);

        assertEquals(0.15f, chances.extraOverParent(DropType.BRIDGE), EPSILON);
        assertEquals(0f, chances.extraOverParent(DropType.REACTOR), EPSILON);
        assertEquals(0f, chances.extraOverParent(DropType.ITEM), EPSILON);
        assertEquals(0f, ItemChances.increasedBy(DropType.BRIDGE, -50f).extraOverParent(DropType.BRIDGE), EPSILON);
    }

    @Test
    void theShareScalesTheSummedQuantityAndNeverGoesNegative() {
        ItemChances chances = ItemChances.increasedBy(DropType.ITEM, 50f).scaled(0.5f);

        assertEquals(0.75f, chances.quantity(DropType.SUBROUTINE), EPSILON);
        assertEquals(0f, ItemChances.increasedBy(DropType.HULL_ITEM, -150f).quantity(DropType.BRIDGE), EPSILON);
        assertEquals(0f, ItemChances.atShare(0f).quantity(DropType.ITEM), EPSILON);
    }

    @Test
    void rarityAddsUpThroughTheSameTreeAsQuantity() {
        ItemChances chances = new ItemChances(Map.of(), Map.of(DropType.ITEM, 10f, DropType.HULL_ITEM, 20f, DropType.REACTOR, 30f,
                DropType.INHIBITOR, 40f), 1f);

        assertEquals(10f, chances.increasedRarity(DropType.ITEM), EPSILON);
        assertEquals(60f, chances.increasedRarity(DropType.REACTOR), EPSILON);
        assertEquals(30f, chances.increasedRarity(DropType.BRIDGE), EPSILON);
        assertEquals(50f, chances.increasedRarity(DropType.INHIBITOR), EPSILON);
        assertEquals(10f, chances.increasedRarity(DropType.ENGRAM), EPSILON);
        assertEquals(1.6f, chances.rarity(DropType.REACTOR), EPSILON);
        assertEquals(1.1f, chances.rarity(DropType.SCHEMATIC), EPSILON);
        assertEquals(1f, chances.quantity(DropType.REACTOR), EPSILON);
    }

    @Test
    void aRarityNodeReachesItselfAndEveryDropTypeBelowItAndNothingElse() {
        for (DropType node : DropType.values()) {
            ItemChances chances = ItemChances.BASE.plusRarityPercent(node, 100f);
            for (DropType type : DropType.values()) {
                float expected = type.chain().contains(node) ? 2f : 1f;
                assertEquals(expected, chances.rarity(type), EPSILON, node + " on " + type);
                assertEquals(1f, chances.quantity(type), EPSILON, node + " on " + type);
            }
        }
    }

    @Test
    void rarityIgnoresTheShareAndNeverGoesNegative() {
        ItemChances chances = ItemChances.BASE.plusRarityPercent(DropType.ITEM, 50f).scaled(0.5f);

        assertEquals(1.5f, chances.rarity(DropType.BRIDGE), EPSILON);
        assertEquals(0f, ItemChances.BASE.plusRarityPercent(DropType.HULL_ITEM, -150f).rarity(DropType.BRIDGE), EPSILON);
        assertEquals(1f, ItemChances.BASE.rarity(DropType.ITEM), EPSILON);
    }

    @Test
    void addingNothingKeepsTheSameChances() {
        ItemChances chances = ItemChances.increasedBy(DropType.STAR_CHART, 25f);

        assertSame(chances, chances.plusPercent(DropType.ITEM, 0f));
        assertSame(chances, chances.plusRarityPercent(DropType.ITEM, 0f));
        assertSame(chances, chances.scaled(1f));
        assertSame(ItemChances.BASE, ItemChances.atShare(1f));
        assertEquals(50f, chances.plusPercent(DropType.STAR_CHART, 25f).nodePercent(DropType.STAR_CHART), EPSILON);
    }
}
