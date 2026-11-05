package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
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
}
