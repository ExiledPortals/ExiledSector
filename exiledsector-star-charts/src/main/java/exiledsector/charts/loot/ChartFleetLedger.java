package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartFleetMarks;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;

public final class ChartFleetLedger {

    public static final String LOOT_FLEETS_MEMORY_KEY = "$exiledSector_chartLootFleets";

    private ChartFleetLedger() {
    }

    public static void recordStart(CampaignFleetAPI fleet) {
        float startDeploymentPoints = deploymentPoints(fleet);
        if (fleet != null && startDeploymentPoints > 0f) {
            fleet.getMemoryWithoutUpdate().set(ChartIds.START_DP_MEMORY_KEY, startDeploymentPoints);
            fleet.getMemoryWithoutUpdate().set(ChartIds.START_FP_MEMORY_KEY, (float) fleet.getFleetPoints());
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

    public static void spendLoot(CampaignFleetAPI fleet) {
        fleet.getMemoryWithoutUpdate().set(ChartIds.LOOT_SPENT_MEMORY_KEY, true);
    }

    public static boolean lootSpent(CampaignFleetAPI fleet) {
        MemoryAPI fleetMemory = fleet == null ? null : fleet.getMemoryWithoutUpdate();
        return fleetMemory != null && fleetMemory.getBoolean(ChartIds.LOOT_SPENT_MEMORY_KEY);
    }

    public static boolean hasLootLedger(CampaignFleetAPI fleet) {
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
        if (fleet == null || hasLootLedger(fleet)) {
            return fleet != null;
        }
        ChartRun run = ChartRuns.liveIn(battleLocation);
        SectorEntityToken site = run == null ? null : defendedSite(fleet, (StarSystemAPI) battleLocation, run.runId());
        float defenderDeploymentPoints = site == null ? 0f : deploymentPoints(fleet);
        if (!(defenderDeploymentPoints > 0f)) {
            return false;
        }
        MemoryAPI siteMemory = site.getMemoryWithoutUpdate();
        if (!siteMemory.contains(ChartIds.START_DP_MEMORY_KEY)) {
            siteMemory.set(ChartIds.START_DP_MEMORY_KEY, defenderDeploymentPoints);
            siteMemory.set(ChartIds.START_FP_MEMORY_KEY, (float) fleet.getFleetPoints());
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

    static FleetClaim claim(CampaignFleetAPI fleet, ChartRun run, float defeatedDeploymentPoints) {
        float claimedShare = claimShare(fleet, defeatedDeploymentPoints);
        if (!(claimedShare > 0f)) {
            return null;
        }
        MemoryAPI ledger = ledgerOf(fleet);
        return new FleetClaim(fleet, claimedShare, ledger.getFloat(ChartIds.START_DP_MEMORY_KEY), startFleetPoints(ledger), lootFleets(fleet),
                isBoss(fleet, run));
    }

    static float claimShare(CampaignFleetAPI fleet, float defeatedDeploymentPoints) {
        MemoryAPI ledger = ledgerOf(fleet);
        if (ledger == null) {
            return 0f;
        }
        float startDeploymentPoints = ledger.getFloat(ChartIds.START_DP_MEMORY_KEY);
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

    private static float startFleetPoints(MemoryAPI ledger) {
        float startFleetPoints = ledger.getFloat(ChartIds.START_FP_MEMORY_KEY);
        return startFleetPoints > 0f ? startFleetPoints : ledger.getFloat(ChartIds.START_DP_MEMORY_KEY);
    }

    public static int lootFleets(CampaignFleetAPI fleet) {
        MemoryAPI fleetMemory = fleet == null ? null : fleet.getMemoryWithoutUpdate();
        return fleetMemory != null && fleetMemory.get(LOOT_FLEETS_MEMORY_KEY) instanceof Number fleets ? Math.max(1, fleets.intValue()) : 1;
    }

    public static ChartRun liveRunOf(CampaignFleetAPI fleet) {
        ChartRun run = ChartRuns.live();
        MemoryAPI fleetMemory = run == null ? null : fleet.getMemoryWithoutUpdate();
        return fleetMemory != null && fleetMemory.getBoolean(ChartIds.fleetRunKey(run.runId())) ? run : null;
    }

    static boolean isBoss(CampaignFleetAPI fleet, ChartRun run) {
        if (fleet == null || fleet.getId() == null) {
            return false;
        }
        return fleet.getId().equals(run.bossFleetId()) || ChartFleetMarks.twinBossFlagshipId(fleet) != null;
    }
}
