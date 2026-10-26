package exiledsector.charts;

import exiledsector.charts.ChartContentPlan.FleetRole;
import exiledsector.charts.ChartContentPlan.PlannedFleet;
import exiledsector.charts.ChartContentPlan.PlannedSkirmish;
import exiledsector.charts.ChartContentPlan.PlannedStation;
import exiledsector.charts.anomaly.skirmish.SkirmishFactions;
import exiledsector.charts.anomaly.skirmish.SkirmishSide;
import exiledsector.items.WeightedPick;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

final class SkirmishPlanner {

    static final float ALLY_STRENGTH_DIVISOR = 1.3f;

    private SkirmishPlanner() {
    }

    static ChartContentPlan apply(ChartContentPlan plan, ChartTier tier, ChartGenContext chartContext, List<ChartFaction> factions,
                                  Predicate<String> factionLoaded, Random random) {
        String defenderId = plan.faction().factionId();
        String attackerId = attacker(chartContext.skirmishAttackerId(), defenderId, factions, factionLoaded, random);
        if (attackerId == null) {
            return plan;
        }
        SkirmishSide side = chartContext.skirmishSide() == null ? SkirmishSide.NEITHER : chartContext.skirmishSide();
        List<PlannedFleet> attackerFleets = attackerFleets(plan.fleets(), tier);
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
        return new ChartContentPlan(plan.tier(), plan.faction(), plan.objective(), plan.pois(), defenderFleets, plan.anomalies(), plan.bossType(),
                station, new PlannedSkirmish(attackerId, side, attackerFleets));
    }

    static String attacker(String chosenId, String defenderId, List<ChartFaction> factions, Predicate<String> factionLoaded, Random random) {
        if (chosenId != null && !chosenId.equals(defenderId) && factionLoaded.test(chosenId)) {
            return chosenId;
        }
        List<ChartFaction> candidates = new ArrayList<>();
        for (ChartFaction faction : factions) {
            String factionId = faction.factionId();
            if (faction.weight() > 0f && !factionId.equals(defenderId) && SkirmishFactions.takesSides(factionId) && factionLoaded.test(factionId)) {
                candidates.add(faction);
            }
        }
        ChartFaction picked = WeightedPick.pick(candidates, ChartFaction::weight, random);
        return picked == null ? null : picked.factionId();
    }

    static List<PlannedFleet> attackerFleets(List<PlannedFleet> defenderFleets, ChartTier tier) {
        List<PlannedFleet> fleets = new ArrayList<>();
        for (PlannedFleet defending : defenderFleets) {
            if (!defending.role().bossFleet()) {
                fleets.add(new PlannedFleet(FleetRole.PATROL, ChartContentPlanner.NO_POI, defending.combatPoints(), defending.quality(),
                        defending.officerLevelBonus(), defending.officerCountBonus(), defending.strengthBonus(), defending.averageSMods()));
            }
        }
        if (tier.boss()) {
            fleets.add(ChartContentPlanner.plannedBoss(tier, ChartContentPlanner.fleetQuality(tier), ChartContentPlanner.NO_POI, ChartBossType.FLEET));
        }
        return fleets;
    }

    static List<PlannedFleet> asAllies(List<PlannedFleet> fleets) {
        List<PlannedFleet> allies = new ArrayList<>(fleets.size());
        for (PlannedFleet fleet : fleets) {
            allies.add(new PlannedFleet(fleet.role(), fleet.poiIndex(), fleet.combatPoints() / ALLY_STRENGTH_DIVISOR, fleet.quality(),
                    fleet.officerLevelBonus(), fleet.officerCountBonus(), fleet.strengthBonus(), fleet.averageSMods()));
        }
        return allies;
    }
}
