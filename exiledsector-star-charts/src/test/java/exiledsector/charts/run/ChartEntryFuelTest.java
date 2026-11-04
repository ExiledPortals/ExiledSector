package exiledsector.charts.run;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.generation.ChartTiers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartEntryFuelTest {

    @Test
    void costStartsAtAThousandAndRisesAHundredPerTier() {
        assertEquals(1000, ChartEntryFuel.cost(ChartTiers.LOWEST_TIER));
        assertEquals(1100, ChartEntryFuel.cost(ChartTiers.LOWEST_TIER + 1));
        assertEquals(2500, ChartEntryFuel.cost(ChartTiers.CORRUPTED_TIER));
    }

    @Test
    void affordingNeedsTheWholeCost() {
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        CargoAPI cargo = mock(CargoAPI.class);
        when(fleet.getCargo()).thenReturn(cargo);

        when(cargo.getFuel()).thenReturn(ChartEntryFuel.cost(run) - 1f);
        assertFalse(ChartEntryFuel.canAfford(run, fleet));
        when(cargo.getFuel()).thenReturn((float) ChartEntryFuel.cost(run));
        assertTrue(ChartEntryFuel.canAfford(run, fleet));
    }

    @Test
    void chargingNeverTakesMoreThanTheFleetHolds() {
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        CargoAPI cargo = mock(CargoAPI.class);
        when(fleet.getCargo()).thenReturn(cargo);
        when(cargo.getFuel()).thenReturn(300f);

        ChartEntryFuel.charge(run, fleet);

        verify(cargo).removeFuel(300f);
    }
}
