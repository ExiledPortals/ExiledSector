package exiledsector.charts.anomaly.stormfront;

import exiledsector.charts.loot.BiomeModifiers;
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

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StormLootTest {

    private static final String RIFT = "abyssal_rift";
    private static final String BELT = "storm_belt";
    private static final String CHRONAL = "TIMEFLOW_PERCENT_ABYSSAL_RIFT";
    private static final String CHARGED = "ARMOR_PERCENT_STORM_BELT";
    private static final String PLASMATIC = "SHIELD_UPKEEP_MULT_ABYSSAL_RIFT";
    private static final String BRIDGE = "test_bridge";
    private static final String EMITTER = "test_emitter";
    private static final String RELIC = "test_relic";
    private static final Predicate<String> EVERY_BIOME_OPEN = biomeId -> true;

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(modifierRow(CHRONAL, "TIMEFLOW_PERCENT:3:5", "Chronal"))
                .put(modifierRow(CHARGED, "ARMOR_PERCENT:3:5", "Charged"))
                .put(modifierRow(PLASMATIC, "SHIELD_UPKEEP_MULT:-20:-15", "Plasmatic")));
        ItemDefinitions.register(List.of(
                definition(BRIDGE, ItemKind.BRIDGE, false),
                definition(EMITTER, ItemKind.SHIELD_GENERATOR, false),
                definition(RELIC, ItemKind.BRIDGE, true)));
        BiomeModifiers.register(new JSONArray()
                .put(biomeRow(CHRONAL, RIFT, "bridge"))
                .put(biomeRow(CHARGED, BELT, "bridge"))
                .put(biomeRow(PLASMATIC, RIFT, "shield_generator")));
    }

    @AfterEach
    void tearDown() {
        BiomeModifiers.unregisterAll();
        ModifierSources.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static JSONObject modifierRow(String id, String lines, String name) throws Exception {
        return new JSONObject().put("id", id).put("affix", "prefix").put("scopes", "hull_item").put("lines", lines).put("name", name);
    }

    private static JSONObject biomeRow(String modifierId, String biomeId, String item) throws Exception {
        return new JSONObject().put("modifier", modifierId).put("biome", biomeId).put("item", item);
    }

    private static ItemDefinition definition(String id, ItemKind kind, boolean unique) {
        return new ItemDefinition(id, kind, id, ItemDefinition.FALLBACK_ICON, 1f, unique, "",
                List.of(PoolEntry.of(ModifierSlot.PREFIX, "HULL_PERCENT", 4f, 6f, 1f)), List.of(), "");
    }

    private static ItemData dropped(String definitionId, long seed) {
        return ItemData.rolled(ItemDefinitions.get(definitionId), seed);
    }

    private static List<String> biomeModifiersOn(ItemData item) {
        return item.explicits().stream().map(RolledModifier::modifierId).filter(BiomeModifiers::isBiomeModifier).toList();
    }

    @Test
    void aStormDropTakesItsSystemBiomesModifierForItsPart() {
        for (long seed = 0L; seed < 20L; seed++) {
            assertEquals(List.of(CHRONAL), biomeModifiersOn(StormLoot.withBiomeModifier(dropped(BRIDGE, seed), RIFT, EVERY_BIOME_OPEN)));
        }
    }

    @Test
    void aSystemWithoutABiomeModifierGivesARandomOpenBiomesOne() {
        Set<String> granted = new HashSet<>();
        Random seeds = new Random(5L);
        for (int drop = 0; drop < 40; drop++) {
            List<String> modifiers = biomeModifiersOn(StormLoot.withBiomeModifier(dropped(BRIDGE, seeds.nextLong()), "normal", EVERY_BIOME_OPEN));

            assertEquals(1, modifiers.size());
            granted.addAll(modifiers);
        }
        assertEquals(Set.of(CHRONAL, CHARGED), granted);
    }

    @Test
    void aBiomeThePlayerCannotReachYetIsLeftOutOfTheRandomPick() {
        for (long seed = 0L; seed < 20L; seed++) {
            assertEquals(List.of(CHARGED), biomeModifiersOn(StormLoot.withBiomeModifier(dropped(BRIDGE, seed), "normal", BELT::equals)));
        }
    }

    @Test
    void anItemAlreadyCarryingABiomeModifierGetsNothingMore() {
        ItemData touched = StormLoot.withBiomeModifier(dropped(BRIDGE, 3L), BELT, EVERY_BIOME_OPEN);

        assertSame(touched, StormLoot.withBiomeModifier(touched, RIFT, EVERY_BIOME_OPEN));
    }

    @Test
    void uniquesAndItemsWithNothingOpenToTakeAreLeftAlone() {
        ItemData relic = dropped(RELIC, 3L);
        ItemData emitter = dropped(EMITTER, 3L);

        assertSame(relic, StormLoot.withBiomeModifier(relic, RIFT, EVERY_BIOME_OPEN));
        assertSame(emitter, StormLoot.withBiomeModifier(emitter, BELT, biomeId -> false));
        assertEquals(List.of(PLASMATIC), biomeModifiersOn(StormLoot.withBiomeModifier(emitter, BELT, EVERY_BIOME_OPEN)));
    }
}
