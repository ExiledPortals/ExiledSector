package exiledsector.items;

import exiledsector.charts.anomaly.storm.StormLoot;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StormDropLimitsTest {

    private static final String STORMPROOF = "EMP_DAMAGE_TAKEN_PERCENT_STORM_FRONT";
    private static final String BASE = "test_bridge";

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(HullItemRows.modifierRow(STORMPROOF, "prefix", "EMP_DAMAGE_TAKEN_PERCENT:-25:-25", "Stormproof")));
        ItemDefinitions.register(List.of(new ItemDefinition(BASE, ItemKind.BRIDGE, BASE, ItemDefinition.FALLBACK_ICON, 1f, false, "",
                List.of(prefix("HULL_PERCENT"), prefix("FLUX_CAPACITY_MULT"), prefix("FLUX_DISSIPATION_MULT")),
                List.of(suffix("BEAM_WEAPON_RANGE_PERCENT"), suffix("ARMOR_PERCENT"), suffix("HULL_MULT")), "")));
        StormLoot.reserveModifiers();
    }

    @AfterEach
    void tearDown() {
        ReservedModifiers.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static PoolEntry prefix(String effectName) {
        return PoolEntry.of(ModifierSlot.PREFIX, effectName, 4f, 6f, 1f);
    }

    private static PoolEntry suffix(String effectName) {
        return PoolEntry.of(ModifierSlot.SUFFIX, effectName, 4f, 6f, 1f);
    }

    @Test
    void aStormTouchedHullItemDropStaysWithinTwoModifiers() {
        for (long seed = 0; seed < 100; seed++) {
            ItemData dropped = ItemData.rolledWithModifierCount(ItemDefinitions.get(BASE), seed, 1);

            ItemData touched = GrantedModifiers.grant(dropped, STORMPROOF, new Random(seed));

            assertEquals(2, touched.explicits().size(), touched.toString());
            assertTrue(touched.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(STORMPROOF)));
        }
    }
}
