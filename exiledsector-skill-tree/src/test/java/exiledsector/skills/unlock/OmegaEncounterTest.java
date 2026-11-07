package exiledsector.skills.unlock;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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

class OmegaEncounterTest {

    private final SectorAPI sector = mock(SectorAPI.class);
    private final MemoryAPI playerMemory = mock(MemoryAPI.class);

    OmegaEncounterTest() {
        when(sector.getPlayerMemoryWithoutUpdate()).thenReturn(playerMemory);
    }

    @Test
    void theGateReadsItsOwnPlayerFlag() {
        assertFalse(OmegaEncounter.isFound(sector));
        when(playerMemory.getBoolean(OmegaEncounter.FOUND_OMEGA_KEY)).thenReturn(true);
        assertTrue(OmegaEncounter.isFound(sector));
        assertFalse(OmegaEncounter.isFound(null));
    }

    @Test
    void theOmegaGateIsAParameterlessGateThatAlwaysApplies() {
        assertTrue(UnlockConditionType.OMEGA_FOUND.isModGate());
        assertFalse(UnlockConditionType.OMEGA_FOUND.modGate().isOpen(sector, null, false));
        when(playerMemory.getBoolean(OmegaEncounter.FOUND_OMEGA_KEY)).thenReturn(true);
        assertTrue(UnlockConditionType.OMEGA_FOUND.modGate().isOpen(sector, null, false));
        assertFalse(UnlockConditionType.OMEGA_FOUND.modGate().isOpen(sector, null, true));
    }

    @Test
    void beatingAnOmegaFleetOpensTheGate() {
        OmegaEncounter.recordBattle(sector, battleAgainst("hegemony", "omega"), true);
        verify(playerMemory).set(OmegaEncounter.FOUND_OMEGA_KEY, true);
    }

    @Test
    void losingToOmegaOrBeatingAnyoneElseLeavesItShut() {
        OmegaEncounter.recordBattle(sector, battleAgainst("omega"), false);
        OmegaEncounter.recordBattle(sector, battleAgainst("remnant"), true);
        verify(playerMemory, never()).set(anyString(), any());
    }

    @Test
    void openingTheAlphaSiteCacheOpensTheGate() {
        OmegaEncounter.recordCacheOpened(sector);
        verify(playerMemory).set(OmegaEncounter.FOUND_OMEGA_KEY, true);
    }

    @Test
    void anOmegaWinReopensARelockedGate() {
        Map<String, Object> persistentData = new HashMap<>();
        when(sector.getPersistentData()).thenReturn(persistentData);
        when(playerMemory.getBoolean(OmegaEncounter.FOUND_OMEGA_KEY)).thenReturn(true);
        RelockedGates.relock(sector, List.of(UnlockConditionType.OMEGA_FOUND));

        OmegaEncounter.recordBattle(sector, battleAgainst("omega"), true);

        assertFalse(RelockedGates.isRelocked(sector, UnlockConditionType.OMEGA_FOUND));
    }

    private static BattleAPI battleAgainst(String... factionIds) {
        BattleAPI battle = mock(BattleAPI.class);
        List<CampaignFleetAPI> fleets = new ArrayList<>();
        for (String factionId : factionIds) {
            FactionAPI faction = mock(FactionAPI.class);
            when(faction.getId()).thenReturn(factionId);
            CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
            when(fleet.getFaction()).thenReturn(faction);
            fleets.add(fleet);
        }
        when(battle.getNonPlayerSideSnapshot()).thenReturn(fleets);
        return battle;
    }
}
