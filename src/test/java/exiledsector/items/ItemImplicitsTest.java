package exiledsector.items;

import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierCategory;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.skilleffect.EffectAliases;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemImplicitsTest {

    @BeforeEach
    void loadRealData() throws Exception {
        EffectAliases.register(csvRows(EffectAliases.DATA_PATH));
        ItemModifiers.register(csvRows(ItemModifiers.DATA_PATH));
        ItemDefinitions.register(csvRows(ItemDefinitions.SUBROUTINES_PATH), csvRows(ItemDefinitions.HULL_ITEMS_PATH));
        ItemImplicits.register(csvRows(ItemImplicits.DATA_PATH));
    }

    @AfterEach
    void clear() {
        ItemImplicits.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    @Test
    void everyHullSocketTypeHasImplicitsThatRollRangesOfKnownEffects() {
        for (ItemKind type : ItemKind.sectionItemTypes()) {
            List<Modifier> implicits = ItemImplicits.forType(type);
            assertFalse(implicits.isEmpty(), type.id() + " has no implicits");
            for (Modifier implicit : implicits) {
                assertFalse(implicit.line().scalesWithHullSize(), implicit.id());
                assertTrue(implicit.line().canVary(), implicit.id() + " should roll a range");
            }
        }
        assertTrue(ItemImplicits.forType(ItemKind.SUBROUTINE).isEmpty());
    }

    @Test
    void subroutinesCannotBeGivenImplicits() {
        assertThrows(IllegalArgumentException.class, () -> ItemImplicits.parseImplicits(ItemKind.SUBROUTINE, "HULL_PERCENT:5:10"));
        assertThrows(IllegalArgumentException.class,
                () -> ItemImplicits.parseImplicits(ItemKind.ARMOR_PLATING, "ARMOR_FLAT:10/15/20/30"));
    }

    @Test
    void anItemRollsTheSameImplicitsFromItsSeedAndKeepsThemFrozen() throws Exception {
        Item shieldItem = Item.withEffects("item", "shield_generator_emitter", 42L, List.of());
        List<RolledEffect> firstRoll = RolledModifier.lines(shieldItem.implicits());
        Item sameSeed = Item.withEffects("other", "shield_generator_emitter", 42L, List.of());

        assertEquals(2, firstRoll.size());
        assertEquals(magnitudes(firstRoll), magnitudes(RolledModifier.lines(sameSeed.implicits())));
        ItemImplicits.register(new JSONArray().put(new JSONObject().put("section", "shield_generator").put("implicits", "HULL_PERCENT:50:60")));
        assertEquals(magnitudes(firstRoll), magnitudes(RolledModifier.lines(shieldItem.implicits())), "already-rolled implicits stay frozen");
    }

    @Test
    void uniquesGetTheirTypeImplicitsAndSubroutinesGetNone() {
        ItemDefinition unique = ItemDefinitions.all().stream()
                .filter(definition -> definition.unique() && definition.kind() == ItemKind.WEAPON_MOUNT).findFirst().orElseThrow();
        ItemDefinition subroutine = ItemDefinitions.all().stream()
                .filter(definition -> definition.kind() == ItemKind.SUBROUTINE).findFirst().orElseThrow();

        assertEquals(1, Item.withEffects("u", unique.id(), 7L, List.of()).implicits().size());
        assertTrue(Item.withEffects("s", subroutine.id(), 7L, List.of()).implicits().isEmpty());
    }

    @Test
    void implicitsApplyAlongsideExplicitsWithoutChangingRarity() {
        Item plate = Item.withEffects("plate", "armor_plating_plate", 9L, List.of(new RolledEffect("HULL_PERCENT", 5f)));
        List<SkillTypeEffect> applied = plate.skillEffects();

        assertEquals(2, applied.size());
        assertEquals("ARMOR_PERCENT", applied.get(0).effect().name());
        assertEquals(ItemRarity.COMMON, plate.rarity());
    }

    @Test
    void anItemGroupsItsRollsIntoImplicitAndExplicitWithPrefixesAndSuffixesUnderExplicit() {
        Item plate = Item.withEffects("plate", "armor_plating_plate", 9L,
                List.of(new RolledEffect("HULL_PERCENT", 5f), new RolledEffect("KINETIC_DAMAGE_TAKEN_PERCENT", -5f)));

        assertEquals(List.of(ModifierSlot.IMPLICIT), plate.rolls(ModifierCategory.IMPLICIT).stream().map(SlottedRoll::slot).toList());
        assertEquals(List.of(ModifierSlot.PREFIX, ModifierSlot.SUFFIX),
                plate.rolls(ModifierCategory.EXPLICIT).stream().map(SlottedRoll::slot).toList());
    }

    private static List<Float> magnitudes(List<RolledEffect> effects) {
        return effects.stream().map(RolledEffect::magnitude).toList();
    }

    private static JSONArray csvRows(String dataPath) throws Exception {
        String csv = Files.readString(RealSkillData.projectRoot().resolve(dataPath), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }
}
