package exiledsector.items;

import exiledsector.charts.anomaly.storm.StormLoot;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrantedModifiersTest {

    private static final String STORMPROOF = "EMP_DAMAGE_TAKEN_PERCENT_STORM_FRONT";
    private static final String THUNDERHEAD = "NON_BEAM_ENERGY_WEAPON_CHAIN_CHANCE_PERCENT_STORM_FRONT";
    private static final String BASE = "test_bridge";
    private static final String RELIC = "test_relic";

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(HullItemRows.modifierRow(STORMPROOF, "prefix", "EMP_DAMAGE_TAKEN_PERCENT:-25:-25", "Stormproof"))
                .put(HullItemRows.modifierRow(THUNDERHEAD, "suffix", "NON_BEAM_ENERGY_WEAPON_CHAIN_CHANCE_PERCENT:8:8", "Thunderhead")));
        ItemDefinitions.register(List.of(
                definition(BASE, false, List.of(prefix("HULL_PERCENT"), prefix("FLUX_CAPACITY_MULT"), prefix("FLUX_DISSIPATION_MULT")),
                        List.of(suffix("BEAM_WEAPON_RANGE_PERCENT"), suffix("ARMOR_PERCENT"), suffix("HULL_MULT"))),
                definition(RELIC, true, List.of(prefix("ARMOR_PERCENT")), List.of())));
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

    private static ItemDefinition definition(String id, boolean unique, List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
        return new ItemDefinition(id, ItemKind.BRIDGE, id, ItemDefinition.FALLBACK_ICON, 1f, unique, "", prefixes, suffixes, "");
    }

    @Test
    void theStormModifiersAreReservedSoNoPoolRollsThem() {
        assertTrue(ReservedModifiers.isReserved(STORMPROOF));
        assertTrue(ReservedModifiers.isReserved(THUNDERHEAD));
    }

    @Test
    void grantingRerollsTheItemAroundTheGrantedModifier() {
        ItemData plain = ItemData.rolled(ItemDefinitions.get(BASE), 7L);

        ItemData granted = GrantedModifiers.grant(plain, STORMPROOF, new Random(3L));

        assertEquals(plain.definitionId(), granted.definitionId());
        assertEquals(plain.seed(), granted.seed());
        assertTrue(granted.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(STORMPROOF)));
        Item reloaded = Item.create("reloaded", BASE, 7L, granted.explicits());
        assertTrue(reloaded.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(STORMPROOF)));
    }

    @Test
    void uniqueItemsAreNeverGranted() {
        ItemData relic = ItemData.rolled(ItemDefinitions.get(RELIC), 7L);

        assertFalse(GrantedModifiers.canGrant(ItemDefinitions.get(RELIC), STORMPROOF));
        assertSame(relic, GrantedModifiers.grant(relic, STORMPROOF, new Random(1L)));
    }

    @Test
    void aShareOfHullItemsFromStormBattlesCarryAStormModifier() {
        List<ItemData> items = new ArrayList<>();
        for (long seed = 0; seed < 400; seed++) {
            items.add(ItemData.rolled(ItemDefinitions.get(BASE), seed));
        }
        ItemDrops.SiteLoot loot = new ItemDrops.SiteLoot(items, List.of(), Map.of("material", 2));

        ItemDrops.SiteLoot touched = StormLoot.touch(loot, new Random(9L));

        long stormy = touched.items().stream().filter(item -> item.explicits().stream()
                .anyMatch(rolled -> StormLoot.MODIFIER_IDS.contains(rolled.modifierId()))).count();
        assertEquals(400, touched.items().size());
        assertEquals(100f, stormy, 30f);
        assertEquals(Map.of("material", 2), touched.materials());
    }
}
