package exiledsector.charts.loot;

import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemModifiers;
import exiledsector.items.ModifierSources;
import exiledsector.items.PoolEntry;
import exiledsector.items.RolledModifier;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeModifiersTest {

    private static final String RIFT = "abyssal_rift";
    private static final String CHRONAL = "TIMEFLOW_PERCENT_ABYSSAL_RIFT";
    private static final String PLASMATIC = "SHIELD_UPKEEP_MULT_ABYSSAL_RIFT";
    private static final String BRIDGE = "test_bridge";
    private static final String RELIC = "test_relic";
    private static final String EMITTER = "test_emitter";

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(modifierRow(CHRONAL, "prefix", "TIMEFLOW_PERCENT:3:5", "Chronal"))
                .put(modifierRow(PLASMATIC, "prefix", "SHIELD_UPKEEP_MULT:-20:-15", "Plasmatic")));
        ItemDefinitions.register(List.of(
                definition(BRIDGE, ItemKind.BRIDGE, false),
                definition(RELIC, ItemKind.BRIDGE, true),
                definition(EMITTER, ItemKind.SHIELD_GENERATOR, false)));
    }

    @AfterEach
    void tearDown() {
        BiomeModifiers.unregisterAll();
        ModifierSources.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static JSONObject modifierRow(String id, String slot, String lines, String name) throws Exception {
        return new JSONObject().put("id", id).put("affix", slot).put("scopes", "hull_item").put("lines", lines).put("name", name);
    }

    private static JSONObject biomeRow(String modifierId, String biomeId, String item) throws Exception {
        return new JSONObject().put("modifier", modifierId).put("biome", biomeId).put("item", item);
    }

    private static ItemDefinition definition(String id, ItemKind kind, boolean unique) {
        return new ItemDefinition(id, kind, id, ItemDefinition.FALLBACK_ICON, 1f, unique, "",
                List.of(PoolEntry.of(ModifierSlot.PREFIX, "HULL_PERCENT", 4f, 6f, 1f)), List.of(), "");
    }

    private static boolean carries(ItemData item, String modifierId) {
        return item.explicits().stream().map(RolledModifier::modifierId).anyMatch(modifierId::equals);
    }

    @Test
    void eachRowReservesItsModifierForItsBiomeAndPart() throws Exception {
        BiomeModifiers.register(new JSONArray().put(biomeRow(CHRONAL, RIFT, "bridge")).put(biomeRow(PLASMATIC, RIFT, "shield_generator")));

        assertEquals(BiomeModifiers.OWNER_PREFIX + RIFT, ModifierSources.owner(CHRONAL));
        assertEquals(Set.of(ItemKind.BRIDGE), ModifierSources.parts(CHRONAL));
        assertEquals(Set.of(ItemKind.SHIELD_GENERATOR), ModifierSources.parts(PLASMATIC));
        assertEquals(CHRONAL, BiomeModifiers.modifierFor(RIFT, ItemKind.BRIDGE));
        assertEquals(PLASMATIC, BiomeModifiers.modifierFor(RIFT, ItemKind.SHIELD_GENERATOR));
        assertNull(BiomeModifiers.modifierFor(RIFT, ItemKind.REACTOR));
        assertNull(BiomeModifiers.modifierFor("normal", ItemKind.BRIDGE));
    }

    @Test
    void oneModifierOnTwoPartsOfABiomeReservesBothParts() throws Exception {
        BiomeModifiers.register(new JSONArray().put(biomeRow(CHRONAL, RIFT, "bridge")).put(biomeRow(CHRONAL, RIFT, "shield_generator")));

        assertEquals(Set.of(ItemKind.BRIDGE, ItemKind.SHIELD_GENERATOR), ModifierSources.parts(CHRONAL));
        assertEquals(CHRONAL, BiomeModifiers.modifierFor(RIFT, ItemKind.SHIELD_GENERATOR));
    }

    @Test
    void aSecondRowForTheSameBiomeAndPartOrANonHullPartIsSkipped() throws Exception {
        BiomeModifiers.register(new JSONArray().put(biomeRow(CHRONAL, RIFT, "bridge")).put(biomeRow(PLASMATIC, RIFT, "bridge"))
                .put(biomeRow("MISSING_MODIFIER", RIFT, "reactor")));

        assertEquals(CHRONAL, BiomeModifiers.modifierFor(RIFT, ItemKind.BRIDGE));
        assertFalse(ModifierSources.isReserved(PLASMATIC));
        assertNull(BiomeModifiers.modifierFor(RIFT, ItemKind.REACTOR));
    }

    @Test
    void aboutATenthOfTheBiomesDropsGainTheModifier() throws Exception {
        BiomeModifiers.register(new JSONArray().put(biomeRow(CHRONAL, RIFT, "bridge")));
        int drops = 2000;
        int touched = 0;
        Random seeds = new Random(1L);
        for (int drop = 0; drop < drops; drop++) {
            if (carries(BiomeModifiers.onDrop(ItemData.rolled(ItemDefinitions.get(BRIDGE), seeds.nextLong()), RIFT), CHRONAL)) {
                touched++;
            }
        }

        assertEquals(BiomeModifiers.DROP_CHANCE, touched / (float) drops, 0.04f);
    }

    @Test
    void aDropInTheBiomeGainsItsPartsModifierAndOnlyThere() throws Exception {
        BiomeModifiers.register(new JSONArray().put(biomeRow(CHRONAL, RIFT, "bridge")));
        long seed = 0L;
        while (!carries(BiomeModifiers.onDrop(ItemData.rolled(ItemDefinitions.get(BRIDGE), seed), RIFT), CHRONAL)) {
            seed++;
        }
        ItemData bridge = ItemData.rolled(ItemDefinitions.get(BRIDGE), seed);
        ItemData emitter = ItemData.rolled(ItemDefinitions.get(EMITTER), seed);
        ItemData relic = ItemData.rolled(ItemDefinitions.get(RELIC), seed);

        ItemData riftBridge = BiomeModifiers.onDrop(bridge, RIFT);

        assertTrue(carries(riftBridge, CHRONAL));
        assertEquals(riftBridge, BiomeModifiers.onDrop(bridge, RIFT));
        assertSame(bridge, BiomeModifiers.onDrop(bridge, "normal"));
        assertSame(emitter, BiomeModifiers.onDrop(emitter, RIFT));
        assertSame(relic, BiomeModifiers.onDrop(relic, RIFT));
    }

    @Test
    void unregisteringReleasesTheReservations() throws Exception {
        BiomeModifiers.register(new JSONArray().put(biomeRow(CHRONAL, RIFT, "bridge")));

        BiomeModifiers.unregisterAll();

        assertFalse(ModifierSources.isReserved(CHRONAL));
        assertNull(BiomeModifiers.modifierFor(RIFT, ItemKind.BRIDGE));
    }
}
