package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import exiledsector.charts.anomaly.StellarFlares;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.i18n.Translation;

import java.util.List;
import java.util.function.Function;

final class ConvoySunstruck {

    static final String SUNSTRUCK_KEY = ChartModifier.CHART_CONVOY_SUNSTRUCK.anomalyTuningKey();
    static final String COMBAT_READINESS_KEY = ChartModifier.CHART_CONVOY_SUNSTRUCK_COMBAT_READINESS_PERCENT.anomalyTuningKey();
    static final String COMBAT_READINESS_MOD_ID = "exiledSector_sunstruckConvoy";

    private ConvoySunstruck() {
    }

    static void apply(ChartRun run, StarSystemAPI system, List<ConvoyState> convoys, Function<ConvoyState, CampaignFleetAPI> fleets) {
        if (run == null || !run.anomalyIds().contains(StellarFlares.ANOMALY_ID)) {
            return;
        }
        ChartGenContext context = ChartRunEffects.contextOf(run);
        PlanetAPI star = system == null ? null : system.getStar();
        if (star != null && context.anomalyPercent(SUNSTRUCK_KEY) > 0f) {
            routeThroughCorona(convoys, star.getId());
        }
        float combatReadinessChange = context.anomalyPercent(COMBAT_READINESS_KEY) / 100f;
        if (combatReadinessChange < 0f) {
            convoys.forEach(convoy -> wearDown(fleets.apply(convoy), combatReadinessChange));
        }
    }

    static void routeThroughCorona(List<ConvoyState> convoys, String starId) {
        for (ConvoyState convoy : convoys) {
            if (convoy.phase() == ConvoyState.Phase.WAITING) {
                convoy.routeThroughFirst(starId);
            }
        }
    }

    static void wearDown(CampaignFleetAPI fleet, float combatReadinessChange) {
        if (fleet == null || fleet.getFleetData() == null) {
            return;
        }
        String source = Translation.gameText("chart.anomaly.supply_convoy.sunstruck");
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            member.getStats().getMaxCombatReadiness().modifyFlat(COMBAT_READINESS_MOD_ID, combatReadinessChange, source);
            RepairTrackerAPI repairs = member.getRepairTracker();
            repairs.setCR(Math.max(0f, repairs.getCR() + combatReadinessChange));
        }
    }
}
