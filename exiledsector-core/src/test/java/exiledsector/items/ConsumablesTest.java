package exiledsector.items;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumablesTest {

    private static final String CONSUMABLE_ID = "test_consumable";
    private static final String INHIBITOR_ID = "test_inhibitor";
    private static final String UNUSED_INHIBITOR_ID = "test_unused_inhibitor";

    private record Doubling(UseVerdict verdict, boolean applies) implements Consumable<Integer, Integer> {

        @Override
        public String itemId() {
            return CONSUMABLE_ID;
        }

        @Override
        public UseVerdict check(Integer target) {
            return verdict;
        }

        @Override
        public Integer apply(Integer target) {
            return applies ? target * 2 : null;
        }

        @Override
        public Collection<String> requiredItemIds() {
            return List.of(INHIBITOR_ID, UNUSED_INHIBITOR_ID);
        }

        @Override
        public Collection<String> spentAlongside(Integer target) {
            return List.of(INHIBITOR_ID);
        }
    }

    private static CraftingMaterials holding(int consumables, int inhibitors, int unusedInhibitors) {
        Map<String, Integer> stored = new HashMap<>();
        stored.put(CONSUMABLE_ID, consumables);
        stored.put(INHIBITOR_ID, inhibitors);
        stored.put(UNUSED_INHIBITOR_ID, unusedInhibitors);
        return new CraftingMaterials(null, stored, true);
    }

    @Test
    void anAllowedUseSpendsTheConsumableAndOnlyTheExtrasItSpent() {
        CraftingMaterials materials = holding(2, 1, 1);

        assertEquals(6, Consumables.use(new Doubling(UseVerdict.ALLOW, true), 3, materials));

        assertEquals(1, materials.count(CONSUMABLE_ID));
        assertEquals(0, materials.count(INHIBITOR_ID));
        assertEquals(1, materials.count(UNUSED_INHIBITOR_ID));
    }

    @Test
    void aRefusedMissingOrFailedUseSpendsNothing() {
        CraftingMaterials materials = holding(1, 1, 1);

        assertNull(Consumables.use(new Doubling(UseVerdict.refused("test.refused"), true), 3, materials));
        assertNull(Consumables.use(new Doubling(UseVerdict.ALLOW, false), 3, materials));
        assertNull(Consumables.use(new Doubling(UseVerdict.ALLOW, true), 3, holding(0, 1, 1)));
        assertNull(Consumables.use(new Doubling(UseVerdict.ALLOW, true), 3, holding(1, 1, 0)));

        assertEquals(1, materials.count(CONSUMABLE_ID));
        assertEquals(1, materials.count(INHIBITOR_ID));
    }

    @Test
    void aVerdictCarriesItsKeyAndArguments() {
        UseVerdict refused = UseVerdict.refused("test.refused").with("limit", "2");

        assertTrue(UseVerdict.ALLOW.allowed());
        assertNull(UseVerdict.ALLOW.text("name"));
        assertFalse(refused.allowed());
        assertEquals("test.refused", refused.refusalKey());
        assertEquals(Map.of("limit", "2"), refused.args());
        assertEquals(UseVerdict.refused("test.refused").with("limit", "2"), refused);
    }
}
