package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.anomaly.ContentPlanSite;
import exiledsector.charts.content.ChartBossType;
import exiledsector.charts.content.ChartContentPlan;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.content.ChartContentPlan.PlannedFleet;
import exiledsector.charts.content.ChartContentPlan.PlannedStation;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.officer.turn.Intent;

import java.util.ArrayList;
import java.util.List;

final class SkirmishPlanner {

    static final float ALLY_COMBAT_POINTS_DIVISOR = 1.3f;

    private SkirmishPlanner() {
    }

    static ChartContentPlan apply(ChartContentPlan plan, ContentPlanSite site) {
        String defenderId = plan.faction().factionId();
        SkirmishJoin join = SkirmishJoin.of(site.context());
        if (!ledByOfficer(join, defenderId, site)) {
            return plan;
        }
        String attackerId = join.attackerId();
        SkirmishSide side = join.side();
        List<PlannedFleet> attackerFleets = attackerFleets(plan.fleets(), site.tier());
        List<PlannedFleet> defenderFleets = plan.fleets();
        PlannedStation station = plan.bossStation();
        if (side == SkirmishSide.DEFENDER) {
            defenderFleets = asAllies(defenderFleets);
            if (station != null && station.fallbackBoss() != null) {
                station = new PlannedStation(station.variantId(), station.officerLevel(), asAllies(List.of(station.fallbackBoss())).get(0));
            }
        } else if (side == SkirmishSide.ATTACKER) {
            attackerFleets = asAllies(attackerFleets);
        }
        return plan.withDefenders(defenderFleets, station, side == SkirmishSide.DEFENDER)
                .withAnomalyPlan(FactionSkirmish.ANOMALY_ID, new SkirmishPlan(attackerId, join.contest().officerRecordId(), side, attackerFleets));
    }

    static boolean ledByOfficer(SkirmishJoin join, String defenderId, ContentPlanSite site) {
        return join != null && join.contest() != null && join.contest().officerRecordId() != Intent.NO_OFFICER && join.attackerId() != null
                && !join.attackerId().equals(defenderId) && site.factionLoaded().test(join.attackerId());
    }

    static List<PlannedFleet> attackerFleets(List<PlannedFleet> defenderFleets, ChartTier tier) {
        List<PlannedFleet> fleets = new ArrayList<>();
        for (PlannedFleet defending : defenderFleets) {
            if (!defending.role().bossFleet()) {
                fleets.add(new PlannedFleet(FleetRole.PATROL, ChartContentPlanner.NO_POI, defending.combatPoints(), defending.quality(),
                        defending.officerLevelBonus(), defending.officerCountBonus(), defending.strengthBonus(), defending.averageSMods()));
            }
        }
        fleets.add(ChartContentPlanner.plannedBoss(tier, ChartContentPlanner.fleetQuality(tier), ChartContentPlanner.NO_POI, ChartBossType.FLEET));
        return fleets;
    }

    static List<PlannedFleet> asAllies(List<PlannedFleet> fleets) {
        List<PlannedFleet> allies = new ArrayList<>(fleets.size());
        for (PlannedFleet fleet : fleets) {
            allies.add(new PlannedFleet(fleet.role(), fleet.poiIndex(), fleet.combatPoints() / ALLY_COMBAT_POINTS_DIVISOR, fleet.quality(),
                    fleet.officerLevelBonus(), fleet.officerCountBonus(), fleet.strengthBonus(), fleet.averageSMods()));
        }
        return allies;
    }
}
