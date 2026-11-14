package exiledsector.items;

import exiledsector.i18n.Style;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemOverchargeTest {

    private static final String PLATE = "overcharge_plate";
    private static final String DRAWBACK = "overcharge_drawback";
    private static final String SCALED = "overcharge_scaled";
    private static final String FIXED = "overcharge_fixed";
    private static final String SMALL = "overcharge_small";
    private static final String BURN = "overcharge_burn";
    private static final String MILD = "overcharge_mild";

    @BeforeEach
    void setUp() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(row(PLATE, "FLUX_CAPACITY_MULT:4:6; HULL_MULT:4:6", "SHIELD_DAMAGE_TAKEN_MULT:-15:-10"))
                .put(row(DRAWBACK, "WEAPON_DAMAGE_PERCENT:-10:-5", ""))
                .put(row(SCALED, "HULL_MULT:4/6/8/10", ""))
                .put(row(FIXED, "BURN_LEVEL_FLAT:1:1", ""))
                .put(row(SMALL, "BURN_LEVEL_FLAT:1:2", ""))
                .put(row(BURN, "BURN_LEVEL_FLAT:2:4", ""))
                .put(row(MILD, "WEAPON_DAMAGE_PERCENT:-2:-1", "")));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
    }

    private static JSONObject row(String id, String prefixes, String suffixes) throws Exception {
        return new JSONObject().put("id", id).put("name", "Overcharge Plate").put("icon", "graphics/icons/cargo/chip1.png").put("rarity", "20")
                .put("prefixes", prefixes).put("suffixes", suffixes).put("section", "armor_plating");
    }

    private static ItemData item(String definitionId, RolledLine... effects) {
        return ItemData.of(Item.withEffects("plate", definitionId, 1L, List.of(effects)));
    }

    private static List<RolledModifier> overchargedModifiers(ItemData item) {
        return item.explicits().stream().filter(RolledModifier::overcharged).toList();
    }

    @Test
    void oneModifierRollsAQuarterPastItsBestEndRoundedDownAndIsMarkedOvercharged() {
        for (long seed = 0; seed < 20; seed++) {
            ItemData item = item(PLATE, new RolledLine("FLUX_CAPACITY_MULT", 5f), new RolledLine("SHIELD_DAMAGE_TAKEN_MULT", -12f));

            ItemData overcharged = ItemOvercharge.overcharge(item, new Random(seed));

            List<RolledModifier> marked = overchargedModifiers(overcharged);
            assertEquals(1, marked.size());
            RolledLine line = marked.get(0).lines().get(0);
            assertEquals("FLUX_CAPACITY_MULT".equals(line.effectName()) ? 7f : -18f, line.magnitude(), 1e-4f,
                    "less shield damage taken is better, so it goes past the low end");
        }
    }

    @Test
    void aHarmfulModifierIsOverchargedTowardItsMildEnd() {
        ItemData overcharged = ItemOvercharge.overcharge(item(DRAWBACK, new RolledLine("WEAPON_DAMAGE_PERCENT", -8f)), new Random(1L));

        assertEquals(-3f, overchargedModifiers(overcharged).get(0).lines().get(0).magnitude(), 1e-4f);
    }

    @Test
    void wholeNumberRangesStayWholeAndOnesThatWouldStallOrVanishAreNeverPicked() {
        ItemData burn = ItemOvercharge.overcharge(item(BURN, new RolledLine("BURN_LEVEL_FLAT", 3f)), new Random(1L));
        ItemData small = item(SMALL, new RolledLine("BURN_LEVEL_FLAT", 1f));

        assertEquals(5f, overchargedModifiers(burn).get(0).lines().get(0).magnitude(), 1e-4f);
        assertSame(small, ItemOvercharge.overcharge(small, new Random(1L)));
        ItemData mild = item(MILD, new RolledLine("WEAPON_DAMAGE_PERCENT", -2f));
        assertSame(mild, ItemOvercharge.overcharge(mild, new Random(1L)));
    }

    @Test
    void anItemIsOverchargedOnlyOnceAndScaledOrFixedModifiersAreNeverPicked() {
        ItemData once = ItemOvercharge.overcharge(item(PLATE, new RolledLine("FLUX_CAPACITY_MULT", 5f), new RolledLine("HULL_MULT", 5f)),
                new Random(2L));

        assertSame(once, ItemOvercharge.overcharge(once, new Random(3L)));
        ItemData scaled = item(SCALED, new RolledLine("HULL_MULT", 4f));
        assertSame(scaled, ItemOvercharge.overcharge(scaled, new Random(3L)));
        ItemData fixed = item(FIXED, new RolledLine("BURN_LEVEL_FLAT", 1f));
        assertSame(fixed, ItemOvercharge.overcharge(fixed, new Random(3L)));
    }

    @Test
    void theOverchargeSurvivesTheCargoRoundTripAndShowsInTheTooltip() {
        ItemData overcharged = ItemOvercharge.overcharge(item(PLATE, new RolledLine("FLUX_CAPACITY_MULT", 5f)), new Random(1L));

        ItemData loaded = ItemData.of(overcharged.toSpecialItem());

        assertEquals(overcharged.explicits(), loaded.explicits());
        assertTrue(loaded.explicits().get(0).overcharged());
        assertTrue(loaded.preview().effectLines().stream().anyMatch(line -> line.spans().stream().anyMatch(span -> span.style() == Style.OVERCHARGED)));
    }

    @Test
    void rerollingTheValuesEndsTheOverchargeWhileOtherChangesKeepIt() {
        RolledModifier overcharged = new RolledModifier("FLUX_CAPACITY_MULT", ModifierSlot.PREFIX, List.of(new RolledLine("FLUX_CAPACITY_MULT", 7.5f)),
                false, true, false);
        Item item = Item.withEffects("plate", PLATE, 1L, List.of(new RolledLine("FLUX_CAPACITY_MULT", 5f)));

        RolledModifier rerolled = overcharged.rerolled(item.explicitModifier("FLUX_CAPACITY_MULT")::line, new Random(4L));

        assertFalse(rerolled.overcharged());
        assertTrue(rerolled.lines().get(0).magnitude() <= 6f);
        assertTrue(overcharged.asImmutable().overcharged());
        assertTrue(overcharged.withModifierId("RENAMED").overcharged());
    }

    @Test
    void perfectLeavesAnOverchargedModifierAlone() {
        Item item = ItemOvercharge.overcharge(item(PLATE, new RolledLine("FLUX_CAPACITY_MULT", 5f)), new Random(1L)).preview();

        assertFalse(ForgeOperation.PERFECT.verdict(item).allowed());
    }

    @Test
    void onlyHullItemsAreOverchargedThroughTheHullItemEntry() {
        ItemData plate = item(PLATE, new RolledLine("FLUX_CAPACITY_MULT", 5f));

        assertEquals(ItemType.HULL_ITEM, plate.definition().type());
        assertEquals(1, overchargedModifiers(ItemOvercharge.overchargeHullItem(plate, new Random(1L))).size());
        ItemData unknown = new ItemData("missing_definition", 1L);
        assertSame(unknown, ItemOvercharge.overchargeHullItem(unknown, new Random(1L)));
    }
}
