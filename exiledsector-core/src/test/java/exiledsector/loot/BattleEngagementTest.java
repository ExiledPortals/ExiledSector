package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BattleEngagementTest {

    @Test
    void anUnfoughtEngagementCountsEveryShipOfTheFleetAsLostAtBaseDifficulty() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        FleetMemberAPI destroyer = mock(FleetMemberAPI.class);
        FleetMemberAPI cruiser = mock(FleetMemberAPI.class);
        when(destroyer.getDeploymentPointsCost()).thenReturn(10f);
        when(cruiser.getDeploymentPointsCost()).thenReturn(18f);

        BattleEngagement engagement = BattleEngagement.unfought(fleet, List.of(destroyer, cruiser));

        assertEquals(28f, engagement.defeatedDeploymentPoints(), 0.001f);
        assertEquals(1f, engagement.difficultyMultiplier(), 0.001f);
        assertEquals(Map.of(fleet, List.of(destroyer, cruiser)), engagement.lostEnemiesBySourceFleet());
        assertSame(fleet, engagement.sourceFleetOf(cruiser));
    }

    @Test
    void shipsLostByAFleetFlyingARunOnlyCopyCountForTheRealFactionBehindIt() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        FactionAPI copy = mock(FactionAPI.class);
        when(copy.getId()).thenReturn("exiledSector_skirmish_attacker");
        when(fleet.getFaction()).thenReturn(copy);
        MemoryAPI memory = mock(MemoryAPI.class);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        FleetMemberAPI frigate = mock(FleetMemberAPI.class);
        when(frigate.getDeploymentPointsCost()).thenReturn(5f);
        BattleEngagement engagement = BattleEngagement.unfought(fleet, List.of(frigate));

        assertEquals(Map.of("exiledSector_skirmish_attacker", 5f), engagement.deploymentPointsLostByFaction());

        when(memory.getString(FleetFactions.REAL_FACTION_MEMORY_KEY)).thenReturn("hegemony");
        assertEquals(Map.of("hegemony", 5f), engagement.deploymentPointsLostByFaction());
    }
}
