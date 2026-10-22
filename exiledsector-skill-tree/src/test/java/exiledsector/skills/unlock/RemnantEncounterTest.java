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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemnantEncounterTest {

    private final MemoryAPI playerMemory = mock(MemoryAPI.class);
    private final SectorAPI sector = mock(SectorAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        when(sector.getPlayerMemoryWithoutUpdate()).thenReturn(playerMemory);
        NpcFactionVolumes.register(new JSONArray()
                .put(new JSONObject().put("faction", "remnant").put("region", "REDACTED"))
                .put(new JSONObject().put("faction", "hegemony").put("region", "hegemony")));
    }

    @AfterEach
    void tearDown() {
        NpcFactionVolumes.clear();
    }

    @Test
    void theGateStaysShutUntilTheFlagIsSet() {
        assertFalse(RemnantEncounter.isFought(sector));
        when(playerMemory.getBoolean(RemnantEncounter.FOUGHT_REMNANT_KEY)).thenReturn(true);
        assertTrue(RemnantEncounter.isFought(sector));
        assertFalse(RemnantEncounter.isFought(null));
    }

    @Test
    void aBattleAgainstARemnantVolumeFleetSetsTheFlag() {
        RemnantEncounter.recordBattle(sector, battleAgainst("hegemony", "remnant"));
        verify(playerMemory).set(RemnantEncounter.FOUGHT_REMNANT_KEY, true);
    }

    @Test
    void aBattleWithoutRemnantFleetsLeavesTheFlagAlone() {
        RemnantEncounter.recordBattle(sector, battleAgainst("hegemony"));
        verify(playerMemory, never()).set(anyString(), eq(true));
    }

    @Test
    void theRemnantGateIsAParameterlessGateThatAlwaysApplies() {
        assertTrue(UnlockConditionType.REMNANT_FOUGHT.isModGate());
        assertFalse(UnlockConditionType.REMNANT_FOUGHT.modGate().isOpen(sector, false));
        when(playerMemory.getBoolean(RemnantEncounter.FOUGHT_REMNANT_KEY)).thenReturn(true);
        assertTrue(UnlockConditionType.REMNANT_FOUGHT.modGate().isOpen(sector, false));
        assertFalse(UnlockConditionType.REMNANT_FOUGHT.modGate().isOpen(sector, true));
    }

    @Test
    void aRemnantBattleReopensARelockedGate() {
        Map<String, Object> persistentData = new HashMap<>();
        when(sector.getPersistentData()).thenReturn(persistentData);
        when(playerMemory.getBoolean(RemnantEncounter.FOUGHT_REMNANT_KEY)).thenReturn(true);
        RelockedGates.relock(sector, List.of(UnlockConditionType.REMNANT_FOUGHT));

        RemnantEncounter.recordBattle(sector, battleAgainst("hegemony"));
        assertTrue(RelockedGates.isRelocked(sector, UnlockConditionType.REMNANT_FOUGHT));

        RemnantEncounter.recordBattle(sector, battleAgainst("remnant"));
        assertFalse(RelockedGates.isRelocked(sector, UnlockConditionType.REMNANT_FOUGHT));
        assertTrue(persistentData.isEmpty());
    }

    private static BattleAPI battleAgainst(String... factionIds) {
        List<CampaignFleetAPI> fleets = Arrays.stream(factionIds).map(RemnantEncounterTest::fleetOf).toList();
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
