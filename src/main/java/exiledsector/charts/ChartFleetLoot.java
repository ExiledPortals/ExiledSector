package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemUnlock;

import java.util.List;
import java.util.Random;

public final class ChartFleetLoot {

    private ChartFleetLoot() {
    }

    static void recordStart(CampaignFleetAPI fleet) {
        float startDeploymentPoints = deploymentPoints(fleet);
        if (startDeploymentPoints > 0f) {
            fleet.getMemoryWithoutUpdate().set(ChartIds.START_DP_MEMORY_KEY, startDeploymentPoints);
        }
    }

    static float deploymentPoints(CampaignFleetAPI fleet) {
        FleetDataAPI fleetData = fleet == null ? null : fleet.getFleetData();
        if (fleetData == null) {
            return 0f;
        }
        float totalDeploymentPoints = 0f;
        for (FleetMemberAPI member : fleetData.getMembersListCopy()) {
            totalDeploymentPoints += member.getDeploymentPointsCost();
        }
        return totalDeploymentPoints;
    }

    public static boolean isChartFleet(CampaignFleetAPI fleet) {
        MemoryAPI fleetMemory = fleet == null ? null : fleet.getMemoryWithoutUpdate();
        return fleetMemory != null && fleetMemory.contains(ChartIds.START_DP_MEMORY_KEY);
    }

    static float claimShare(CampaignFleetAPI fleet, float defeatedDeploymentPoints) {
        MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();
        float startDeploymentPoints = fleetMemory.getFloat(ChartIds.START_DP_MEMORY_KEY);
        if (!(startDeploymentPoints > 0f) || !(defeatedDeploymentPoints > 0f)) {
            return 0f;
        }
        float paidDeploymentPoints = Math.max(0f, fleetMemory.getFloat(ChartIds.LOOT_DP_PAID_MEMORY_KEY));
        float claimedDeploymentPoints = Math.min(defeatedDeploymentPoints, startDeploymentPoints - paidDeploymentPoints);
        if (!(claimedDeploymentPoints > 0f)) {
            return 0f;
        }
        fleetMemory.set(ChartIds.LOOT_DP_PAID_MEMORY_KEY, paidDeploymentPoints + claimedDeploymentPoints);
        return claimedDeploymentPoints / startDeploymentPoints;
    }

    static ChartRun liveRunOf(CampaignFleetAPI fleet) {
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null || !run.runState().isLive()) {
            return null;
        }
        MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();
        return fleetMemory != null && fleetMemory.getBoolean(ChartIds.fleetRunKey(run.runId())) ? run : null;
    }

    public static ItemDrops.SiteLoot roll(SectorAPI sector, CampaignFleetAPI fleet, float defeatedDeploymentPoints, Random random) {
        if (!isChartFleet(fleet)) {
            return ItemDrops.SiteLoot.NONE;
        }
        ChartRun run = liveRunOf(fleet);
        return run == null ? ItemDrops.SiteLoot.NONE : roll(sector, fleet, run, ChartRunEffects.contextOf(run), defeatedDeploymentPoints, random);
    }

    static ItemDrops.SiteLoot roll(SectorAPI sector, CampaignFleetAPI fleet, ChartRun run, ChartGenContext context, float defeatedDeploymentPoints,
                                   Random random) {
        float share = claimShare(fleet, defeatedDeploymentPoints);
        if (!(share > 0f)) {
            return ItemDrops.SiteLoot.NONE;
        }
        ItemDrops.SiteLoot fleetLoot = roll(sector, run, context, ChartIds.FLEET_LOOT_SITE, ChartDrops.Source.CHART_FLEET, share, random);
        if (fleet.getId() != null && fleet.getId().equals(run.bossFleetId())) {
            fleetLoot = fleetLoot.plus(roll(sector, run, context, ChartIds.WARDEN_LOOT_SITE, ChartDrops.Source.CHART_WARDEN,
                    share * context.wardenSpoilsMult(), random));
        }
        return fleetLoot;
    }

    private static ItemDrops.SiteLoot roll(SectorAPI sector, ChartRun run, ChartGenContext context, String siteId, ChartDrops.Source source,
                                           float chanceMult, Random random) {
        ItemDrops.SiteLoot siteLoot = ItemDrops.rollSite(siteId, random, context.itemChances().scaled(chanceMult),
                context.dropMult(DropType.FEEDSTOCK) * chanceMult, context.dropMult(DropType.CURRENCY) * chanceMult, ItemUnlock.sectionsOpen(sector));
        ChartDrops.Loot chartLoot = ChartDrops.roll(source, run.tier(), context.dropMult(DropType.STAR_CHART) * chanceMult,
                context.dropMult(DropType.CURRENCY) * chanceMult, random, ChartDrops.uniquesFor(sector));
        return siteLoot.plus(chartLoot.charts(), List.of(), chartLoot.currencies());
    }
}
