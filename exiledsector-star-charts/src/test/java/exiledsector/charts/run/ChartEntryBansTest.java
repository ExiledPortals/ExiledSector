package exiledsector.charts.run;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartEntryBansTest {

    private static FleetMemberAPI member(HullSize hullSize, boolean civilian) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHullSize()).thenReturn(hullSize);
        when(member.getHullSpec()).thenReturn(hullSpec);
        when(member.isCivilian()).thenReturn(civilian);
        return member;
    }

    private static CampaignFleetAPI fleetOf(FleetMemberAPI... members) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleet.getFleetData()).thenReturn(fleetData);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(members));
        return fleet;
    }

    @Test
    void onlyCombatShipsOfABannedSizeKeepTheFleetOut() {
        CampaignFleetAPI fleet = fleetOf(member(HullSize.CAPITAL_SHIP, true), member(HullSize.CRUISER, false), member(HullSize.FRIGATE, false));

        assertEquals(Set.of(HullSize.CRUISER), ChartEntryBans.breached(Set.of(HullSize.CAPITAL_SHIP, HullSize.CRUISER), fleet));
        assertTrue(ChartEntryBans.breached(Set.of(HullSize.CAPITAL_SHIP, HullSize.DESTROYER), fleet).isEmpty());
        assertTrue(ChartEntryBans.breached(Set.of(), fleet).isEmpty());
        assertTrue(ChartEntryBans.breached(Set.of(HullSize.CRUISER), null).isEmpty());
    }
}
