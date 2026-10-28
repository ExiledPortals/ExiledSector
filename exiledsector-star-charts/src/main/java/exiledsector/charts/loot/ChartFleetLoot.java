package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.anomaly.debris.DormantFleetLoot;
import exiledsector.charts.anomaly.excavation.ExcavationLoot;
import exiledsector.charts.anomaly.relay.RelayStates;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemType;
import exiledsector.items.ItemUnlock;
import exiledsector.loot.LootSources;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class ChartFleetLoot {

    public static final String LOOT_FLEETS_MEMORY_KEY = "$exiledSector_chartLootFleets";
    static final Set<ItemType> GUARANTEED_UNIQUE_TYPES = Set.of(ItemType.HULL_ITEM, ItemType.SUBROUTINE);

    private ChartFleetLoot() {
    }

    public static void recordStart(CampaignFleetAPI fleet) {
        float startDeploymentPoints = deploymentPoints(fleet);
        if (fleet != null && startDeploymentPoints > 0f) {
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

    public static int lootFleets(CampaignFleetAPI fleet) {
        MemoryAPI fleetMemory = fleet == null ? null : fleet.getMemoryWithoutUpdate();
        return fleetMemory != null && fleetMemory.get(LOOT_FLEETS_MEMORY_KEY) instanceof Number fleets ? Math.max(1, fleets.intValue()) : 1;
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
        float share = claimShare(fleet, defeatedDeploymentPoints) * lootFleets(fleet);
        if (!(share > 0f)) {
            return ItemDrops.SiteLoot.NONE;
        }
        ItemDrops.SiteLoot siteItems = ExcavationLoot.fleetItems(sector, fleet, run, context, share, random);
        ItemDrops.SiteLoot fleetLoot = (siteItems != null ? siteItems
                : roll(sector, run, context, ChartIds.FLEET_LOOT_SITE, ChartDrops.Source.CHART_FLEET, share, random))
                .plus(DormantFleetLoot.currencyFor(fleet, run.tier(), context, random));
        if (fleet.getId() != null && fleet.getId().equals(run.bossFleetId())) {
            fleetLoot = fleetLoot.plus(roll(sector, run, context, ChartIds.BOSS_LOOT_SITE, ChartDrops.Source.CHART_BOSS,
                    share * context.bossSpoilsMult(), random));
            List<ItemData> bossDrops = LootSources.bossDrops(run.tier(), run.contentFaction(), share * context.bossSpoilsMult(), random,
                    definition -> ItemUnlock.canDrop(definition, sector));
            if (!bossDrops.isEmpty()) {
                fleetLoot = fleetLoot.plus(bossDrops, List.of(), Map.of());
            }
            ItemData guaranteedUnique = guaranteedUnique(sector, run, context, random);
            if (guaranteedUnique != null) {
                fleetLoot = fleetLoot.plus(List.of(guaranteedUnique), List.of(), Map.of());
            }
        }
        return fleetLoot;
    }

    static ItemData guaranteedUnique(SectorAPI sector, ChartRun run, ChartGenContext context, Random random) {
        if (!context.guaranteedBossUnique() || run.isBossUniqueGranted() || !RelayStates.allWoken(run)) {
            return null;
        }
        ItemDefinition unique = ItemDrops.pickUniqueFromAnySource(random, definition -> GUARANTEED_UNIQUE_TYPES.contains(definition.type())
                && ItemUnlock.available(definition.type(), sector) && ItemUnlock.canDrop(definition, sector));
        if (unique == null) {
            return null;
        }
        run.grantBossUnique();
        return ItemData.rolled(unique, random.nextLong());
    }

    private static ItemDrops.SiteLoot roll(SectorAPI sector, ChartRun run, ChartGenContext context, String siteId, ChartDrops.Source source,
                                           float chanceMult, Random random) {
        ItemChances chances = context.itemChances().scaled(chanceMult);
        float feedstockMult = context.dropMult(DropType.FEEDSTOCK) * chanceMult;
        float currencyMult = context.dropMult(DropType.CURRENCY) * chanceMult;
        ItemDrops.SiteLoot siteLoot = source == ChartDrops.Source.CHART_FLEET
                ? ItemDrops.rollFleetSalvage(siteId, random, chances, feedstockMult, currencyMult)
                : ItemDrops.rollSite(siteId, random, chances, feedstockMult, currencyMult, ItemUnlock.available(ItemType.HULL_ITEM, sector));
        boolean offerTierUp = source == ChartDrops.Source.CHART_BOSS && context.guaranteedBossTierUp() && !run.isBossTierUpGranted();
        ChartDrops.Loot chartLoot = ChartDrops.roll(source, run.tier(), ChartDrops.Mults.of(context, chanceMult), random, ChartDrops.uniquesFor(sector),
                offerTierUp);
        if (offerTierUp && !chartLoot.charts().isEmpty()) {
            run.grantBossTierUp();
        }
        return siteLoot.plus(chartLoot.charts(), List.of(), chartLoot.materials());
    }
}
