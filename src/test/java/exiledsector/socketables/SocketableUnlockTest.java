package exiledsector.socketables;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.intel.misc.GateHaulerIntel;
import exiledsector.skills.npc.RealSkillData;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SocketableUnlockTest {

    private final Map<String, Object> globals = new HashMap<>();
    private final List<StarSystemAPI> systems = new ArrayList<>();
    private SectorAPI sector;
    private IntelManagerAPI intel;
    private MutableCharacterStatsAPI player;

    @BeforeEach
    void setUp() throws Exception {
        sector = mock(SectorAPI.class);
        MemoryAPI memory = memoryOver(globals);
        when(sector.getMemoryWithoutUpdate()).thenReturn(memory);
        intel = mock(IntelManagerAPI.class);
        when(sector.getIntelManager()).thenReturn(intel);
        when(sector.getStarSystems()).thenReturn(systems);
        player = mock(MutableCharacterStatsAPI.class);
        when(player.getLevel()).thenReturn(15);
        when(sector.getPlayerStats()).thenReturn(player);
        SocketableDefinitions.register(new JSONArray()
                .put(SocketableFixtures.row("basic", "subroutine", "HULL_MULT:4:6"))
                .put(SocketableFixtures.row("open_unique", "team", "HULL_MULT:4:6").put("unique", "true"))
                .put(SocketableFixtures.row("vambrace", "subroutine", "HULL_MULT:4:6").put("unique", "true").put("unlock", "found_onslaught_mk1"))
                .put(SocketableFixtures.row("coil", "subroutine", "HULL_MULT:4:6").put("unique", "true").put("unlock", "found_gate_hauler"))
                .put(SocketableFixtures.row("circuit", "subroutine", "HULL_MULT:4:6").put("unique", "true").put("unlock", "found_planetkiller"))
                .put(SocketableFixtures.row("survivor", "officer", "HULL_MULT:4:6").put("unique", "true").put("unlock", "defeated_ziggurat"))
                .put(SocketableFixtures.row("mystery", "subroutine", "HULL_MULT:4:6").put("unique", "true").put("unlock", "found_something_else")));
    }

    @AfterEach
    void tearDown() {
        SocketableDefinitions.clear();
    }

    private static MemoryAPI memoryOver(Map<String, Object> values) {
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getBoolean(anyString())).thenAnswer(call -> Boolean.TRUE.equals(values.get(call.<String>getArgument(0))));
        doAnswer(call -> values.put(call.getArgument(0), call.getArgument(1))).when(memory).set(anyString(), any());
        return memory;
    }

    private boolean canDrop(String id) {
        return SocketableUnlock.canDrop(SocketableDefinitions.get(id), sector);
    }

    @Test
    void uniquesNeverDropBeforePlayerLevelFifteenButBasicsAlwaysCan() {
        when(player.getLevel()).thenReturn(14);

        assertFalse(canDrop("open_unique"));
        assertTrue(canDrop("basic"));

        when(player.getLevel()).thenReturn(15);
        assertTrue(canDrop("open_unique"));
    }

    @Test
    void theStoryUnlockCheckIgnoresThePlayerLevelGate() {
        when(player.getLevel()).thenReturn(1);

        assertTrue(SocketableUnlock.unlockConditionMet(SocketableDefinitions.get("open_unique"), sector));
        assertFalse(SocketableUnlock.unlockConditionMet(SocketableDefinitions.get("mystery"), sector));
        assertFalse(canDrop("open_unique"));
    }

    @Test
    void withNoSectorNoUniqueCanDrop() {
        assertFalse(SocketableUnlock.canDrop(SocketableDefinitions.get("open_unique"), null));
    }

    @Test
    void vambracePlatingWaitsForTheOnslaughtMkOneToBeFound() {
        assertFalse(canDrop("vambrace"));

        globals.put("$foundOneslaught", true);

        assertTrue(canDrop("vambrace"));
    }

    @Test
    void theGateHaulerCoilUnlocksFromItsIntelAndStaysUnlockedOnceTheIntelEnds() {
        assertFalse(canDrop("coil"));

        when(intel.hasIntelOfClass(GateHaulerIntel.class)).thenReturn(true);
        assertTrue(canDrop("coil"));

        when(intel.hasIntelOfClass(GateHaulerIntel.class)).thenReturn(false);
        assertTrue(canDrop("coil"));
    }

    @Test
    void aSystemWhereTheGateHaulerDeployedAGateAlsoCountsAsFindingIt() {
        StarSystemAPI system = mock(StarSystemAPI.class);
        Map<String, Object> systemMemory = new HashMap<>();
        MemoryAPI memory = memoryOver(systemMemory);
        when(system.getMemoryWithoutUpdate()).thenReturn(memory);
        systems.add(system);
        assertFalse(canDrop("coil"));

        systemMemory.put("$deployedGateHaulerHere", true);

        assertTrue(canDrop("coil"));
    }

    @Test
    void thePlanetkillerCircuitWaitsForTheFirstPlanetkillerToBeRecovered() {
        assertFalse(canDrop("circuit"));

        globals.put("$pk_recovered", true);

        assertTrue(canDrop("circuit"));
    }

    @Test
    void theAlphaSiteSurvivorWaitsForTheZigguratToBeDefeated() {
        assertFalse(canDrop("survivor"));

        globals.put("$defeatedZiggurat", true);

        assertTrue(canDrop("survivor"));
    }

    @Test
    void anUnknownConditionNeverUnlocks() {
        globals.put("$foundOneslaught", true);
        globals.put("$pk_recovered", true);
        when(intel.hasIntelOfClass(GateHaulerIntel.class)).thenReturn(true);

        assertFalse(canDrop("mystery"));
    }

    @Test
    void everyShippedConditionIsKnownAndTheEditorOffersExactlyThem() throws Exception {
        String csv = Files.readString(RealSkillData.projectRoot().resolve(SocketableDefinitions.DATA_PATH), StandardCharsets.UTF_8);
        JSONArray rows = CDL.toJSONArray(csv.replace("\r\n", "\n"));
        Map<String, String> unlocks = new HashMap<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String unlock = row.optString("unlock", "").trim();
            if (!unlock.isEmpty()) {
                assertNotNull(SocketableUnlock.byId(unlock), unlock);
                unlocks.put(row.getString("id"), unlock);
            }
        }
        assertEquals(Map.of("unique_vambrace_plating", "found_onslaught_mk1", "unique_gate_hauler_coil", "found_gate_hauler",
                "unique_planetkiller_circuit", "found_planetkiller", "unique_alpha_site_survivor", "defeated_ziggurat"), unlocks);

        String editor = Files.readString(RealSkillData.projectRoot().resolve("tools/skill_tree_editor.html"), StandardCharsets.UTF_8);
        Matcher list = Pattern.compile("var SOCKETABLE_UNLOCKS = (.*);").matcher(editor);
        assertTrue(list.find());
        Set<String> offered = new HashSet<>();
        Matcher option = Pattern.compile("\\['([a-z0-9_]*)',").matcher(list.group(1));
        while (option.find()) {
            offered.add(option.group(1));
        }
        Set<String> expected = new HashSet<>(Set.of(""));
        Arrays.stream(SocketableUnlock.values()).forEach(unlock -> expected.add(unlock.id()));
        assertEquals(expected, offered);
    }
}
