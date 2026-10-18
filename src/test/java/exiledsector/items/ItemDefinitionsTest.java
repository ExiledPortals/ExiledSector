package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.npc.RealSkillData;
import org.json.CDL;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static exiledsector.items.ItemFixtures.MILITARY;
import static exiledsector.items.ItemFixtures.MILITARY_PREFIXES;
import static exiledsector.items.ItemFixtures.MILITARY_SUFFIXES;
import static exiledsector.items.ItemFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDefinitionsTest {

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        ItemModifiers.clear();
        exiledsector.skills.skilleffect.EffectAliases.clear();
    }

    @Test
    void aRowBecomesADefinitionWithItsPrefixesAndSuffixesInOrderAndRangesLowToHigh() throws Exception {
        ItemDefinition definition = ItemFixtures.registerMilitary();

        assertEquals(ItemKind.SUBROUTINE, definition.kind());
        assertEquals(20f, definition.rarity());
        assertEquals(List.of("FLUX_CAPACITY_MULT", "FLUX_DISSIPATION_MULT", "BEAM_WEAPON_DAMAGE_PERCENT"),
                definition.prefixes().stream().map(PoolEntry::effectName).toList());
        assertEquals(PoolEntry.of(ModifierSlot.SUFFIX, "SHIELD_DAMAGE_TAKEN_MULT", -15f, -10f, 1f), definition.suffixes().get(0));
        assertEquals(6, definition.pool().size());
        assertTrue(definition.isPrefix("FLUX_CAPACITY_MULT") && definition.isSuffix("HULL_MULT"));
        assertFalse(definition.isPrefix("HULL_MULT") || definition.isSuffix("FLUX_CAPACITY_MULT"));
    }

    @Test
    void reversedRangesAndExplicitWeightsAreAccepted() throws Exception {
        ItemDefinitions.register(new JSONArray().put(row("x", "crew_quarters", "HULL_MULT:6:4:2.5")));

        assertEquals(PoolEntry.of(ModifierSlot.PREFIX, "HULL_MULT", 4f, 6f, 2.5f), ItemDefinitions.get("x").pool().get(0));
    }

    @Test
    void badRowsAreSkippedWithoutLosingTheGoodOnes() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(row(MILITARY, "subroutine", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row("unknown_kind", "drone", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row("unknown_effect", "subroutine", "NOT_AN_EFFECT:1:2"))
                .put(row("not_a_number", "subroutine", "HULL_MULT:one:2"))
                .put(row("empty_pool", "subroutine", " ; "))
                .put(row("zero_weight", "subroutine", "HULL_MULT:1:2:0"))
                .put(row("duplicate_effect", "subroutine", "HULL_MULT:1:2; HULL_MULT:3:4"))
                .put(row("both_sides", "subroutine", "HULL_MULT:1:2", "HULL_MULT:3:4")));

        assertNotNull(ItemDefinitions.get(MILITARY));
        assertEquals(1, ItemDefinitions.all().size());
        assertNull(ItemDefinitions.get("unknown_kind"));
    }

    @Test
    void anUnknownEffectIsDroppedFromThePoolWithoutLosingTheDefinition() throws Exception {
        ItemDefinitions.register(new JSONArray().put(row("x", "crew_quarters", "NOT_AN_EFFECT:1:2; HULL_MULT:4:6")));

        assertEquals(List.of(PoolEntry.of(ModifierSlot.PREFIX, "HULL_MULT", 4f, 6f, 1f)), ItemDefinitions.get("x").pool());
    }

    @Test
    void aRenamedEffectInAPoolResolvesToItsCurrentName() throws Exception {
        exiledsector.skills.skilleffect.EffectAliases.register(new JSONArray()
                .put(new org.json.JSONObject().put("alias", "OLD_HULL_MULT").put("effect", "HULL_MULT")));

        ItemDefinitions.register(new JSONArray().put(row("x", "crew_quarters", "OLD_HULL_MULT:4:6")));

        assertEquals("HULL_MULT", ItemDefinitions.get("x").pool().get(0).effectName());
    }

    @Test
    void aBlankNameFallsBackToTheId() throws Exception {
        ItemDefinitions.register(new JSONArray().put(row("nameless", "crew_quarters", "HULL_MULT:4:6").put("name", "")));

        assertEquals("nameless", ItemDefinitions.get("nameless").name());
    }

    @Test
    void socketTypeIdsRoundTripAndLegacyKindsAreRejected() {
        for (ItemKind type : ItemKind.values()) {
            assertEquals(type, ItemKind.byId(type.id()));
        }
        for (String legacyKind : List.of("officer", "team", "ai_core", "drone")) {
            assertThrows(IllegalArgumentException.class, () -> ItemKind.byId(legacyKind));
            assertNull(ItemKind.byIdOrNull(legacyKind));
        }
        assertEquals(List.of(ItemKind.SUBROUTINE), Arrays.stream(ItemKind.values()).filter(type -> !type.fitsSection()).toList());
        assertEquals(9, ItemKind.sectionItemTypes().size());
    }

    @Test
    void aHullItemsKindFollowsItsDefinition() throws Exception {
        ItemDefinitions.register(new JSONArray().put(row("bridge_item", "bridge", "HULL_MULT:4:6")));

        assertEquals(ItemKind.BRIDGE, Item.withEffects("a", "bridge_item", 1L, List.of()).kind());
        assertNull(Item.withEffects("b", "not_loaded", 1L, List.of()).kind());
    }

    @Test
    void aRowWithALegacyKindIsSkipped() throws Exception {
        ItemDefinitions.register(new JSONArray().put(row("old_team", "crew_quarters", "HULL_MULT:4:6").put("section", "team")));

        assertNull(ItemDefinitions.get("old_team"));
    }

    @Test
    void theShippedDefinitionsAllLoadAndAreTheThreeDomainSubroutines() throws Exception {
        JSONArray subroutineRows = ItemFixtures.shippedRows(ItemDefinitions.SUBROUTINES_PATH);
        JSONArray hullItemRows = ItemFixtures.shippedRows(ItemDefinitions.HULL_ITEMS_PATH);

        ItemFixtures.registerShippedItems();

        assertEquals(subroutineRows.length() + hullItemRows.length(), ItemDefinitions.all().size());
        assertTrue(ItemDefinitions.all().stream().allMatch(definition -> definition.type() != null));
        assertEquals(subroutineRows.length(), ItemDefinitions.all().stream().filter(definition -> definition.type() == ItemType.SUBROUTINE).count());
        for (String variant : List.of("military", "industrial", "consumer")) {
            ItemDefinition definition = ItemDefinitions.get("domain_subroutine_" + variant);
            assertNotNull(definition, variant);
            assertEquals(ItemKind.SUBROUTINE, definition.kind());
            assertTrue(definition.prefixes().size() >= 2 && definition.suffixes().size() >= 2,
                    variant + " needs two prefixes and two suffixes for a four-effect roll");
        }
        for (ItemDefinition definition : ItemDefinitions.all()) {
            String icon = definition.icon();
            assertFalse(icon.startsWith("graphics/unused/") || icon.startsWith("graphics/description/"),
                    definition.id() + " uses " + icon + ", which build-common.ps1 leaves out of deploys and releases");
        }
    }
}
