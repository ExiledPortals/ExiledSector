package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.anomaly.ContentPlanSite;
import exiledsector.charts.content.ChartBossType;
import exiledsector.charts.content.ChartContentPlan;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.content.ChartContentPlan.PlannedFleet;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.officer.OfficerFleetBoons;
import exiledsector.charts.officer.OfficerRanks;

final class SkirmishPlanner {

    private SkirmishPlanner() {
    }

    static ChartContentPlan apply(ChartContentPlan plan, ContentPlanSite site) {
        SkirmishFight fight = SkirmishFight.of(site.context());
        if (fight == null || !fight.betweenOfficers() || !site.factionLoaded().test(fight.attackerFactionId())) {
            return plan;
        }
        SkirmishPlan skirmish = new SkirmishPlan(fight.attackerFactionId(), fight.attackerRecordId(), fight.defenderRecordId(),
                officerFleet(site, fight.attackerRankId()), officerFleet(site, fight.defenderRankId()));
        return plan.withFleets(plan.fleets().stream().filter(planned -> planned.role() != FleetRole.BOSS).toList())
                .withAnomalyPlan(FactionSkirmish.ANOMALY_ID, skirmish);
    }

    private static PlannedFleet officerFleet(ContentPlanSite site, String rankId) {
        return officerFleet(OfficerFleetBoons.inSkirmish(site.context()).strengthen(site.tier()), rankId);
    }

    static PlannedFleet officerFleet(ChartTier tier, String rankId) {
        ChartTier ranked = tier.withExtraBossSize(OfficerRanks.rank(rankId).bossSizePercent());
        return ChartContentPlanner.plannedBoss(ranked, ChartContentPlanner.NO_POI, ChartBossType.FLEET);
    }
}
