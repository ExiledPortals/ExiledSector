package exiledsector.skills.unlock;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.BeforeEach;
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

class MothershipGateTest {

    private final MemoryAPI playerMemory = mock(MemoryAPI.class);
    private final SectorAPI sector = mock(SectorAPI.class);

    @BeforeEach
    void setUp() {
        when(sector.getPlayerMemoryWithoutUpdate()).thenReturn(playerMemory);
    }

    @Test
    void theGateReadsItsOwnPlayerFlag() {
        assertFalse(MothershipGate.isDefeated(sector));
        assertFalse(MothershipGate.isDefeated(null));
        when(playerMemory.getBoolean(MothershipGate.DEFEATED_FLAG)).thenReturn(true);
        assertTrue(MothershipGate.isDefeated(sector));
        assertTrue(UnlockConditionType.MOTHERSHIP_DEFEATED.modGate().isOpen(sector, null, false));
        assertFalse(UnlockConditionType.MOTHERSHIP_DEFEATED.modGate().isOpen(sector, null, true));
    }

    @Test
    void destroyingOrDisablingAMothershipInAWonBattleOpensTheGate() {
        MothershipGate.recordBattle(sector, result(true, List.of(member(MothershipGate.MOTHERSHIP_HULL_ID)), List.of(), List.of()));
        verify(playerMemory).set(MothershipGate.DEFEATED_FLAG, true);
    }

    @Test
    void aMothershipStillListedInTheBeatenFleetOpensTheGate() {
        MothershipGate.recordBattle(sector, result(true, List.of(), List.of(), List.of(member("guardian"), member(MothershipGate.MOTHERSHIP_HULL_ID))));
        verify(playerMemory).set(MothershipGate.DEFEATED_FLAG, true);
    }

    @Test
    void losingToAMothershipOrBeatingOnlyDronesLeavesTheGateShut() {
        MothershipGate.recordBattle(sector, result(false, List.of(member(MothershipGate.MOTHERSHIP_HULL_ID)), List.of(), List.of()));
        MothershipGate.recordBattle(sector, result(true, List.of(member("guardian")), List.of(member("defender")), List.of(member("picket"))));
        verify(playerMemory, never()).set(anyString(), any());
    }

    @Test
    void aWinReopensTheGateWhenRelocked() {
        Map<String, Object> persistentData = new HashMap<>();
        when(sector.getPersistentData()).thenReturn(persistentData);
        when(playerMemory.getBoolean(MothershipGate.DEFEATED_FLAG)).thenReturn(true);
        RelockedGates.relock(sector, List.of(UnlockConditionType.MOTHERSHIP_DEFEATED));

        MothershipGate.recordBattle(sector, result(true, List.of(), List.of(member(MothershipGate.MOTHERSHIP_HULL_ID)), List.of()));

        assertFalse(RelockedGates.isRelocked(sector, UnlockConditionType.MOTHERSHIP_DEFEATED));
    }

    private static EngagementResultAPI result(boolean playerWon, List<FleetMemberAPI> destroyed, List<FleetMemberAPI> disabled,
                                              List<FleetMemberAPI> remaining) {
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenReturn(remaining);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getFleetData()).thenReturn(fleetData);
        EngagementResultForFleetAPI losers = mock(EngagementResultForFleetAPI.class);
        when(losers.getDestroyed()).thenReturn(destroyed);
        when(losers.getDisabled()).thenReturn(disabled);
        when(losers.getFleet()).thenReturn(fleet);
        EngagementResultAPI result = mock(EngagementResultAPI.class);
        when(result.didPlayerWin()).thenReturn(playerWon);
        when(result.getLoserResult()).thenReturn(losers);
        return result;
    }

    private static FleetMemberAPI member(String hullId) {
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHullId()).thenReturn(hullId);
        when(hullSpec.getBaseHullId()).thenReturn(hullId);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getHullSpec()).thenReturn(hullSpec);
        return member;
    }
}
