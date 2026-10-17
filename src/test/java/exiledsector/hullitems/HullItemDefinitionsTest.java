package exiledsector.hullitems;

import exiledsector.skills.npc.RealSkillData;
import org.json.CDL;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static exiledsector.hullitems.HullItemFixtures.MILITARY;
import static exiledsector.hullitems.HullItemFixtures.MILITARY_PREFIXES;
import static exiledsector.hullitems.HullItemFixtures.MILITARY_SUFFIXES;
import static exiledsector.hullitems.HullItemFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullItemDefinitionsTest {

    @AfterEach
    void tearDown() {
        HullItemDefinitions.clear();
        exiledsector.skills.skilleffect.EffectAliases.clear();
    }

    @Test
    void aRowBecomesADefinitionWithItsPrefixesAndSuffixesInOrderAndRangesLowToHigh() throws Exception {
        HullItemDefinition definition = HullItemFixtures.registerMilitary();

        assertEquals(SocketType.SUBROUTINE, definition.kind());
        assertEquals(20f, definition.rarity());
        assertEquals(List.of("FLUX_CAPACITY_MULT", "FLUX_DISSIPATION_MULT", "BEAM_WEAPON_DAMAGE_PERCENT"),
                definition.prefixes().stream().map(PoolEntry::effectName).toList());
        assertEquals(new PoolEntry("SHIELD_DAMAGE_TAKEN_MULT", -15f, -10f, 1f), definition.suffixes().get(0));
        assertEquals(6, definition.pool().size());
        assertTrue(definition.isPrefix("FLUX_CAPACITY_MULT") && definition.isSuffix("HULL_MULT"));
        assertFalse(definition.isPrefix("HULL_MULT") || definition.isSuffix("FLUX_CAPACITY_MULT"));
    }

    @Test
    void reversedRangesAndExplicitWeightsAreAccepted() throws Exception {
        HullItemDefinitions.register(new JSONArray().put(row("x", "crew_quarters", "HULL_MULT:6:4:2.5")));

        assertEquals(new PoolEntry("HULL_MULT", 4f, 6f, 2.5f), HullItemDefinitions.get("x").pool().get(0));
    }

    @Test
    void badRowsAreSkippedWithoutLosingTheGoodOnes() throws Exception {
        HullItemDefinitions.register(new JSONArray()
                .put(row(MILITARY, "subroutine", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row("unknown_kind", "drone", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row("unknown_effect", "subroutine", "NOT_AN_EFFECT:1:2"))
                .put(row("not_a_number", "subroutine", "HULL_MULT:one:2"))
                .put(row("empty_pool", "subroutine", " ; "))
                .put(row("zero_weight", "subroutine", "HULL_MULT:1:2:0"))
                .put(row("duplicate_effect", "subroutine", "HULL_MULT:1:2; HULL_MULT:3:4"))
                .put(row("both_sides", "subroutine", "HULL_MULT:1:2", "HULL_MULT:3:4")));

        assertNotNull(HullItemDefinitions.get(MILITARY));
        assertEquals(1, HullItemDefinitions.all().size());
        assertNull(HullItemDefinitions.get("unknown_kind"));
    }

    @Test
    void anUnknownEffectIsDroppedFromThePoolWithoutLosingTheDefinition() throws Exception {
        HullItemDefinitions.register(new JSONArray().put(row("x", "crew_quarters", "NOT_AN_EFFECT:1:2; HULL_MULT:4:6")));

        assertEquals(List.of(new PoolEntry("HULL_MULT", 4f, 6f, 1f)), HullItemDefinitions.get("x").pool());
    }

    @Test
    void aRenamedEffectInAPoolResolvesToItsCurrentName() throws Exception {
        exiledsector.skills.skilleffect.EffectAliases.register(new JSONArray()
                .put(new org.json.JSONObject().put("alias", "OLD_HULL_MULT").put("effect", "HULL_MULT")));

        HullItemDefinitions.register(new JSONArray().put(row("x", "crew_quarters", "OLD_HULL_MULT:4:6")));

        assertEquals("HULL_MULT", HullItemDefinitions.get("x").pool().get(0).effectName());
    }

    @Test
    void aBlankNameFallsBackToTheId() throws Exception {
        HullItemDefinitions.register(new JSONArray().put(row("nameless", "crew_quarters", "HULL_MULT:4:6").put("name", "")));

        assertEquals("nameless", HullItemDefinitions.get("nameless").name());
    }

    @Test
    void socketTypeIdsRoundTripAndLegacyKindsAreRejected() {
        for (SocketType type : SocketType.values()) {
            assertEquals(type, SocketType.byId(type.id()));
        }
        for (String legacyKind : List.of("officer", "team", "ai_core", "drone")) {
            assertThrows(IllegalArgumentException.class, () -> SocketType.byId(legacyKind));
            assertNull(SocketType.byIdOrNull(legacyKind));
        }
        assertEquals(List.of(SocketType.SUBROUTINE), Arrays.stream(SocketType.values()).filter(type -> !type.isFramework()).toList());
        assertEquals(9, SocketType.frameworkTypes().size());
    }

    @Test
    void aHullItemsKindFollowsItsDefinition() throws Exception {
        HullItemDefinitions.register(new JSONArray().put(row("bridge_item", "bridge", "HULL_MULT:4:6")));

        assertEquals(SocketType.BRIDGE, new HullItem("a", "bridge_item", 1L, List.of()).kind());
        assertNull(new HullItem("b", "not_loaded", 1L, List.of()).kind());
    }

    @Test
    void aRowWithALegacyKindIsSkipped() throws Exception {
        HullItemDefinitions.register(new JSONArray().put(row("old_team", "crew_quarters", "HULL_MULT:4:6").put("kind", "team")));

        assertNull(HullItemDefinitions.get("old_team"));
    }

    @Test
    void theShippedDefinitionsAllLoadAndAreTheThreeDomainSubroutines() throws Exception {
        String csv = Files.readString(RealSkillData.projectRoot().resolve(HullItemDefinitions.DATA_PATH), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        JSONArray rows = CDL.toJSONArray(csv);

        HullItemDefinitions.register(rows);

        assertEquals(rows.length(), HullItemDefinitions.all().size());
        for (String variant : List.of("military", "industrial", "consumer")) {
            HullItemDefinition definition = HullItemDefinitions.get("domain_subroutine_" + variant);
            assertNotNull(definition, variant);
            assertEquals(SocketType.SUBROUTINE, definition.kind());
            assertTrue(definition.prefixes().size() >= 2 && definition.suffixes().size() >= 2,
                    variant + " needs two prefixes and two suffixes for a four-effect roll");
        }
        for (HullItemDefinition definition : HullItemDefinitions.all()) {
            String icon = definition.icon();
            assertFalse(icon.startsWith("graphics/unused/") || icon.startsWith("graphics/description/"),
                    definition.id() + " uses " + icon + ", which build-common.ps1 leaves out of deploys and releases");
        }
    }
}
