package exiledsector.skills.unlock;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.skills.npc.NpcFactionVolumes;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FactionEncounterGateTest {

    private final MemoryAPI playerMemory = mock(MemoryAPI.class);
    private final SectorAPI sector = mock(SectorAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        when(sector.getPlayerMemoryWithoutUpdate()).thenReturn(playerMemory);
        NpcFactionVolumes.register(new JSONArray()
                .put(new JSONObject().put("faction", "remnant").put("volume", "REDACTED"))
                .put(new JSONObject().put("faction", "hegemony").put("volume", "hegemony")));
    }

    @AfterEach
    void tearDown() {
        NpcFactionVolumes.clear();
    }

    @Test
    void theGatesKeepTheirSavedFlagKeys() {
        assertEquals("$encounteredThreat", FactionEncounterGate.THREAT.flagKey());
        assertEquals("$exiledSector_foughtRemnant", FactionEncounterGate.REMNANT.flagKey());
        assertEquals("$encounteredDweller", FactionEncounterGate.DWELLER.flagKey());
        assertEquals("$exiledSector_foundOmega", FactionEncounterGate.OMEGA.flagKey());
    }

    @Test
    void eachGateReadsItsOwnPlayerFlag() {
        for (FactionEncounterGate gate : FactionEncounterGate.ALL) {
            assertFalse(gate.isMet(sector), gate.gateName());
            assertFalse(gate.isMet(null), gate.gateName());
        }
        when(playerMemory.getBoolean(FactionEncounterGate.REMNANT.flagKey())).thenReturn(true);
        assertTrue(FactionEncounterGate.REMNANT.isMet(sector));
        assertFalse(FactionEncounterGate.THREAT.isMet(sector));
    }

    @Test
    void everyGateIsAParameterlessConditionThatAlwaysApplies() {
        for (FactionEncounterGate gate : FactionEncounterGate.ALL) {
            UnlockConditionType conditionType = gate.conditionType();
            assertTrue(conditionType.isModGate(), gate.gateName());
            assertFalse(conditionType.modGate().isOpen(sector, null, false), gate.gateName());
            when(playerMemory.getBoolean(gate.flagKey())).thenReturn(true);
            assertTrue(conditionType.modGate().isOpen(sector, null, false), gate.gateName());
            assertFalse(conditionType.modGate().isOpen(sector, null, true), gate.gateName());
        }
    }

    @Test
    void aBattleAgainstARemnantVolumeFleetSetsTheRemnantFlagWonOrLost() {
        FactionEncounterGate.REMNANT.recordBattle(sector, battleAgainst("hegemony", "remnant"), false);
        verify(playerMemory).set(FactionEncounterGate.REMNANT.flagKey(), true);
    }

    @Test
    void aBattleWithoutTheGatesFactionLeavesItsFlagAlone() {
        FactionEncounterGate.REMNANT.recordBattle(sector, battleAgainst("hegemony"), true);
        FactionEncounterGate.OMEGA.recordBattle(sector, battleAgainst("remnant"), true);
        verify(playerMemory, never()).set(anyString(), any());
    }

    @Test
    void beatingAnOmegaFleetOpensTheOmegaGateButLosingDoesNot() {
        FactionEncounterGate.OMEGA.recordBattle(sector, battleAgainst("omega"), false);
        verify(playerMemory, never()).set(anyString(), any());

        FactionEncounterGate.OMEGA.recordBattle(sector, battleAgainst("hegemony", "omega"), true);
        verify(playerMemory).set(FactionEncounterGate.OMEGA.flagKey(), true);
    }

    @Test
    void threatAndDwellerBattlesReopenARelockedGateWithoutTouchingVanillasFlags() {
        Map<String, Object> persistentData = new HashMap<>();
        when(sector.getPersistentData()).thenReturn(persistentData);
        when(playerMemory.getBoolean(anyString())).thenReturn(true);
        RelockedGates.relock(sector, List.of(UnlockConditionType.THREAT_FOUGHT, UnlockConditionType.DWELLER_FOUGHT));

        FactionEncounterGate.THREAT.recordBattle(sector, battleAgainst("hegemony"), true);
        assertTrue(RelockedGates.isRelocked(sector, UnlockConditionType.THREAT_FOUGHT));

        FactionEncounterGate.THREAT.recordBattle(sector, battleAgainst("threat"), false);
        FactionEncounterGate.DWELLER.recordBattle(sector, battleAgainst("dweller"), false);
        assertFalse(RelockedGates.isRelocked(sector, UnlockConditionType.THREAT_FOUGHT));
        assertFalse(RelockedGates.isRelocked(sector, UnlockConditionType.DWELLER_FOUGHT));
        verify(playerMemory, never()).set(anyString(), any());
    }

    @Test
    void anOwnedFlagGateReopensWhenRelocked() {
        Map<String, Object> persistentData = new HashMap<>();
        when(sector.getPersistentData()).thenReturn(persistentData);
        when(playerMemory.getBoolean(FactionEncounterGate.REMNANT.flagKey())).thenReturn(true);
        RelockedGates.relock(sector, List.of(UnlockConditionType.REMNANT_FOUGHT));

        FactionEncounterGate.REMNANT.recordBattle(sector, battleAgainst("remnant"), true);

        assertFalse(RelockedGates.isRelocked(sector, UnlockConditionType.REMNANT_FOUGHT));
        assertTrue(persistentData.isEmpty());
    }

    private static BattleAPI battleAgainst(String... factionIds) {
        List<CampaignFleetAPI> fleets = Arrays.stream(factionIds).map(FactionEncounterGateTest::fleetOf).toList();
        BattleAPI battle = mock(BattleAPI.class);
        when(battle.getNonPlayerSideSnapshot()).thenReturn(fleets);
        return battle;
    }

    private static CampaignFleetAPI fleetOf(String factionId) {
        FactionAPI faction = mock(FactionAPI.class);
        when(faction.getId()).thenReturn(factionId);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getFaction()).thenReturn(faction);
        return fleet;
    }
}
