package exiledsector.charts.officer;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class OfficerFleetBoonsTest {

    private static final ChartTier TIER = ChartTier.fallback(5);

    private static ChartGenContext boosted() {
        return ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_OFFICER_FLEET_SIZE_PERCENT, 25f),
                new AppliedEffect(ChartModifier.CHART_OFFICER_LEVELS, 1f), new AppliedEffect(ChartModifier.CHART_OFFICER_FLEET_QUALITY_PERCENT, 50f),
                new AppliedEffect(ChartModifier.CHART_OFFICER_ESCORT_OFFICER_LEVELS, 2f),
                new AppliedEffect(ChartModifier.CHART_SKIRMISH_OFFICER_FLEET_SIZE_PERCENT, 30f)));
    }

    @Test
    void anOfficersCommandCentreTakesTheOfficerLines() {
        OfficerFleetBoons boons = OfficerFleetBoons.atCommandCentre(boosted().withAnomalyData(AtlasOfficer.RUN_DATA_KEY, "4"));

        assertEquals(new OfficerFleetBoons(25f, 50f, 2, 1), boons);
        ChartTier strengthened = boons.strengthen(TIER);
        assertEquals(TIER.bossSize() + 25f, strengthened.bossSize());
        assertEquals(TIER.fleetQuality() + 50f, strengthened.fleetQuality());
        assertEquals(TIER.officerLevelBonus() + 2, strengthened.officerLevelBonus());
        assertEquals(TIER.fleetSize(), strengthened.fleetSize());
    }

    @Test
    void aChartWithoutAnOfficerIsNotStrengthened() {
        assertEquals(OfficerFleetBoons.NONE, OfficerFleetBoons.atCommandCentre(boosted()));
        assertEquals(OfficerFleetBoons.NONE, OfficerFleetBoons.atCommandCentre(null));
        assertSame(TIER, OfficerFleetBoons.NONE.strengthen(TIER));
    }

    @Test
    void aSkirmishAddsItsOwnFleetSize() {
        assertEquals(new OfficerFleetBoons(55f, 50f, 2, 1), OfficerFleetBoons.inSkirmish(boosted()));
        assertEquals(OfficerFleetBoons.NONE, OfficerFleetBoons.inSkirmish(null));
    }
}
