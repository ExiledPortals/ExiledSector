package exiledsector.items;

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
        ForgeInfusions.use(definition -> INFUSION);
    }

    @AfterEach
    void tearDown() {
        ForgeInfusions.use(null);
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
        return Item.withEffects("plate", PLATE, 1L, List.of(effects));
    }

    private static String refusal(Item item) {
        return ForgeOperation.INFUSE.verdict(item).refusalKey();
    }

    @Test
    void infusionAddsThePartsModifierOnceToAnItemWithRoom() {
        Item item = plate(new RolledLine("FLUX_CAPACITY_MULT", 5f));

        assertTrue(ForgeOperation.INFUSE.verdict(item).allowed());
        assertFalse(ForgeOperation.INFUSE.picksAChoice());
        assertEquals(List.of(ForgeChoice.WHOLE_ITEM), ForgeOperation.INFUSE.choices(item));
        assertTrue(ForgeOperation.INFUSE.apply(item, ForgeChoice.WHOLE_ITEM, new Random(3L)));

        assertTrue(item.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(INFUSION)));
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "infuse.present", refusal(item));
        assertFalse(ForgeOperation.INFUSE.apply(item, ForgeChoice.WHOLE_ITEM, new Random(3L)));
    }

    @Test
    void aFullSideRefusesTheInfusionInsteadOfReplacingAModifier() {
        Item item = plate(new RolledLine("FLUX_CAPACITY_MULT", 5f), new RolledLine("HULL_MULT", 5f));

        assertEquals(ForgeOperation.REFUSAL_PREFIX + "infuse.full", refusal(item));
        assertFalse(ForgeOperation.INFUSE.apply(item, ForgeChoice.WHOLE_ITEM, new Random(1L)));
        assertEquals(2, item.explicits().size());
    }

    @Test
    void uniquesPartsWithoutAnInfusionAndClashingEffectsAreRefused() {
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "infuse.unique", refusal(Item.withEffects("relic", RELIC, 1L, List.of())));

        ForgeInfusions.use(definition -> CLASHING_INFUSION);
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "infuse.clash", refusal(plate(new RolledLine("FLUX_CAPACITY_MULT", 5f))));

        ForgeInfusions.use(definition -> null);
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "infuse.none", refusal(plate()));
    }
}
