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
import static exiledsector.socketables.SocketableFixtures.MILITARY_POOL;
import static exiledsector.socketables.SocketableFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketableDefinitionsTest {

    @AfterEach
    void tearDown() {
        SocketableDefinitions.clear();
    }

    @Test
    void aRowBecomesADefinitionWithItsPoolInOrderAndRangesLowToHigh() throws Exception {
        SocketableDefinition definition = SocketableFixtures.registerMilitary();

        assertEquals(SocketableKind.SUBROUTINE, definition.kind());
        assertEquals("military", definition.grade());
        assertEquals("high_tech", definition.alignment());
        assertEquals(20f, definition.rarity());
        assertEquals(5, definition.pool().size());
        assertEquals(new SocketableDefinition.PoolEntry("SHIELD_DAMAGE_TAKEN_MULT", -15f, -10f, 1f), definition.pool().get(0));
    }

    @Test
    void reversedRangesAndExplicitWeightsAreAccepted() throws Exception {
        SocketableDefinitions.register(new JSONArray().put(row("x", "team", "HULL_MULT:6:4:2.5")));

        assertEquals(new SocketableDefinition.PoolEntry("HULL_MULT", 4f, 6f, 2.5f), SocketableDefinitions.get("x").pool().get(0));
    }

    @Test
    void badRowsAreSkippedWithoutLosingTheGoodOnes() throws Exception {
        SocketableDefinitions.register(new JSONArray()
                .put(row(MILITARY, "subroutine", MILITARY_POOL))
                .put(row("unknown_kind", "drone", MILITARY_POOL))
                .put(row("unknown_effect", "subroutine", "NOT_AN_EFFECT:1:2"))
                .put(row("not_a_number", "subroutine", "HULL_MULT:one:2"))
                .put(row("empty_pool", "subroutine", " ; "))
                .put(row("zero_weight", "subroutine", "HULL_MULT:1:2:0")));

        assertNotNull(SocketableDefinitions.get(MILITARY));
        assertEquals(1, SocketableDefinitions.all().size());
        assertNull(SocketableDefinitions.get("unknown_kind"));
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
        for (String grade : List.of("military", "industrial", "consumer")) {
            SocketableDefinition definition = SocketableDefinitions.get("domain_subroutine_" + grade);
            assertNotNull(definition, grade);
            assertEquals(SocketableKind.SUBROUTINE, definition.kind());
            assertTrue(definition.pool().size() >= 4, grade + " needs room for a four-effect roll");
        }
    }
}
