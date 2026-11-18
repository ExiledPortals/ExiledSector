package exiledsector.items.crafting;

import exiledsector.items.Item;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.TestItems;
import exiledsector.items.modifier.ItemModifiers;
import exiledsector.items.modifier.RolledLine;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeInfusionsTest {

    private static final String PLATE = "infusion_plate";
    private static final String RELIC = "infusion_relic";
    private static final String INFUSION = "TIMEFLOW_PERCENT_TEST_INFUSION";
    private static final String CLASHING_INFUSION = "FLUX_CAPACITY_MULT_TEST_INFUSION";

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(modifierRow(INFUSION, "TIMEFLOW_PERCENT:3:5"))
                .put(modifierRow(CLASHING_INFUSION, "FLUX_CAPACITY_MULT:5:5")));
        ItemDefinitions.register(new JSONArray()
                .put(row(PLATE, "FLUX_CAPACITY_MULT:4:6; FLUX_DISSIPATION_MULT:4:6; HULL_MULT:4:6", "SHIELD_DAMAGE_TAKEN_MULT:-15:-10; ARMOR_PERCENT:6:9"))
                .put(row(RELIC, "HULL_MULT:4:6", "").put("unique", "TRUE")));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static JSONObject modifierRow(String id, String lines) throws Exception {
        return new JSONObject().put("id", id).put("affix", "prefix").put("scopes", "hull_item").put("lines", lines).put("name", id);
    }

    private static JSONObject row(String id, String prefixes, String suffixes) throws Exception {
        return new JSONObject().put("id", id).put("name", "Infusion Plate").put("icon", "graphics/icons/cargo/chip1.png").put("rarity", "20")
                .put("prefixes", prefixes).put("suffixes", suffixes).put("section", "armor_plating");
    }

    private static Item plate(RolledLine... effects) {
        return TestItems.withLines("plate", PLATE, 1L, List.of(effects));
    }

    private static final ForgeInfusions.Source SOURCE = definition -> INFUSION;

    private static String refusal(ForgeOperation operation, Item item, ForgeInfusions.Source source) {
        return operation.verdict(item, source).refusalKey();
    }

    private static String refusal(Item item) {
        return refusal(ForgeOperation.INFUSE, item, SOURCE);
    }

    @Test
    void infusionAddsThePartsModifierOnceToAnItemWithRoom() {
        Item item = plate(new RolledLine("FLUX_CAPACITY_MULT", 5f));

        assertTrue(ForgeOperation.INFUSE.verdict(item, SOURCE).allowed());
        assertFalse(ForgeOperation.INFUSE.picksAChoice());
        assertEquals(List.of(ForgeChoice.WHOLE_ITEM), ForgeOperation.INFUSE.choices(item, SOURCE));
        assertTrue(ForgeOperation.INFUSE.apply(item, ForgeChoice.WHOLE_ITEM, SOURCE, new Random(3L)));

        assertTrue(item.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(INFUSION)));
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "infuse.present", refusal(item));
        assertFalse(ForgeOperation.INFUSE.apply(item, ForgeChoice.WHOLE_ITEM, SOURCE, new Random(3L)));
    }

    @Test
    void aFullSideRefusesTheInfusionInsteadOfReplacingAModifier() {
        Item item = plate(new RolledLine("FLUX_CAPACITY_MULT", 5f), new RolledLine("HULL_MULT", 5f));

        assertEquals(ForgeOperation.REFUSAL_PREFIX + "infuse.full", refusal(item));
        assertFalse(ForgeOperation.INFUSE.apply(item, ForgeChoice.WHOLE_ITEM, SOURCE, new Random(1L)));
        assertEquals(2, item.explicits().size());
    }

    @Test
    void uniquesPartsWithoutAnInfusionAndClashingEffectsAreRefused() {
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "infuse.unique", refusal(TestItems.withLines("relic", RELIC, 1L, List.of())));
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "infuse.clash",
                refusal(ForgeOperation.INFUSE, plate(new RolledLine("FLUX_CAPACITY_MULT", 5f)), definition -> CLASHING_INFUSION));
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "infuse.none", refusal(ForgeOperation.INFUSE, plate(), definition -> null));
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "infuse.none", refusal(ForgeOperation.INFUSE, plate(), ForgeInfusions.Source.NONE));
    }

    @Test
    void biomeInfusionAddsTheSourcesModifierAndIsRefusedOnTheSameTermsUnderItsOwnKeys() {
        Item item = plate(new RolledLine("FLUX_CAPACITY_MULT", 5f));

        assertFalse(ForgeOperation.BIOME_INFUSE.picksAChoice());
        assertTrue(ForgeOperation.BIOME_INFUSE.apply(item, ForgeChoice.WHOLE_ITEM, SOURCE, new Random(3L)));
        assertTrue(item.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(INFUSION)));

        assertEquals(ForgeOperation.REFUSAL_PREFIX + "biome_infuse.present", refusal(ForgeOperation.BIOME_INFUSE, item, SOURCE));
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "biome_infuse.full",
                refusal(ForgeOperation.BIOME_INFUSE, plate(new RolledLine("FLUX_CAPACITY_MULT", 5f), new RolledLine("HULL_MULT", 5f)), SOURCE));
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "biome_infuse.unique",
                refusal(ForgeOperation.BIOME_INFUSE, TestItems.withLines("relic", RELIC, 1L, List.of()), SOURCE));
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "biome_infuse.clash",
                refusal(ForgeOperation.BIOME_INFUSE, plate(new RolledLine("FLUX_CAPACITY_MULT", 5f)), definition -> CLASHING_INFUSION));
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "biome_infuse.none", refusal(ForgeOperation.BIOME_INFUSE, plate(), ForgeInfusions.Source.NONE));
    }

    @Test
    void anInfusionWithoutASourceAddsNothing() {
        Item item = plate(new RolledLine("FLUX_CAPACITY_MULT", 5f));

        assertFalse(ForgeOperation.INFUSE.verdict(item).allowed());
        assertFalse(ForgeOperation.BIOME_INFUSE.apply(item, ForgeChoice.WHOLE_ITEM, new Random(1L)));
        assertEquals(1, item.explicits().size());
    }
}
