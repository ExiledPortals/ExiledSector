package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartStationsTest {

    private static CampaignFleetAPI fleet(String factionId, boolean chartStation, boolean stationMode) {
        FactionAPI faction = mock(FactionAPI.class);
        when(faction.getId()).thenReturn(factionId);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getFaction()).thenReturn(faction);
        when(fleet.hasTag(ChartStations.STATION_TAG)).thenReturn(chartStation);
        when(fleet.isStationMode()).thenReturn(stationMode);
        return fleet;
    }

    private static LocationAPI systemWith(CampaignFleetAPI... fleets) {
        LocationAPI system = mock(LocationAPI.class);
        when(system.getFleets()).thenReturn(List.of(fleets));
        return system;
    }

    @Test
    void remnantChartStationsLeaveStationModeWhileThePlayerIsOutsideTheirChart() {
        CampaignFleetAPI nexus = fleet(Factions.REMNANTS, true, true);
        CampaignFleetAPI pirateStation = fleet(Factions.PIRATES, true, true);
        CampaignFleetAPI patrol = fleet(Factions.REMNANTS, false, false);
        CampaignFleetAPI otherStation = fleet(Factions.REMNANTS, false, true);

        ChartStations.matchStationModeTo(systemWith(nexus, pirateStation, patrol, otherStation), false);

        verify(nexus).setStationMode(false);
        verify(pirateStation, never()).setStationMode(anyBoolean());
        verify(patrol, never()).setStationMode(anyBoolean());
        verify(otherStation, never()).setStationMode(anyBoolean());
    }

    @Test
    void chartStationsReturnToStationModeOnceThePlayerIsInside() {
        CampaignFleetAPI resting = fleet(Factions.REMNANTS, true, false);
        CampaignFleetAPI alreadyActive = fleet(Factions.REMNANTS, true, true);

        ChartStations.matchStationModeTo(systemWith(resting, alreadyActive), true);

        verify(resting).setStationMode(true);
        verify(alreadyActive, never()).setStationMode(anyBoolean());
    }

    @Test
    void aStationThatStoppedBeingRemnantGoesBackToStationMode() {
        CampaignFleetAPI joinedThePlayer = fleet(Factions.INDEPENDENT, true, false);

        ChartStations.matchStationModeTo(systemWith(joinedThePlayer), false);

        verify(joinedThePlayer).setStationMode(true);
    }

    @Test
    void aChartStationCountsAsAStationOutsideStationMode() {
        assertTrue(ChartStations.isStation(fleet(Factions.REMNANTS, true, false)));
        assertTrue(ChartStations.isStation(fleet(Factions.PIRATES, false, true)));
        assertFalse(ChartStations.isStation(fleet(Factions.PIRATES, false, false)));
        assertFalse(ChartStations.isStation(null));
    }
}
