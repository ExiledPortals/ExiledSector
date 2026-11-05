package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.content.ChartFleetSpread;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.ChartIds;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemType;
import exiledsector.items.ItemTypes;
import exiledsector.items.ItemUnlock;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.BattleMaterials;
import exiledsector.loot.FleetLootRules;
import exiledsector.loot.LootMultipliers;
import exiledsector.loot.LootSources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public final class ChartFleetLoot {

    public static final String LOOT_FLEETS_MEMORY_KEY = "$exiledSector_chartLootFleets";
    static final Set<ItemType> GUARANTEED_UNIQUE_TYPES = Set.of(ItemType.HULL_ITEM, ItemType.SUBROUTINE);
    static final String SUBROUTINE_RATE = "subroutine";

    private ChartFleetLoot() {
    }

    public record Battle(float deploymentPoints, float difficultyMultiplier, List<LootShaper> anomalies) {

        public Battle {
            anomalies = anomalies == null ? List.of() : List.copyOf(anomalies);
        }

        static Battle of(float deploymentPoints) {
            return new Battle(deploymentPoints, 1f, List.of());
        }
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
        ChartRun run = ChartRuns.live();
        MemoryAPI fleetMemory = run == null ? null : fleet.getMemoryWithoutUpdate();
        return fleetMemory != null && fleetMemory.getBoolean(ChartIds.fleetRunKey(run.runId())) ? run : null;
    }

    public static ItemDrops.SiteLoot roll(SectorAPI sector, CampaignFleetAPI fleet, float defeatedDeploymentPoints, Random random) {
        return roll(sector, fleet, defeatedDeploymentPoints, Battle.of(defeatedDeploymentPoints), random);
    }

    public static ItemDrops.SiteLoot roll(SectorAPI sector, CampaignFleetAPI fleet, float defeatedDeploymentPoints, Battle battle, Random random) {
        if (!isChartFleet(fleet)) {
            return ItemDrops.SiteLoot.NONE;
        }
        ChartRun run = liveRunOf(fleet);
        if (run == null) {
            return ItemDrops.SiteLoot.NONE;
        }
        ChartGenContext context = ChartRunEffects.contextOf(run);
        return roll(sector, fleet, run, context, defeatedDeploymentPoints, battle, battleLootMult(battle.anomalies(), context), random);
    }

    public static float battleLootMult(List<LootShaper> battleAnomalies, ChartGenContext context) {
        float quantityMult = 1f;
        for (LootShaper anomaly : battleAnomalies) {
            quantityMult *= anomaly.battleLootMult(context);
        }
        return quantityMult;
    }

    static ItemDrops.SiteLoot roll(SectorAPI sector, CampaignFleetAPI fleet, ChartRun run, ChartGenContext context, float defeatedDeploymentPoints,
                                   Random random) {
        return roll(sector, fleet, run, context, defeatedDeploymentPoints, Battle.of(defeatedDeploymentPoints), 1f, random);
    }

    static ItemDrops.SiteLoot roll(SectorAPI sector, CampaignFleetAPI fleet, ChartRun run, ChartGenContext context, float defeatedDeploymentPoints,
                                   Battle battle, float quantityMult, Random random) {
        float claimedShare = claimShare(fleet, defeatedDeploymentPoints);
        if (!(claimedShare > 0f)) {
            return ItemDrops.SiteLoot.NONE;
        }
        MemoryAPI ledger = ledgerOf(fleet);
        boolean boss = isBoss(fleet, run);
        float spoilsMult = boss ? context.bossSpoilsMult() : 1f;
        float share = claimedShare * lootFleets(fleet) * quantityMult;
        float lootDeploymentPoints = claimedShare * ledger.getFloat(ChartIds.START_DP_MEMORY_KEY) * spoilsMult * quantityMult;
        float chartShare = claimedShare * startFleetPoints(ledger) / Math.max(1f, expectedFleetPoints(run.tier())) * spoilsMult * quantityMult;
        float materialShare = chartShare * BattleLootRates.difficulty(battle.difficultyMultiplier()) * context.dropMult(DropType.CRAFTING_ITEM);
        ItemDrops.SiteLoot fleetLoot = subroutines(context, lootDeploymentPoints, battle, random)
                .plus(List.of(), List.of(), BattleMaterials.roll(lootDeploymentPoints, battle.deploymentPoints(), battle.difficultyMultiplier(),
                        FleetLootRules.full(context.lootMultipliers(1f)), drop -> !ChartMaterials.covers(drop), random))
                .plus(List.of(), List.of(), ChartMaterials.roll(run.tier(), materialShare, random))
                .plus(chartDrops(run, context, boss, chartShare, random));
        for (LootShaper anomaly : ChartAnomalies.inRun(run, LootShaper.class)) {
            ItemDrops.SiteLoot anomalyItems = anomaly.fleetItems(sector, fleet, run, context, share, random);
            if (anomalyItems != null) {
                fleetLoot = fleetLoot.plus(anomalyItems);
            }
        }
        for (LootShaper anomaly : ChartAnomalies.inRun(run, LootShaper.class)) {
            fleetLoot = fleetLoot.plus(anomaly.extraFleetLoot(fleet, run, context, random));
        }
        if (boss) {
            fleetLoot = fleetLoot.plus(bossChest(sector, context, share * context.bossSpoilsMult(), random));
            List<ItemData> bossDrops = LootSources.bossDrops(run.tier(), run.contentFaction(), share * context.bossSpoilsMult(), random,
                    definition -> ItemUnlock.canDrop(definition, sector));
            if (!bossDrops.isEmpty()) {
                fleetLoot = fleetLoot.plus(bossDrops, List.of(), Map.of());
            }
            ItemData guaranteedUnique = guaranteedUnique(sector, run, context, random);
            if (guaranteedUnique != null) {
                fleetLoot = fleetLoot.plus(List.of(guaranteedUnique), List.of(), Map.of());
            }
            fleetLoot = withBossGuarantees(sector, run, fleetLoot, random);
        }
        return fleetLoot;
    }

    static ItemDrops.SiteLoot withBossGuarantees(SectorAPI sector, ChartRun run, ItemDrops.SiteLoot bossLoot, Random random) {
        if (run.isBossLootGuaranteed()) {
            return bossLoot;
        }
        run.guaranteeBossLoot();
        if (!ItemUnlock.available(ItemType.HULL_ITEM, sector) || bossLoot.items().stream().anyMatch(ChartFleetLoot::isHullItem)) {
            return bossLoot;
        }
        ItemDefinition definition = ItemDrops.pickHullItemBasic(random);
        return definition == null ? bossLoot : bossLoot.plus(List.of(ItemData.rolled(definition, random.nextLong())), List.of(), Map.of());
    }

    private static boolean isHullItem(ItemData item) {
        ItemDefinition definition = ItemDefinitions.get(item.definitionId());
        return definition != null && definition.type() == ItemType.HULL_ITEM;
    }

    static ItemData guaranteedUnique(SectorAPI sector, ChartRun run, ChartGenContext context, Random random) {
        if (!context.guaranteedBossUnique() || run.isBossUniqueGranted()
                || ChartAnomalies.inRun(run, LootShaper.class).stream().anyMatch(anomaly -> anomaly.holdsBackBossUnique(run))) {
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

    static boolean isBoss(CampaignFleetAPI fleet, ChartRun run) {
        return fleet != null && fleet.getId() != null && fleet.getId().equals(run.bossFleetId());
    }

    private static float startFleetPoints(MemoryAPI ledger) {
        float startFleetPoints = ledger.getFloat(ChartIds.START_FP_MEMORY_KEY);
        return startFleetPoints > 0f ? startFleetPoints : ledger.getFloat(ChartIds.START_DP_MEMORY_KEY);
    }

    public static float expectedFleetPoints(int tier) {
        ChartTier base = ChartTiers.forTier(tier);
        int fleetLimit = StarChartSettings.fleetLimit();
        float fleets = (spreadFleetPoints(base.fleetsMin(), base, fleetLimit) + spreadFleetPoints(base.fleetsMax(), base, fleetLimit)) / 2f;
        float boss = base.boss() ? ChartTier.fleetPoints(Math.min(ChartContentPlanner.BOSS_SIZE_CAP, base.bossSize())) : 0f;
        return fleets + boss;
    }

    private static float spreadFleetPoints(int rolledFleets, ChartTier base, int fleetLimit) {
        ChartFleetSpread spread = ChartFleetSpread.of(rolledFleets, base.fleetSize(), 0, fleetLimit);
        return spread.fleets() * ChartTier.fleetPoints(spread.cappedFleetSize());
    }

    static ItemDrops.SiteLoot subroutines(ChartGenContext context, float lootDeploymentPoints, Battle battle, Random random) {
        BattleLootRates.Rate rate = BattleLootRates.rate(SUBROUTINE_RATE);
        if (rate == null) {
            return ItemDrops.SiteLoot.NONE;
        }
        float expected = rate.expected(lootDeploymentPoints, battle.deploymentPoints()) * BattleLootRates.difficulty(battle.difficultyMultiplier());
        ItemChances chances = context.itemChances();
        List<ItemData> items = new ArrayList<>();
        addRolled(items, ItemDrops.wholeFeedstock(expected * chances.general(), random), ItemDrops::pickBasic, chances.rarity(), random);
        if (ItemDrops.hasDroppableBasic(ItemKind.SUBROUTINE)) {
            addRolled(items, ItemDrops.wholeFeedstock(expected * chances.targeted(ItemKind.SUBROUTINE), random),
                    picker -> ItemDrops.pickSectionItemBasic(ItemKind.SUBROUTINE, picker), chances.rarity(), random);
        }
        items.addAll(ItemTypes.rollSalvage(random, expected * chances.general()));
        return items.isEmpty() ? ItemDrops.SiteLoot.NONE : new ItemDrops.SiteLoot(items, List.of(), Map.of());
    }

    private static void addRolled(List<ItemData> items, int count, Function<Random, ItemDefinition> picker, float rarity, Random random) {
        for (int i = 0; i < count; i++) {
            ItemDefinition definition = picker.apply(random);
            if (definition != null) {
                items.add(ItemData.rolledWithRarity(definition, random.nextLong(), rarity));
            }
        }
    }

    private static ItemDrops.SiteLoot chartDrops(ChartRun run, ChartGenContext context, boolean boss, float chartShare, Random random) {
        boolean offerTierUp = boss && context.guaranteedBossTierUp() && !run.isBossTierUpGranted();
        ChartDrops.Mults mults = new ChartDrops.Mults(context.dropMult(DropType.STAR_CHART) * chartShare, 0f,
                context.dropMult(DropType.CATALYST) * chartShare);
        ChartDrops.Loot chartLoot = ChartDrops.roll(boss ? ChartDrops.Source.CHART_BOSS : ChartDrops.Source.CHART_FLEET, run.tier(), mults,
                random, offerTierUp);
        if (offerTierUp && !chartLoot.charts().isEmpty()) {
            run.grantBossTierUp();
        }
        return chartLoot.isEmpty() ? ItemDrops.SiteLoot.NONE : new ItemDrops.SiteLoot(chartLoot.charts(), List.of(), chartLoot.materials());
    }

    private static ItemDrops.SiteLoot bossChest(SectorAPI sector, ChartGenContext context, float chanceMult, Random random) {
        LootMultipliers chartMultipliers = context.lootMultipliers(chanceMult);
        return LootSources.rollSite(sector, ChartIds.BOSS_LOOT_SITE, random,
                new LootMultipliers(chartMultipliers.itemChances(), chartMultipliers.feedstockMult(), 0f));
    }
}
