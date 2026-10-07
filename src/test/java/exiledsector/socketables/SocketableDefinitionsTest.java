package exiledsector.socketables;

import exiledsector.skills.npc.RealSkillData;
import org.json.CDL;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static exiledsector.socketables.SocketableFixtures.MILITARY;
import static exiledsector.socketables.SocketableFixtures.MILITARY_PREFIXES;
import static exiledsector.socketables.SocketableFixtures.MILITARY_SUFFIXES;
import static exiledsector.socketables.SocketableFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketableDefinitionsTest {

    @AfterEach
    void tearDown() {
        SocketableDefinitions.clear();
        exiledsector.skills.skilleffect.EffectAliases.clear();
    }

    @Test
    void aRowBecomesADefinitionWithItsPrefixesAndSuffixesInOrderAndRangesLowToHigh() throws Exception {
        SocketableDefinition definition = SocketableFixtures.registerMilitary();

        assertEquals(SocketableKind.SUBROUTINE, definition.kind());
        assertEquals(20f, definition.rarity());
        assertEquals(List.of("FLUX_CAPACITY_MULT", "FLUX_DISSIPATION_MULT", "BEAM_WEAPON_DAMAGE_PERCENT"),
                definition.prefixes().stream().map(SocketableDefinition.PoolEntry::effectName).toList());
        assertEquals(new SocketableDefinition.PoolEntry("SHIELD_DAMAGE_TAKEN_MULT", -15f, -10f, 1f), definition.suffixes().get(0));
        assertEquals(6, definition.pool().size());
        assertTrue(definition.isPrefix("FLUX_CAPACITY_MULT") && definition.isSuffix("HULL_MULT"));
        assertFalse(definition.isPrefix("HULL_MULT") || definition.isSuffix("FLUX_CAPACITY_MULT"));
    }

    @Test
    void reversedRangesAndExplicitWeightsAreAccepted() throws Exception {
        SocketableDefinitions.register(new JSONArray().put(row("x", "team", "HULL_MULT:6:4:2.5")));

        assertEquals(new SocketableDefinition.PoolEntry("HULL_MULT", 4f, 6f, 2.5f), SocketableDefinitions.get("x").pool().get(0));
    }

    @Test
    void badRowsAreSkippedWithoutLosingTheGoodOnes() throws Exception {
        SocketableDefinitions.register(new JSONArray()
                .put(row(MILITARY, "subroutine", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row("unknown_kind", "drone", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row("unknown_effect", "subroutine", "NOT_AN_EFFECT:1:2"))
                .put(row("not_a_number", "subroutine", "HULL_MULT:one:2"))
                .put(row("empty_pool", "subroutine", " ; "))
                .put(row("zero_weight", "subroutine", "HULL_MULT:1:2:0"))
                .put(row("duplicate_effect", "subroutine", "HULL_MULT:1:2; HULL_MULT:3:4"))
                .put(row("both_sides", "subroutine", "HULL_MULT:1:2", "HULL_MULT:3:4")));

        assertNotNull(SocketableDefinitions.get(MILITARY));
        assertEquals(1, SocketableDefinitions.all().size());
        assertNull(SocketableDefinitions.get("unknown_kind"));
    }

    @Test
    void anUnknownEffectIsDroppedFromThePoolWithoutLosingTheDefinition() throws Exception {
        SocketableDefinitions.register(new JSONArray().put(row("x", "team", "NOT_AN_EFFECT:1:2; HULL_MULT:4:6")));

        assertEquals(List.of(new SocketableDefinition.PoolEntry("HULL_MULT", 4f, 6f, 1f)), SocketableDefinitions.get("x").pool());
    }

    @Test
    void aRenamedEffectInAPoolResolvesToItsCurrentName() throws Exception {
        exiledsector.skills.skilleffect.EffectAliases.register(new JSONArray()
                .put(new org.json.JSONObject().put("alias", "OLD_HULL_MULT").put("effect", "HULL_MULT")));

        SocketableDefinitions.register(new JSONArray().put(row("x", "team", "OLD_HULL_MULT:4:6")));

        assertEquals("HULL_MULT", SocketableDefinitions.get("x").pool().get(0).effectName());
    }

    @Test
    void aBlankNameFallsBackToTheId() throws Exception {
        SocketableDefinitions.register(new JSONArray().put(row("nameless", "team", "HULL_MULT:4:6").put("name", "")));

        assertEquals("nameless", SocketableDefinitions.get("nameless").name());
    }

    @Test
    void everyKindIdMapsToItsOwnClass() {
        assertInstanceOf(Subroutine.class, SocketableKind.byId("subroutine").create("a", "d", 1L, List.of()));
        assertInstanceOf(Officer.class, SocketableKind.byId("officer").create("a", "d", 1L, List.of()));
        assertInstanceOf(Team.class, SocketableKind.byId("team").create("a", "d", 1L, List.of()));
        assertInstanceOf(AiCore.class, SocketableKind.byId("ai_core").create("a", "d", 1L, List.of()));
        for (SocketableKind kind : SocketableKind.values()) {
            assertEquals(kind, kind.create("a", "d", 1L, List.of()).kind());
        }
        assertThrows(IllegalArgumentException.class, () -> SocketableKind.byId("drone"));
    }

    @Test
    void theShippedDefinitionsAllLoadAndAreTheThreeDomainSubroutines() throws Exception {
        String csv = Files.readString(RealSkillData.projectRoot().resolve(SocketableDefinitions.DATA_PATH), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        JSONArray rows = CDL.toJSONArray(csv);

        SocketableDefinitions.register(rows);

        assertEquals(rows.length(), SocketableDefinitions.all().size());
        for (String variant : List.of("military", "industrial", "consumer")) {
            SocketableDefinition definition = SocketableDefinitions.get("domain_subroutine_" + variant);
            assertNotNull(definition, variant);
            assertEquals(SocketableKind.SUBROUTINE, definition.kind());
            assertTrue(definition.prefixes().size() >= 2 && definition.suffixes().size() >= 2,
                    variant + " needs two prefixes and two suffixes for a four-effect roll");
        }
        for (SocketableDefinition definition : SocketableDefinitions.all()) {
            String icon = definition.icon();
            assertFalse(icon.startsWith("graphics/unused/") || icon.startsWith("graphics/description/"),
                    definition.id() + " uses " + icon + ", which build-common.ps1 leaves out of deploys and releases");
        }
    }
}
