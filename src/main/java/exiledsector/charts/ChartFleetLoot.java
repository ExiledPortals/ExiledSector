package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
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
        return ledgerOf(fleet) != null;
    }

    static MemoryAPI ledgerOf(CampaignFleetAPI fleet) {
        MemoryAPI fleetMemory = fleet == null ? null : fleet.getMemoryWithoutUpdate();
        if (fleetMemory == null) {
            return null;
        }
        if (fleetMemory.contains(ChartIds.START_DP_MEMORY_KEY)) {
            return fleetMemory;
        }
        return fleetMemory.get(ChartIds.DEFENDED_SITE_MEMORY_KEY) instanceof SectorEntityToken site ? site.getMemoryWithoutUpdate() : null;
    }

    public static boolean adoptDefender(CampaignFleetAPI fleet, LocationAPI battleLocation) {
        if (fleet == null || isChartFleet(fleet)) {
            return fleet != null;
        }
        ChartRun run = ChartDrops.liveSystemRun(battleLocation);
        SectorEntityToken site = run == null ? null : defendedSite(fleet, (StarSystemAPI) battleLocation, run.runId());
        float defenderDeploymentPoints = site == null ? 0f : deploymentPoints(fleet);
        if (!(defenderDeploymentPoints > 0f)) {
            return false;
        }
        MemoryAPI siteMemory = site.getMemoryWithoutUpdate();
        if (!siteMemory.contains(ChartIds.START_DP_MEMORY_KEY)) {
            siteMemory.set(ChartIds.START_DP_MEMORY_KEY, defenderDeploymentPoints);
        }
        MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();
        fleetMemory.set(ChartIds.fleetRunKey(run.runId()), true);
        fleetMemory.set(ChartIds.DEFENDED_SITE_MEMORY_KEY, site);
        if (!siteMemory.getBoolean(ChartIds.DEFENDER_LOOT_ADDED_MEMORY_KEY)) {
            ChartVanillaLoot.addToFleet(fleet, false, ChartRunEffects.contextOf(run), run.tier());
            siteMemory.set(ChartIds.DEFENDER_LOOT_ADDED_MEMORY_KEY, true);
        }
        return true;
    }

    static SectorEntityToken defendedSite(CampaignFleetAPI fleet, StarSystemAPI system, String runId) {
        for (SectorEntityToken entity : system.getEntitiesWithTag(ChartIds.CONTENT_TAG)) {
            if (entity instanceof CampaignFleetAPI || !ChartIds.belongsToRun(entity, runId)) {
                continue;
            }
            MemoryAPI entityMemory = entity.getMemoryWithoutUpdate();
            if (entityMemory != null && entityMemory.get(ChartIds.VANILLA_DEFENDER_FLEET_MEMORY_KEY) == fleet) {
                return entity;
            }
        }
        return null;
    }

    static float claimShare(CampaignFleetAPI fleet, float defeatedDeploymentPoints) {
        MemoryAPI ledger = ledgerOf(fleet);
        float startDeploymentPoints = ledger == null ? 0f : ledger.getFloat(ChartIds.START_DP_MEMORY_KEY);
        if (!(startDeploymentPoints > 0f) || !(defeatedDeploymentPoints > 0f)) {
            return 0f;
        }
        float paidDeploymentPoints = Math.max(0f, ledger.getFloat(ChartIds.LOOT_DP_PAID_MEMORY_KEY));
        float claimedDeploymentPoints = Math.min(defeatedDeploymentPoints, startDeploymentPoints - paidDeploymentPoints);
        if (!(claimedDeploymentPoints > 0f)) {
            return 0f;
        }
        ledger.set(ChartIds.LOOT_DP_PAID_MEMORY_KEY, paidDeploymentPoints + claimedDeploymentPoints);
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
