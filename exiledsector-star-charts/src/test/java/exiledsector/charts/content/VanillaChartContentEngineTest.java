package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import exiledsector.i18n.Translation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VanillaChartContentEngineTest {

    @Test
    void aFleetWhoseFactionHasNoNameForItsTypeGetsAFallbackName() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);

        VanillaChartContentEngine.nameIfUnnamed(fleet, FleetTypes.PATROL_MEDIUM);

        verify(fleet).setName(Translation.gameText("chart.fleet.unnamed.patrol"));
    }

    @Test
    void aNamedFleetKeepsItsName() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getName()).thenReturn("Sub-Ordo");

        VanillaChartContentEngine.nameIfUnnamed(fleet, FleetTypes.PATROL_MEDIUM);

        verify(fleet, never()).setName(anyString());
    }

    @Test
    void everyChartFleetTypeHasAFallbackName() {
        assertEquals("chart.fleet.unnamed.taskForce", VanillaChartContentEngine.fallbackNameKey(FleetTypes.TASK_FORCE));
        assertEquals("chart.fleet.unnamed.station", VanillaChartContentEngine.fallbackNameKey(FleetTypes.BATTLESTATION));
        assertEquals("chart.fleet.unnamed.patrol", VanillaChartContentEngine.fallbackNameKey(FleetTypes.PATROL_SMALL));
        assertEquals("chart.fleet.unnamed.patrol", VanillaChartContentEngine.fallbackNameKey(FleetTypes.PATROL_LARGE));
        assertEquals("chart.fleet.unnamed.fleet", VanillaChartContentEngine.fallbackNameKey(FleetTypes.TRADE));
        assertEquals("chart.fleet.unnamed.fleet", VanillaChartContentEngine.fallbackNameKey(null));
        for (String fleetType : new String[]{FleetTypes.TASK_FORCE, FleetTypes.BATTLESTATION, FleetTypes.PATROL_MEDIUM, FleetTypes.TRADE}) {
            assertTrue(Translation.has(VanillaChartContentEngine.fallbackNameKey(fleetType)), fleetType);
        }
    }
}
