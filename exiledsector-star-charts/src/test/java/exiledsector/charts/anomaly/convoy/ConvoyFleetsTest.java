package exiledsector.charts.anomaly.convoy;

import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.ChartContentPlan.PlannedFleet;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvoyFleetsTest {

    @Test
    void theCloseEscortAloneOutgunsTheBossOnEveryTier() {
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.CORRUPTED_TIER; tier++) {
            ChartTier chartTier = ChartAnomalyFleets.tier(tier, ChartGenContext.none());
            PlannedFleet boss = ChartContentPlanner.plannedBoss(chartTier, ChartContentPlanner.fleetQuality(chartTier), ChartContentPlanner.NO_POI);
            ChartAnomalyFleets.Spec escort = ConvoyFleets.spec(ConvoyRole.ESCORT, "pirates", chartTier, 1f);

            int escortStrength = ConvoyFleets.strengthBonus(ConvoyRole.ESCORT, chartTier, 1f);
            assertTrue(escort.combatPoints() >= boss.combatPoints(), "tier " + tier);
            assertTrue(escort.combatPoints() > boss.combatPoints() || escortStrength > boss.strengthBonus(), "tier " + tier);
            assertTrue(escort.quality() >= boss.quality(), "tier " + tier);
            assertTrue(escort.officerLevelBonus() >= boss.officerLevelBonus(), "tier " + tier);
        }
    }

    @Test
    void theScreenIsSmallerThanTheCloseEscort() {
        ChartTier chartTier = ChartAnomalyFleets.tier(1, ChartGenContext.none());

        float escort = ConvoyFleets.spec(ConvoyRole.ESCORT, "pirates", chartTier, 1f).combatPoints();
        float screen = ConvoyFleets.spec(ConvoyRole.SCREEN, "pirates", chartTier, 1f).combatPoints();

        assertTrue(screen > 0f && screen < escort);
    }
}
