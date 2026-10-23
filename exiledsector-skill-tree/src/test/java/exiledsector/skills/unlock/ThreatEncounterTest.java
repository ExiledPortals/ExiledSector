package exiledsector.skills.unlock;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
}
