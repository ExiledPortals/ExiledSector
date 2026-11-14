package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullItemCraftTest {

    private static final String FEEDSTOCK_A = "test_feedstock_a";
    private static final String FEEDSTOCK_B = "test_feedstock_b";
    private static final CraftCost COST = new CraftCost(List.of(FEEDSTOCK_A, FEEDSTOCK_B), 10, 2);

    private static final class FakeCraft implements HullItemCraft {

        private final UseVerdict verdict;
        private final boolean changes;
        private int changeCount;

        FakeCraft(UseVerdict verdict, boolean changes) {
            this.verdict = verdict;
            this.changes = changes;
        }

        @Override
        public String id() {
            return "fake";
        }

        @Override
        public CraftCost cost() {
            return COST;
        }

        @Override
        public UseVerdict verdict(Item item, Collection<CraftingInhibitor> inhibitors) {
            return verdict;
        }

        @Override
        public Set<CraftingInhibitor> inhibitorsSpent(Item item, Collection<CraftingInhibitor> inhibitors) {
            return Set.of(CraftingInhibitor.PREFIX);
        }

        @Override
        public boolean changeModifiers(Item item, Collection<CraftingInhibitor> inhibitors, Random random) {
            changeCount++;
            return changes;
        }
    }

    private static CraftingMaterials materials(int feedstockEach, int reshapingCount, int prefixInhibitors) {
        Map<String, Integer> counts = new HashMap<>(Map.of(FEEDSTOCK_A, feedstockEach, FEEDSTOCK_B, feedstockEach,
                CraftCost.reshapingId(), reshapingCount, CraftingInhibitor.PREFIX.itemId(), prefixInhibitors,
                CraftingInhibitor.SUFFIX.itemId(), 1));
        return CraftingMaterials.storedForTests(counts);
    }

    @Test
    void aCostNeedsItsAmountOfEveryFeedstockAndItsReshapingNanoswarms() {
        assertTrue(COST.canAfford(materials(10, 2, 0)));
        assertFalse(COST.canAfford(materials(9, 2, 0)));
        assertFalse(COST.canAfford(materials(10, 1, 0)));
    }

    @Test
    void aCraftThatRunsTakesItsCostAndTheInhibitorsItSpends() {
        CraftingMaterials materials = materials(15, 3, 1);

        Optional<Set<CraftingInhibitor>> spent = new FakeCraft(UseVerdict.ALLOW, true).use(null, materials,
                Set.of(CraftingInhibitor.PREFIX, CraftingInhibitor.SUFFIX), new Random(1L));

        assertEquals(Optional.of(Set.of(CraftingInhibitor.PREFIX)), spent);
        assertEquals(5, materials.count(FEEDSTOCK_A));
        assertEquals(5, materials.count(FEEDSTOCK_B));
        assertEquals(1, materials.count(CraftCost.reshapingId()));
        assertEquals(0, materials.count(CraftingInhibitor.PREFIX.itemId()));
        assertEquals(1, materials.count(CraftingInhibitor.SUFFIX.itemId()));
    }

    @Test
    void aRefusedUnaffordableOrFailedCraftTakesNothing() {
        FakeCraft refused = new FakeCraft(UseVerdict.refused("refused"), true);
        FakeCraft failed = new FakeCraft(UseVerdict.ALLOW, false);
        FakeCraft unheldInhibitor = new FakeCraft(UseVerdict.ALLOW, true);
        CraftingMaterials materials = materials(15, 3, 0);

        assertEquals(Optional.empty(), refused.use(null, materials, Set.of(), new Random(1L)));
        assertEquals(Optional.empty(), failed.use(null, materials, Set.of(), new Random(1L)));
        assertEquals(Optional.empty(), unheldInhibitor.use(null, materials, Set.of(CraftingInhibitor.PREFIX), new Random(1L)));
        assertEquals(Optional.empty(), unheldInhibitor.use(null, materials(9, 3, 1), Set.of(), new Random(1L)));

        assertEquals(0, refused.changeCount);
        assertEquals(1, failed.changeCount);
        assertEquals(0, unheldInhibitor.changeCount);
        assertEquals(15, materials.count(FEEDSTOCK_A));
        assertEquals(3, materials.count(CraftCost.reshapingId()));
    }

    @Test
    void theSharedRefusalsCheckTheItemThenWhetherInhibitorsAloneBlockTheCraft() {
        Set<ModifierSlot> prefixInhibited = Set.of(ModifierSlot.PREFIX);

        assertEquals(HullItemCraftRefusals.PREFIX + "loadFirst", HullItemCraftRefusals.ofItem(null).refusalKey());
        assertTrue(HullItemCraftRefusals.ofInhibitors(inhibited -> true, prefixInhibited, "impossible").allowed());
        assertEquals(HullItemCraftRefusals.PREFIX + "inhibited",
                HullItemCraftRefusals.ofInhibitors(Set::isEmpty, prefixInhibited, "impossible").refusalKey());
        assertEquals("impossible", HullItemCraftRefusals.ofInhibitors(inhibited -> false, prefixInhibited, "impossible").refusalKey());
    }
}
