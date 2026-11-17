package exiledsector.items;

import exiledsector.loot.DropType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UniqueDropRollTest {

    private static final int TRIALS = 40000;
    private static final float BASE_CHANCE = 0.2f;
    private static final ItemDefinition UNIQUE_BRIDGE = unique("unique_bridge", ItemKind.BRIDGE);
    private static final ItemDefinition UNIQUE_SUBROUTINE = unique("unique_subroutine", ItemKind.SUBROUTINE);

    private static ItemDefinition unique(String id, ItemKind kind) {
        return new ItemDefinition(id, kind, id, ItemDefinition.FALLBACK_ICON, 10f, true, "", List.of(), List.of(), null);
    }

    private static float dropShare(ItemDefinition picked, ItemChances chances) {
        Random random = new Random(3L);
        int drops = 0;
        for (int trial = 0; trial < TRIALS; trial++) {
            if (UniqueDropRoll.roll(random, BASE_CHANCE, chances, picker -> picked) != null) {
                drops++;
            }
        }
        return drops / (float) TRIALS;
    }

    private static float rarityScaledDropShare(ItemDefinition picked, ItemChances chances) {
        Random random = new Random(3L);
        int drops = 0;
        for (int trial = 0; trial < TRIALS; trial++) {
            if (UniqueDropRoll.rollScaledByRarity(random, BASE_CHANCE, chances, picker -> picked) != null) {
                drops++;
            }
        }
        return drops / (float) TRIALS;
    }

    private static ItemChances increased(DropType node, float increasedPercent) {
        return ItemChances.increasedBy(node, increasedPercent);
    }

    @Test
    void aUniqueDropsAtTheQuantityOfItsOwnDropType() {
        assertEquals(BASE_CHANCE * 2f, dropShare(UNIQUE_SUBROUTINE, increased(DropType.SUBROUTINE, 100f)), 0.01f);
        assertEquals(BASE_CHANCE, dropShare(UNIQUE_BRIDGE, increased(DropType.SUBROUTINE, 100f)), 0.01f);
        assertEquals(BASE_CHANCE * 2f, dropShare(UNIQUE_BRIDGE, increased(DropType.BRIDGE, 100f)), 0.01f);
        assertEquals(BASE_CHANCE, dropShare(UNIQUE_BRIDGE, increased(DropType.REACTOR, 100f)), 0.01f);
    }

    @Test
    void theRootAndTheHullItemNodeRaiseEveryUniqueBelowThem() {
        assertEquals(BASE_CHANCE * 1.5f, dropShare(UNIQUE_BRIDGE, increased(DropType.ITEM, 50f)), 0.01f);
        assertEquals(BASE_CHANCE * 1.5f, dropShare(UNIQUE_SUBROUTINE, increased(DropType.ITEM, 50f)), 0.01f);
        assertEquals(BASE_CHANCE * 1.5f, dropShare(UNIQUE_BRIDGE, increased(DropType.HULL_ITEM, 50f)), 0.01f);
        assertEquals(BASE_CHANCE, dropShare(UNIQUE_SUBROUTINE, increased(DropType.HULL_ITEM, 50f)), 0.01f);
    }

    @Test
    void aUniqueReadsTheSumOfItsWholeChain() {
        ItemChances chances = new ItemChances(Map.of(DropType.ITEM, 20f, DropType.HULL_ITEM, 30f, DropType.BRIDGE, 50f), Map.of(), 1f);

        assertEquals(BASE_CHANCE * 2f, dropShare(UNIQUE_BRIDGE, chances), 0.01f);
        assertEquals(BASE_CHANCE * 1.2f, dropShare(UNIQUE_SUBROUTINE, chances), 0.01f);
    }

    @Test
    void aRarityScaledUniqueReadsTheRarityChainOfTheUniqueItRolls() {
        ItemChances chances = ItemChances.BASE.plusRarityPercent(DropType.HULL_ITEM, 100f).plusPercent(DropType.BRIDGE, 50f);

        assertEquals(BASE_CHANCE * 3f, rarityScaledDropShare(UNIQUE_BRIDGE, chances), 0.01f);
        assertEquals(BASE_CHANCE, rarityScaledDropShare(UNIQUE_SUBROUTINE, chances), 0.01f);
        assertEquals(BASE_CHANCE * 1.5f, dropShare(UNIQUE_BRIDGE, chances), 0.01f);
    }

    @Test
    void withoutRarityTheRarityScaledRollMakesTheSameDraws() {
        ItemChances chances = ItemChances.increasedBy(DropType.BRIDGE, 50f);
        Random plain = new Random(9L);
        Random scaled = new Random(9L);
        for (int trial = 0; trial < 2000; trial++) {
            assertEquals(UniqueDropRoll.roll(plain, BASE_CHANCE, chances, picker -> UNIQUE_BRIDGE),
                    UniqueDropRoll.rollScaledByRarity(scaled, BASE_CHANCE, chances, picker -> UNIQUE_BRIDGE));
        }
    }

    @Test
    void withNothingToPickNothingDrops() {
        assertNull(UniqueDropRoll.roll(new Random(1L), 1f, ItemChances.BASE, picker -> null));
    }

    @Test
    void aUniqueWithoutItsOwnKindReadsTheHullItemNodeOrTheRoot() {
        assertEquals(DropType.SUBROUTINE, DropType.forDefinition(UNIQUE_SUBROUTINE));
        assertEquals(DropType.BRIDGE, DropType.forDefinition(UNIQUE_BRIDGE));
    }
}
