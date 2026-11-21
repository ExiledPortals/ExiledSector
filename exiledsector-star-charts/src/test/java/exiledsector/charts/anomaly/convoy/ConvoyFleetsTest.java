package exiledsector.charts.anomaly.convoy;

import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.ChartContentPlan.PlannedFleet;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvoyFleetsTest {

    @Test
    void theConvoyOutgunsTheBossOnEveryTier() {
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            ChartTier chartTier = ChartAnomalyFleets.tier(tier, ChartGenContext.none());
            PlannedFleet boss = ChartContentPlanner.plannedBoss(chartTier, ChartContentPlanner.NO_POI);
            ChartAnomalyFleets.Spec convoy = ConvoyFleets.spec("pirates", chartTier, 1f);

            int convoyStrength = ConvoyFleets.strengthBonus(chartTier, 1f);
            assertTrue(convoy.strength().combatPoints() >= boss.strength().combatPoints(), "tier " + tier);
            assertTrue(convoy.strength().combatPoints() > boss.strength().combatPoints() || convoyStrength > boss.strengthBonus(), "tier " + tier);
            assertTrue(convoy.strength().quality() >= boss.strength().quality(), "tier " + tier);
            assertTrue(convoy.strength().officerLevelBonus() >= boss.strength().officerLevelBonus(), "tier " + tier);
        }
    }

    @Test
    void theConvoyCarriesItsFreightersAndTankersAlongsideTheWarships() {
        ChartTier chartTier = ChartAnomalyFleets.tier(1, ChartGenContext.none());

        ChartAnomalyFleets.Spec convoy = ConvoyFleets.spec("pirates", chartTier, 1f);

        float cargoPoints = ConvoyRules.freighterPoints(1);
        assertEquals(cargoPoints * (1f - ConvoyRules.TANKER_SHARE), convoy.freighterPoints(), 0.01f);
        assertEquals(cargoPoints * ConvoyRules.TANKER_SHARE, convoy.tankerPoints(), 0.01f);
        assertEquals(ConvoyRules.FREIGHTER_GUARD_POINTS, ConvoyFleets.spec("pirates", chartTier, 0f).strength().combatPoints(), 0.01f);
    }
}
