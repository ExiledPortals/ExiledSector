package exiledsector.items;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItemUnlockTest {

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
        ItemDefinitions.register(new JSONArray()
                .put(ItemFixtures.row("basic", "subroutine", "HULL_MULT:4:6"))
                .put(ItemFixtures.row("open_unique", "crew_quarters", "HULL_MULT:4:6").put("unique", "true"))
                .put(ItemFixtures.row("vambrace", "subroutine", "HULL_MULT:4:6").put("unique", "true").put("unlock", "found_onslaught_mk1"))
                .put(ItemFixtures.row("coil", "subroutine", "HULL_MULT:4:6").put("unique", "true").put("unlock", "found_gate_hauler"))
                .put(ItemFixtures.row("circuit", "subroutine", "HULL_MULT:4:6").put("unique", "true").put("unlock", "found_planetkiller"))
                .put(ItemFixtures.row("survivor", "bridge", "HULL_MULT:4:6").put("unique", "true").put("unlock", "defeated_ziggurat"))
                .put(ItemFixtures.row("mystery", "subroutine", "HULL_MULT:4:6").put("unique", "true").put("unlock", "found_something_else")));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
    }

    private static MemoryAPI memoryOver(Map<String, Object> values) {
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getBoolean(anyString())).thenAnswer(call -> Boolean.TRUE.equals(values.get(call.<String>getArgument(0))));
        doAnswer(call -> values.put(call.getArgument(0), call.getArgument(1))).when(memory).set(anyString(), any());
        return memory;
    }

    private boolean canDrop(String id) {
        return ItemUnlock.canDrop(ItemDefinitions.get(id), sector);
    }

    @Test
    void hullSocketsOpenAtLevelFifteenUnlessHullUpgradesAreSwitchedOff() {
        assertFalse(ItemUnlock.sectionsOpen(14));
        assertTrue(ItemUnlock.sectionsOpen(15));

        HullUpgradesSetting.setEnabledForTests(false);
        try {
            assertFalse(ItemUnlock.sectionsOpen(50));
        } finally {
            HullUpgradesSetting.setEnabledForTests(HullUpgradesSetting.DEFAULT_ENABLED);
        }
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

        assertTrue(ItemUnlock.unlockConditionMet(ItemDefinitions.get("open_unique"), sector));
        assertFalse(ItemUnlock.unlockConditionMet(ItemDefinitions.get("mystery"), sector));
        assertFalse(canDrop("open_unique"));
    }

    @Test
    void withNoSectorNoUniqueCanDrop() {
        assertFalse(ItemUnlock.canDrop(ItemDefinitions.get("open_unique"), null));
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
    void everyShippedConditionIsKnown() throws Exception {
        JSONArray rows = new JSONArray();
        for (String dataPath : List.of(ItemDefinitions.SUBROUTINES_PATH, ItemDefinitions.HULL_ITEMS_PATH)) {
            String csv = Files.readString(RealSkillData.projectRoot().resolve(dataPath), StandardCharsets.UTF_8);
            JSONArray fileRows = CDL.toJSONArray(csv.replace("\r\n", "\n"));
            for (int i = 0; i < fileRows.length(); i++) {
                rows.put(fileRows.getJSONObject(i));
            }
        }
        Map<String, String> unlocks = new HashMap<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String unlock = row.optString("unlock", "").trim();
            if (!unlock.isEmpty()) {
                assertNotNull(ItemUnlock.byId(unlock), unlock);
                unlocks.put(row.getString("id"), unlock);
            }
        }
        assertEquals(Map.of("unique_vambrace_plating", "found_onslaught_mk1", "unique_gate_hauler_coil", "found_gate_hauler",
                "unique_planetkiller_circuit", "found_planetkiller", "unique_alpha_site_survivor", "defeated_ziggurat"), unlocks);
    }
}
