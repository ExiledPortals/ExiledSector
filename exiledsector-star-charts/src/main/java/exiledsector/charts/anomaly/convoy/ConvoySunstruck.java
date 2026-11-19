package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import exiledsector.charts.anomaly.StellarFlares;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;

import java.util.List;
import java.util.function.Function;

final class ConvoySunstruck {

    static final String SUNSTRUCK_KEY = ChartModifier.CHART_CONVOY_SUNSTRUCK.anomalyTuningKey();
    static final String COMBAT_READINESS_KEY = ChartModifier.CHART_CONVOY_SUNSTRUCK_COMBAT_READINESS_PERCENT.anomalyTuningKey();

    private ConvoySunstruck() {
    }

    static void route(ChartRun run, StarSystemAPI system, List<ConvoyState> convoys) {
        PlanetAPI star = system == null ? null : system.getStar();
        if (hasFlares(run) && star != null && ChartRunEffects.contextOf(run).anomalyPercent(SUNSTRUCK_KEY) > 0f) {
            routeThroughCorona(convoys, star.getId());
        }
    }

    static void routeThroughCorona(List<ConvoyState> convoys, String starId) {
        for (ConvoyState convoy : convoys) {
            if (convoy.phase() == ConvoyState.Phase.WAITING) {
                convoy.routeThroughFirst(starId);
            }
        }
    }

    static List<CampaignFleetAPI> fleetsToWearDown(SectorEntityToken opponent) {
        return fleetsToWearDown(opponent, ConvoyScript::convoyOf, fleet -> ChartRuns.liveIn(fleet.getContainingLocation()));
    }

    static List<CampaignFleetAPI> fleetsToWearDown(SectorEntityToken opponent, Function<CampaignFleetAPI, ConvoyState> convoys,
                                                   Function<CampaignFleetAPI, ChartRun> runs) {
        if (!(opponent instanceof CampaignFleetAPI opposingFleet)) {
            return List.of();
        }
        BattleAPI battle = opposingFleet.getBattle();
        List<CampaignFleetAPI> opposingSide = battle == null ? List.of(opposingFleet) : battle.getNonPlayerSide();
        return opposingSide.stream().filter(fleet -> {
            ConvoyState convoy = convoys.apply(fleet);
            return convoy != null && !convoy.isSunstruck() && combatReadinessChange(runs.apply(fleet)) < 0f;
        }).toList();
    }

    static void wearDown(CampaignFleetAPI fleet, ConvoyState convoy, ChartRun run) {
        float combatReadinessChange = combatReadinessChange(run);
        if (convoy == null || !(combatReadinessChange < 0f) || fleet.getFleetData() == null || !convoy.markSunstruck()) {
            return;
        }
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            RepairTrackerAPI repairs = member.getRepairTracker();
            repairs.setCR(Math.max(0f, repairs.getCR() + combatReadinessChange));
        }
    }

    static void wearDown(CampaignFleetAPI fleet) {
        wearDown(fleet, ConvoyScript.convoyOf(fleet), ChartRuns.liveIn(fleet.getContainingLocation()));
    }

    static float combatReadinessChange(ChartRun run) {
        return hasFlares(run) ? ChartRunEffects.contextOf(run).anomalyPercent(COMBAT_READINESS_KEY) / 100f : 0f;
    }

    private static boolean hasFlares(ChartRun run) {
        return run != null && run.anomalyIds().contains(StellarFlares.ANOMALY_ID);
    }
}
