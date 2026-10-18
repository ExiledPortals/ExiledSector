package exiledsector.items;

import exiledsector.modifiers.ModifierLine;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemCorruptionTest {

    private static final String PLATE = "plate";
    private final ItemStore store = new ItemStore();

    @BeforeEach
    void setUp() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(ItemFixtures.row(PLATE, "armor_plating", "HULL_MULT:4:6; FLUX_CAPACITY_MULT:4:6; FLUX_DISSIPATION_MULT:4:6",
                        "ARMOR_PERCENT:10:20; SHIELD_DAMAGE_TAKEN_MULT:-15:-10; BEAM_WEAPON_DAMAGE_PERCENT:10:15")));
        ItemImplicits.register(new JSONArray()
                .put(new JSONObject().put("section", "armor_plating").put("implicits", "ARMOR_PERCENT:10:20"))
                .put(new JSONObject().put("section", "reactor").put("implicits", "FLUX_DISSIPATION_PERCENT:10:20")));
    }

    @AfterEach
    void tearDown() {
        ItemImplicits.clear();
        ItemDefinitions.clear();
        ItemNames.clear();
    }

    private Item plate(long seed) {
        return store.add(ItemDefinitions.get(PLATE), seed);
    }

    @Test
    void everyOutcomeCanHappenAndEachOneCorruptsTheItem() {
        Set<CorruptionOutcome> seen = EnumSet.noneOf(CorruptionOutcome.class);
        Random random = new Random(7L);
        for (long seed = 0; seed < 60; seed++) {
            Item item = plate(seed);
            Item corrupted = ItemCrafting.corrupt(store, item, random, definition -> true);
            assertTrue(corrupted.corrupted());
            seen.add(corrupted.corruption());
            for (CraftingCurrency currency : CraftingCurrency.values()) {
                assertFalse(ItemCrafting.canUse(currency, corrupted, definition -> true), currency + " must not touch a corrupted item");
            }
        }
        assertEquals(EnumSet.allOf(CorruptionOutcome.class), seen);
    }

    @Test
    void aReplacedImplicitComesFromAnotherSectionAndKeepsItsRangeAfterLoading() {
        Item item = plate(3L);

        assertTrue(ItemCrafting.replaceImplicit(item, new Random(1L)));
        item.corrupt(CorruptionOutcome.IMPLICIT_REPLACED);

        assertEquals("FLUX_DISSIPATION_PERCENT", item.implicits().get(0).modifierId());
        float magnitude = item.implicits().get(0).lines().get(0).magnitude();
        assertTrue(magnitude >= 10f && magnitude <= 20f);
        assertTrue(item.skillEffects().stream().anyMatch(effect -> effect.effect().name().equals("FLUX_DISSIPATION_PERCENT")));
        Item reloaded = new HullItem(item);
        assertEquals("FLUX_DISSIPATION_PERCENT", reloaded.implicits().get(0).modifierId(), "a corrupted implicit is never remapped back");
        assertTrue(reloaded.headerLines().stream().anyMatch(line -> line.plain().equals("Corrupted")));
    }

    @Test
    void randomisedValuesCanLandUpToAThirdPastTheirUsualRange() {
        boolean beyondRange = false;
        for (long seed = 0; seed < 80; seed++) {
            Item item = plate(seed);
            ItemCrafting.randomiseValues(item, new Random(seed));
            for (RolledModifier rolled : item.explicits()) {
                ModifierLine line = item.definition().poolEntryFor(rolled.modifierId()).modifier().line();
                float magnitude = rolled.lines().get(0).magnitude();
                ModifierLine widened = line.widened(ItemCrafting.CORRUPTION_RANGE_STRETCH);
                assertTrue(magnitude >= widened.min() && magnitude <= widened.max(), rolled.toString());
                beyondRange |= magnitude < line.min() || magnitude > line.max();
            }
        }
        assertTrue(beyondRange);
    }

    @Test
    void widenedRangesStretchBothBoundsAwayFromTheMiddleAndKeepTheirPrecision() {
        ModifierLine positive = ModifierLine.range("ARMOR_PERCENT", 10f, 20f).widened(0.35f);
        ModifierLine negative = ModifierLine.range("SHIELD_DAMAGE_TAKEN_MULT", -15f, -10f).widened(0.35f);
        ModifierLine decimal = ModifierLine.range("HULL_MULT", 0.2f, 0.4f).widened(0.35f);

        assertEquals(7f, positive.min());
        assertEquals(27f, positive.max());
        assertEquals(-20f, negative.min());
        assertEquals(-6f, negative.max());
        assertEquals(0.1f, decimal.min(), 1e-5f);
        assertEquals(0.5f, decimal.max(), 1e-5f);
        ModifierLine small = ModifierLine.range("BURN_LEVEL_FLAT", 1f, 2f).widened(0.35f);
        ModifierLine smallNegative = ModifierLine.range("SHIELD_DAMAGE_TAKEN_MULT", -3f, -1f).widened(0.35f);
        assertEquals(1f, small.min(), "a positive range never reaches zero");
        assertEquals(3f, small.max());
        assertEquals(-4f, smallNegative.min());
        assertEquals(-1f, smallNegative.max(), "a negative range never reaches zero");
    }

    @Test
    void randomisedModifiersAreDifferentButKeepAFixedOne() {
        for (long seed = 0; seed < 30; seed++) {
            Item item = plate(seed);
            if (item.explicits().size() < ItemCrafting.MIN_FIXATION_MODIFIERS) {
                continue;
            }
            ItemCrafting.fixate(item, new Random(seed));
            RolledModifier locked = item.explicits().stream().filter(RolledModifier::locked).findFirst().orElseThrow();
            Set<String> before = new HashSet<>(item.explicits().stream().map(RolledModifier::modifierId).toList());

            assertNotNull(ItemCrafting.randomiseModifiers(store, item, new Random(seed + 100), definition -> true));

            assertTrue(item.explicits().contains(locked));
            List<String> after = item.explicits().stream().map(RolledModifier::modifierId).toList();
            assertEquals(after.size(), new HashSet<>(after).size(), "no duplicate effects");
            assertTrue(item.explicits().stream().filter(modifier -> modifier.slot() == locked.slot()).count() <= AffixLayout.MAX_PER_SIDE);
            assertFalse(before.equals(new HashSet<>(after)) && after.size() > 1, "the effects change");
        }
    }
}
