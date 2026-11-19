package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import exiledsector.charts.anomaly.LiveAnomalyRuns;
import exiledsector.charts.anomaly.MagneticStorm;
import exiledsector.charts.anomaly.StellarFlares;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.charts.run.ChartRun;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConvoySunstruckTest {

    private static final String STAR_ID = "star";
    private static final List<AppliedEffect> SUNSTRUCK_CONVOY = List.of(new AppliedEffect(ChartModifier.CHART_CONVOY_SUNSTRUCK, 1f),
            new AppliedEffect(ChartModifier.CHART_CONVOY_SUNSTRUCK_COMBAT_READINESS_PERCENT, -20f));

    private static ConvoyState waitingConvoy(int index, String destinationId, List<String> waypointIds) {
        return new ConvoyState(index, waypointIds, destinationId, 0f, 0f, 0f, 0f, index);
    }

    private static StarSystemAPI systemWithStar() {
        StarSystemAPI system = mock(StarSystemAPI.class);
        PlanetAPI star = mock(PlanetAPI.class);
        when(star.getId()).thenReturn(STAR_ID);
        when(system.getStar()).thenReturn(star);
        return system;
    }

    private static ChartRun run(List<AppliedEffect> treeLines, String... anomalyIds) {
        ChartRun run = LiveAnomalyRuns.run(LiveAnomalyRuns.SITE_TIER, anomalyIds);
        run.recordAtlasTreeLines(treeLines);
        return run;
    }

    private record Ship(MutableStat maxCombatReadiness, RepairTrackerAPI repairs) {
    }

    private static Ship ship(CampaignFleetAPI fleet, float combatReadiness) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat maxCombatReadiness = new MutableStat(0.7f);
        RepairTrackerAPI repairs = mock(RepairTrackerAPI.class);
        when(member.getStats()).thenReturn(stats);
        when(stats.getMaxCombatReadiness()).thenReturn(maxCombatReadiness);
        when(member.getRepairTracker()).thenReturn(repairs);
        when(repairs.getCR()).thenReturn(combatReadiness);
        FleetDataAPI data = mock(FleetDataAPI.class);
        when(fleet.getFleetData()).thenReturn(data);
        when(data.getMembersListCopy()).thenReturn(List.of(member));
        return new Ship(maxCombatReadiness, repairs);
    }

    @Test
    void waitingConvoysRouteThroughTheStarFirstAndDepartedOnesKeepTheirRoute() {
        ConvoyState waiting = waitingConvoy(0, "planet_a", List.of("planet_x"));
        ConvoyState alreadyRouted = waitingConvoy(1, "planet_b", List.of(STAR_ID, "planet_y"));
        ConvoyState departed = waitingConvoy(2, "planet_c", List.of("planet_z"));
        departed.setPhase(ConvoyState.Phase.EN_ROUTE);

        ConvoySunstruck.routeThroughCorona(List.of(waiting, alreadyRouted, departed), STAR_ID);

        assertEquals(List.of(STAR_ID, "planet_x"), waiting.waypointIds());
        assertEquals(List.of(STAR_ID, "planet_y"), alreadyRouted.waypointIds());
        assertEquals(List.of("planet_z"), departed.waypointIds());
    }

    @Test
    void sunstruckConvoysRouteThroughTheCoronaAndLoseCombatReadinessInChartsWithTheFlares() {
        ConvoyState convoy = waitingConvoy(0, "planet_a", List.of("planet_x"));
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        Ship ship = ship(fleet, 0.7f);

        ConvoySunstruck.apply(run(SUNSTRUCK_CONVOY, StellarFlares.ANOMALY_ID), systemWithStar(), List.of(convoy), state -> fleet);

        assertEquals(List.of(STAR_ID, "planet_x"), convoy.waypointIds());
        assertEquals(0.5f, ship.maxCombatReadiness().getModifiedValue(), 1e-4f);
        verify(ship.repairs()).setCR(0.5f);
    }

    @Test
    void combatReadinessNeverDropsBelowZero() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        Ship ship = ship(fleet, 0.1f);

        ConvoySunstruck.apply(run(SUNSTRUCK_CONVOY, StellarFlares.ANOMALY_ID), systemWithStar(), List.of(waitingConvoy(0, "planet_a", List.of())),
                state -> fleet);

        verify(ship.repairs()).setCR(0f);
    }

    @Test
    void withoutTheFlaresOrTheNodeConvoysAreLeftAlone() {
        ConvoyState calm = waitingConvoy(0, "planet_a", List.of("planet_x"));
        ConvoyState unallocated = waitingConvoy(1, "planet_b", List.of("planet_y"));
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        Ship ship = ship(fleet, 0.7f);

        ConvoySunstruck.apply(run(SUNSTRUCK_CONVOY, MagneticStorm.ANOMALY_ID), systemWithStar(), List.of(calm), state -> fleet);
        ConvoySunstruck.apply(run(List.of(), StellarFlares.ANOMALY_ID), systemWithStar(), List.of(unallocated), state -> fleet);
        ConvoySunstruck.apply(null, systemWithStar(), List.of(unallocated), state -> fleet);

        assertEquals(List.of("planet_x"), calm.waypointIds());
        assertEquals(List.of("planet_y"), unallocated.waypointIds());
        assertEquals(0.7f, ship.maxCombatReadiness().getModifiedValue(), 1e-4f);
        verify(ship.repairs(), never()).setCR(anyFloat());
    }
}
