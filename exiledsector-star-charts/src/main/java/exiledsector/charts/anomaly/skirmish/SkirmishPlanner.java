package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.anomaly.ContentPlanSite;
import exiledsector.charts.content.ChartBossType;
import exiledsector.charts.content.ChartContentPlan;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.content.ChartContentPlan.PlannedFleet;
import exiledsector.charts.content.ChartContentPlan.PlannedStation;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.content.ChartFaction;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.objective.ChartObjective;
import exiledsector.items.WeightedPick;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

final class SkirmishPlanner {

    static final float ALLY_STRENGTH_DIVISOR = 1.3f;
    static final long ATTACKER_SALT = 0x4348534b524d53L;

    private SkirmishPlanner() {
    }

    static ChartContentPlan apply(ChartContentPlan plan, ContentPlanSite site) {
        String defenderId = plan.faction().factionId();
        SkirmishJoin join = SkirmishJoin.of(site.context());
        String attackerId = attacker(join == null ? null : join.attackerId(), defenderId, site.factions(), site.factionLoaded(),
                site.random(ATTACKER_SALT));
        if (attackerId == null) {
            return plan;
        }
        SkirmishSide side = join == null ? SkirmishSide.NEITHER : join.side();
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
        ChartContentPlan joined = plan.withDefenders(defenderFleets, station, side == SkirmishSide.DEFENDER)
                .withAnomalyPlan(FactionSkirmish.ANOMALY_ID, new SkirmishPlan(attackerId, side, attackerFleets));
        return side == SkirmishSide.DEFENDER ? joined : joined.withObjective(ChartObjective.BOSS);
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
