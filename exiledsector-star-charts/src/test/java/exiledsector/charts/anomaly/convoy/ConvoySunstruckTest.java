package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    private static RepairTrackerAPI ship(CampaignFleetAPI fleet, float combatReadiness) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        RepairTrackerAPI repairs = mock(RepairTrackerAPI.class);
        when(member.getRepairTracker()).thenReturn(repairs);
        when(repairs.getCR()).thenReturn(combatReadiness);
        FleetDataAPI data = mock(FleetDataAPI.class);
        when(fleet.getFleetData()).thenReturn(data);
        when(data.getMembersListCopy()).thenReturn(List.of(member));
        return repairs;
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
    void sunstruckConvoysRouteThroughTheCoronaInChartsWithTheFlares() {
        ConvoyState convoy = waitingConvoy(0, "planet_a", List.of("planet_x"));

        ConvoySunstruck.route(run(SUNSTRUCK_CONVOY, StellarFlares.ANOMALY_ID), systemWithStar(), List.of(convoy));

        assertEquals(List.of(STAR_ID, "planet_x"), convoy.waypointIds());
    }

    @Test
    void aSunstruckConvoyLosesCombatReadinessOnceWhenItsFirstBattleStarts() {
        ConvoyState convoy = waitingConvoy(0, "planet_a", List.of());
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        RepairTrackerAPI repairs = ship(fleet, 0.7f);
        ChartRun run = run(SUNSTRUCK_CONVOY, StellarFlares.ANOMALY_ID);

        ConvoySunstruck.wearDown(fleet, convoy, run);
        ConvoySunstruck.wearDown(fleet, convoy, run);

        verify(repairs).setCR(0.5f);
        assertTrue(convoy.isSunstruck());
    }

    @Test
    void combatReadinessNeverDropsBelowZero() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        RepairTrackerAPI repairs = ship(fleet, 0.1f);

        ConvoySunstruck.wearDown(fleet, waitingConvoy(0, "planet_a", List.of()), run(SUNSTRUCK_CONVOY, StellarFlares.ANOMALY_ID));

        verify(repairs).setCR(0f);
    }

    @Test
    void everyConvoyOnTheOpposingSideNotYetSunstruckIsWornDown() {
        CampaignFleetAPI convoyFleet = mock(CampaignFleetAPI.class);
        CampaignFleetAPI sunstruckFleet = mock(CampaignFleetAPI.class);
        CampaignFleetAPI escort = mock(CampaignFleetAPI.class);
        BattleAPI battle = mock(BattleAPI.class);
        when(escort.getBattle()).thenReturn(battle);
        when(battle.getNonPlayerSide()).thenReturn(List.of(escort, convoyFleet, sunstruckFleet));
        ConvoyState convoy = waitingConvoy(0, "planet_a", List.of());
        ConvoyState sunstruck = waitingConvoy(1, "planet_b", List.of());
        sunstruck.markSunstruck();
        ChartRun run = run(SUNSTRUCK_CONVOY, StellarFlares.ANOMALY_ID);

        List<CampaignFleetAPI> wornDown = ConvoySunstruck.fleetsToWearDown(escort,
                fleet -> fleet == convoyFleet ? convoy : fleet == sunstruckFleet ? sunstruck : null, fleet -> run);

        assertEquals(List.of(convoyFleet), wornDown);
    }

    @Test
    void withoutTheFlaresOrTheNodeConvoysAreLeftAlone() {
        ConvoyState calm = waitingConvoy(0, "planet_a", List.of("planet_x"));
        ConvoyState unallocated = waitingConvoy(1, "planet_b", List.of("planet_y"));
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        RepairTrackerAPI repairs = ship(fleet, 0.7f);

        ConvoySunstruck.route(run(SUNSTRUCK_CONVOY, MagneticStorm.ANOMALY_ID), systemWithStar(), List.of(calm));
        ConvoySunstruck.route(run(List.of(), StellarFlares.ANOMALY_ID), systemWithStar(), List.of(unallocated));
        ConvoySunstruck.route(null, systemWithStar(), List.of(unallocated));
        ConvoySunstruck.wearDown(fleet, calm, run(SUNSTRUCK_CONVOY, MagneticStorm.ANOMALY_ID));
        ConvoySunstruck.wearDown(fleet, unallocated, run(List.of(), StellarFlares.ANOMALY_ID));
        ConvoySunstruck.wearDown(fleet, unallocated, null);

        assertEquals(List.of("planet_x"), calm.waypointIds());
        assertEquals(List.of("planet_y"), unallocated.waypointIds());
        verify(repairs, never()).setCR(anyFloat());
    }
}
