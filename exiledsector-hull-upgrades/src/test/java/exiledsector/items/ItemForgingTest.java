package exiledsector.items;

import exiledsector.skills.skilleffect.SkillEffect;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemForgingTest {

    private static final String PLATE = "forge_plate";
    private static final String RELIC = "forge_relic";
    private static final String DRAWBACK = "forge_drawback";

    @BeforeEach
    void setUp() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(row(PLATE, "FLUX_CAPACITY_MULT:4:6; FLUX_DISSIPATION_MULT:4:6; HULL_MULT:4:6",
                        "SHIELD_DAMAGE_TAKEN_MULT:-15:-10; ARMOR_PERCENT:6:9"))
                .put(row(RELIC, "HULL_MULT:4:6; ARMOR_PERCENT:6:9", "").put("unique", "TRUE"))
                .put(row(DRAWBACK, "WEAPON_DAMAGE_PERCENT:-10:-5", "")));
        ItemImplicits.register(new JSONArray().put(new JSONObject().put("section", "armor_plating").put("implicits", "ARMOR_PERCENT:10:20")));
    }

    @AfterEach
    void tearDown() {
        ItemImplicits.clear();
        ItemDefinitions.clear();
    }

    private static JSONObject row(String id, String prefixes, String suffixes) throws Exception {
        return new JSONObject().put("id", id).put("name", "Forge Plate").put("icon", "graphics/icons/cargo/chip1.png").put("rarity", "20")
                .put("prefixes", prefixes).put("suffixes", suffixes).put("section", "armor_plating");
    }

    private static Item plate(RolledEffect... effects) {
        return Item.withEffects("plate", PLATE, 1L, List.of(effects));
    }

    private static List<String> effectNames(Item item) {
        return item.effects().stream().map(RolledEffect::effectName).toList();
    }

    @Test
    void anEffectsPartIsItsSkillEffectSubpackage() {
        assertEquals("flux", ItemForging.partOf(SkillEffect.byName("FLUX_CAPACITY_MULT")));
        assertEquals("shield", ItemForging.partOf(SkillEffect.byName("SHIELD_DAMAGE_TAKEN_MULT")));
        assertEquals("defense", ItemForging.partOf(SkillEffect.byName("HULL_MULT")));
        assertNull(ItemForging.partOf(null));
    }

    @Test
    void graftOffersOnlyPartsWithAnOpenModifierAndAddsOneOfThatPart() {
        Item item = plate(new RolledEffect("FLUX_CAPACITY_MULT", 5f));

        assertEquals(List.of("defense", "flux", "shield"), ItemForging.graftParts(item));
        for (long seed = 0; seed < 10; seed++) {
            Item fresh = plate(new RolledEffect("FLUX_CAPACITY_MULT", 5f));
            RolledModifier added = ItemForging.graft(fresh, "shield", new Random(seed));
            assertNotNull(added);
            assertEquals(List.of("FLUX_CAPACITY_MULT", "SHIELD_DAMAGE_TAKEN_MULT"), effectNames(fresh));
        }
        assertTrue(ForgeOperation.GRAFT.apply(item, ForgeChoice.part("flux"), new Random(2L)));
        assertEquals(List.of("FLUX_CAPACITY_MULT", "FLUX_DISSIPATION_MULT"), effectNames(item));
        assertFalse(ForgeOperation.GRAFT.choices(item).contains(ForgeChoice.part("flux")), "both prefixes are taken");
    }

    @Test
    void graftRefusesAFullItem() {
        Item item = plate(new RolledEffect("FLUX_CAPACITY_MULT", 5f), new RolledEffect("HULL_MULT", 5f),
                new RolledEffect("SHIELD_DAMAGE_TAKEN_MULT", -12f), new RolledEffect("ARMOR_PERCENT", 7f));

        assertEquals(ForgeOperation.REFUSAL_PREFIX + "graft", ForgeOperation.GRAFT.verdict(item).refusalKey());
        assertFalse(ForgeOperation.GRAFT.apply(item, ForgeChoice.part("flux"), new Random(1L)));
    }

    @Test
    void exciseRemovesTheChosenModifierButNeverAnImmutableOne() {
        Item item = plate(new RolledEffect("FLUX_CAPACITY_MULT", 5f), new RolledEffect("ARMOR_PERCENT", 7f));
        RolledModifier armor = item.explicits().get(1);
        item.replaceExplicits(List.of(item.explicits().get(0).asLocked(), armor));

        assertEquals(List.of(ForgeChoice.modifier(armor)), ForgeOperation.EXCISE.choices(item));
        assertTrue(ForgeOperation.EXCISE.apply(item, ForgeChoice.modifier(armor), new Random(1L)));
        assertEquals(List.of("FLUX_CAPACITY_MULT"), effectNames(item));
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "excise", ForgeOperation.EXCISE.verdict(item).refusalKey());
    }

    @Test
    void perfectSetsEveryLineToTheBestEndForThePlayer() {
        Item item = plate(new RolledEffect("FLUX_CAPACITY_MULT", 5f), new RolledEffect("SHIELD_DAMAGE_TAKEN_MULT", -12f));

        assertTrue(ForgeOperation.PERFECT.apply(item, ForgeChoice.modifier(item.explicits().get(1)), new Random(1L)));
        assertEquals(-15f, item.effects().get(1).magnitude(), "less shield damage taken is better");
        assertTrue(ForgeOperation.PERFECT.apply(item, ForgeChoice.modifier(item.explicits().get(0)), new Random(1L)));
        assertEquals(6f, item.effects().get(0).magnitude());
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "perfect", ForgeOperation.PERFECT.verdict(item).refusalKey());
    }

    @Test
    void perfectingAHarmfulModifierMakesItAsMildAsItsRangeAllows() {
        Item item = Item.withEffects("drawback", DRAWBACK, 1L, List.of(new RolledEffect("WEAPON_DAMAGE_PERCENT", -8f)));

        assertTrue(ForgeOperation.PERFECT.apply(item, ForgeChoice.modifier(item.explicits().get(0)), new Random(1L)));
        assertEquals(-5f, item.effects().get(0).magnitude());
    }

    @Test
    void aRollAlreadyPastTodaysRangeIsNeverPerfectedDownward() {
        Item item = plate(new RolledEffect("FLUX_CAPACITY_MULT", 7f));

        assertEquals(ForgeOperation.REFUSAL_PREFIX + "perfect", ForgeOperation.PERFECT.verdict(item).refusalKey());
        Item retempered = plate();
        retempered.replaceImplicits(List.of(retempered.implicits().get(0).withLines(List.of(new RolledEffect("ARMOR_PERCENT", 25f)))));
        assertFalse(ForgeOperation.RETEMPER.verdict(retempered).allowed());
    }

    @Test
    void anchorRerollKeepsTheAnchorAndTheModifierCount() {
        for (long seed = 0; seed < 20; seed++) {
            Item item = plate(new RolledEffect("FLUX_CAPACITY_MULT", 5f), new RolledEffect("HULL_MULT", 5f),
                    new RolledEffect("ARMOR_PERCENT", 7f));
            RolledModifier anchor = item.explicits().get(1);

            assertTrue(ForgeOperation.ANCHOR_REROLL.apply(item, ForgeChoice.modifier(anchor), new Random(seed)));
            assertEquals(3, item.explicits().size());
            assertTrue(item.explicits().contains(anchor));
            assertTrue(item.explicits().stream().filter(rolled -> rolled.slot() == ModifierSlot.PREFIX).count() <= AffixLayout.MAX_PER_SIDE);
        }
    }

    @Test
    void anchorRerollNeedsAnotherModifierToReroll() {
        Item item = plate(new RolledEffect("FLUX_CAPACITY_MULT", 5f));

        assertEquals(ForgeOperation.REFUSAL_PREFIX + "anchor_reroll", ForgeOperation.ANCHOR_REROLL.verdict(item).refusalKey());
    }

    @Test
    void retemperKeepsTheStrongerOfTheOldAndNewImplicitRolls() {
        for (long seed = 0; seed < 20; seed++) {
            Item item = plate();
            item.replaceImplicits(List.of(item.implicits().get(0).withLines(List.of(new RolledEffect("ARMOR_PERCENT", 14f)))));

            assertTrue(ForgeOperation.RETEMPER.apply(item, ForgeChoice.WHOLE_ITEM, new Random(seed)));
            assertTrue(item.implicits().get(0).lines().get(0).magnitude() >= 14f);
        }
        Item perfect = plate();
        perfect.replaceImplicits(List.of(perfect.implicits().get(0).withLines(List.of(new RolledEffect("ARMOR_PERCENT", 20f)))));
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "retemper", ForgeOperation.RETEMPER.verdict(perfect).refusalKey());
    }

    @Test
    void uniquesRefuseEveryProgramThatChangesWhichModifiersTheyHold() {
        Item relic = Item.withEffects("relic", RELIC, 1L, List.of(new RolledEffect("HULL_MULT", 5f), new RolledEffect("ARMOR_PERCENT", 7f)));

        assertFalse(ForgeOperation.GRAFT.verdict(relic).allowed());
        assertFalse(ForgeOperation.EXCISE.verdict(relic).allowed());
        assertFalse(ForgeOperation.ANCHOR_REROLL.verdict(relic).allowed());
        assertTrue(ForgeOperation.PERFECT.verdict(relic).allowed());
    }

    @Test
    void mirroredAndCorruptedItemsAreRefusedLikeNanoswarmsRefuseThem() {
        Item corrupted = plate(new RolledEffect("FLUX_CAPACITY_MULT", 5f));
        corrupted.corrupt(CorruptionOutcome.NOTHING);

        assertEquals(ForgeOperation.REFUSAL_PREFIX + "corrupted", ForgeOperation.PERFECT.verdict(corrupted).refusalKey());
        assertEquals(ForgeOperation.REFUSAL_PREFIX + "loadFirst", ForgeOperation.PERFECT.verdict(null).refusalKey());
        assertFalse(ForgeOperation.PERFECT.apply(corrupted, ForgeChoice.modifier(corrupted.explicits().get(0)), new Random(1L)));
    }

    @Test
    void everyOperationHasAnId() {
        for (ForgeOperation operation : ForgeOperation.values()) {
            assertEquals(operation, ForgeOperation.byIdOrNull(operation.id()));
        }
    }
}
