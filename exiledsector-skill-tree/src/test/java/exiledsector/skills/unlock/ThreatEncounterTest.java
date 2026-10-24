package exiledsector.skills.unlock;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThreatEncounterTest {

    private static SectorAPI sectorWherePlayerMemoryHasThreat(boolean encountered) {
        SectorAPI sector = mock(SectorAPI.class);
        MemoryAPI playerMemory = mock(MemoryAPI.class);
        when(sector.getPlayerMemoryWithoutUpdate()).thenReturn(playerMemory);
        when(playerMemory.getBoolean(ThreatEncounter.ENCOUNTERED_THREAT_KEY)).thenReturn(encountered);
        return sector;
    }

    @Test
    void theGateOpensOnceVanillaRecordsABattleAgainstAThreatFleet() {
        assertTrue(ThreatEncounter.isFought(sectorWherePlayerMemoryHasThreat(true)));
    }

    @Test
    void theGateStaysShutBeforeAnyThreatBattle() {
        assertFalse(ThreatEncounter.isFought(sectorWherePlayerMemoryHasThreat(false)));
    }

    @Test
    void noSectorMeansNotFought() {
        assertFalse(ThreatEncounter.isFought(null));
    }

    @Test
    void theThreatGateIsAParameterlessGateThatAlwaysApplies() {
        assertTrue(UnlockConditionType.THREAT_FOUGHT.isModGate());
        assertTrue(UnlockConditionType.THREAT_FOUGHT.modGate().isOpen(sectorWherePlayerMemoryHasThreat(true), null, false));
        assertFalse(UnlockConditionType.THREAT_FOUGHT.modGate().isOpen(sectorWherePlayerMemoryHasThreat(false), null, false));
    }

    @Test
    void aThreatBattleReopensARelockedGateWithoutTouchingVanillasFlag() {
        SectorAPI sector = sectorWherePlayerMemoryHasThreat(true);
        Map<String, Object> persistentData = new HashMap<>();
        when(sector.getPersistentData()).thenReturn(persistentData);
        RelockedGates.relock(sector, List.of(UnlockConditionType.THREAT_FOUGHT));

        ThreatEncounter.recordBattle(sector, battleAgainst("hegemony"));
        assertTrue(RelockedGates.isRelocked(sector, UnlockConditionType.THREAT_FOUGHT));

        ThreatEncounter.recordBattle(sector, battleAgainst("threat"));
        assertFalse(RelockedGates.isRelocked(sector, UnlockConditionType.THREAT_FOUGHT));
        verify(sector.getPlayerMemoryWithoutUpdate(), never()).set(anyString(), any());
    }

    private static BattleAPI battleAgainst(String factionId) {
        FactionAPI faction = mock(FactionAPI.class);
        when(faction.getId()).thenReturn(factionId);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getFaction()).thenReturn(faction);
        BattleAPI battle = mock(BattleAPI.class);
        when(battle.getNonPlayerSideSnapshot()).thenReturn(List.of(fleet));
        return battle;
    }
}
