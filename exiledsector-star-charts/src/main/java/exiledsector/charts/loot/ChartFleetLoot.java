package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.content.ChartFleetSpread;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemData;
import exiledsector.items.ItemOvercharge;
import exiledsector.items.ItemQuantity;
import exiledsector.items.ItemTypes;
import exiledsector.items.ItemUnlock;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.DropType;
import exiledsector.loot.Loot;
import exiledsector.loot.LootSources;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ChartFleetLoot {

    static final String MODSPEC_CACHE_RATE = "modspec_cache";
    static final int TWINNED_BOSS_FLEETS = 2;

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

    public static Loot roll(SectorAPI sector, CampaignFleetAPI fleet, float defeatedDeploymentPoints, Battle battle, Random random) {
        if (!ChartFleetLedger.hasLootLedger(fleet)) {
            return Loot.NONE;
        }
        ChartRun run = ChartFleetLedger.liveRunOf(fleet);
        if (run == null) {
            return Loot.NONE;
        }
        return roll(sector, fleet, run, ChartLootContext.of(run), defeatedDeploymentPoints, battle, random);
    }

    static Loot roll(SectorAPI sector, CampaignFleetAPI fleet, ChartRun run, ChartLootContext loot, float defeatedDeploymentPoints, Battle battle,
                     Random random) {
        FleetClaim claim = ChartFleetLedger.claim(fleet, run, defeatedDeploymentPoints);
        if (claim == null) {
            return Loot.NONE;
        }
        ChartLootContext fleetLoot = fleetLootContext(loot, battle.anomalies(), claim.boss());
        Loot claimedLoot = modspecCaches(fleetLoot, claim.lootDeploymentPoints(), battle, random)
                .plus(List.of(), List.of(), craftingItems(fleetLoot, claim.lootDeploymentPoints(), battle, random))
                .plus(chartDrops(run, fleetLoot, claim, random))
                .plus(anomalyFleetLoot(run, fleetLoot, claim, battle, random));
        if (overchargesHullItems(fleet, run)) {
            claimedLoot = overcharged(claimedLoot, random);
        }
        return claim.boss() ? claimedLoot.plus(doctrineArchives(sector, run, fleetLoot, claim, random)) : claimedLoot;
    }

    static ChartLootContext fleetLootContext(ChartLootContext loot, List<LootShaper> battleAnomalies, boolean boss) {
        float battleQuantityPercent = 0f;
        for (LootShaper anomaly : battleAnomalies) {
            battleQuantityPercent += anomaly.battleQuantityPercent(loot.effects());
        }
        return loot.forFleetLoot(boss).plusQuantityPercent(battleQuantityPercent);
    }

    private static Loot anomalyFleetLoot(ChartRun run, ChartLootContext loot, FleetClaim claim, Battle battle, Random random) {
        Loot anomalyLoot = Loot.NONE;
        for (LootShaper anomaly : ChartAnomalies.inRun(run, LootShaper.class)) {
            anomalyLoot = anomalyLoot.plus(anomaly.fleetLoot(claim, loot, battle, random));
        }
        return anomalyLoot;
    }

    private static Loot doctrineArchives(SectorAPI sector, ChartRun run, ChartLootContext bossLoot, FleetClaim claim, Random random) {
        List<ItemData> archives = LootSources.bossDrops(run.tier(), run.contentFaction(), doctrineArchiveQuantity(bossLoot, claim.mergedFleetShare()), random,
                definition -> ItemUnlock.canDrop(definition, sector));
        return archives.isEmpty() ? Loot.NONE : new Loot(archives, List.of(), Map.of());
    }

    static ItemQuantity doctrineArchiveQuantity(ChartLootContext bossLoot, float mergedFleetShare) {
        return new ItemQuantity(bossLoot.itemChances().increasedPercent(DropType.SUBROUTINE), mergedFleetShare);
    }

    public static boolean overchargesHullItems(CampaignFleetAPI fleet, ChartRun run) {
        return ChartAnomalies.inRun(run, LootShaper.class).stream().anyMatch(anomaly -> anomaly.overchargesHullItems(fleet));
    }

    private static Loot overcharged(Loot fleetLoot, Random random) {
        List<ItemData> items = fleetLoot.items().stream().map(item -> ItemOvercharge.overchargeHullItem(item, random)).toList();
        return new Loot(items, fleetLoot.schematics(), fleetLoot.materials());
    }

    public static float expectedFleetPoints(int tier, boolean twinnedBoss) {
        ChartTier base = ChartTiers.forTier(tier);
        int fleetLimit = StarChartSettings.fleetLimit();
        float fleets = (spreadFleetPoints(base.fleetsMin(), base, fleetLimit) + spreadFleetPoints(base.fleetsMax(), base, fleetLimit)) / 2f;
        float boss = base.boss() ? ChartTier.fleetPoints(Math.min(ChartContentPlanner.BOSS_SIZE_CAP, base.bossSize())) : 0f;
        return fleets + boss * (twinnedBoss ? TWINNED_BOSS_FLEETS : 1);
    }

    private static float spreadFleetPoints(int rolledFleets, ChartTier base, int fleetLimit) {
        ChartFleetSpread spread = ChartFleetSpread.of(rolledFleets, base.fleetSize(), 0, fleetLimit);
        return spread.fleets() * ChartTier.fleetPoints(spread.cappedFleetSize());
    }

    static Loot modspecCaches(ChartLootContext loot, float lootDeploymentPoints, Battle battle, Random random) {
        BattleLootRates.Rate rate = BattleLootRates.rate(MODSPEC_CACHE_RATE);
        if (rate == null) {
            return Loot.NONE;
        }
        float expectedCacheRolls = rate.expectedAtDifficulty(lootDeploymentPoints, battle.deploymentPoints(), battle.difficultyMultiplier());
        List<ItemData> caches = ItemTypes.rollSalvage(random, expectedCacheRolls * loot.quantityMult(DropType.SUBROUTINE));
        return caches.isEmpty() ? Loot.NONE : new Loot(caches, List.of(), Map.of());
    }

    private static Loot chartDrops(ChartRun run, ChartLootContext loot, FleetClaim claim, Random random) {
        float chartShare = claim.chartShare(loot);
        float chartMult = claim.boss() ? loot.effects().bossStarChartMult(ChartAnomalies.bossStarChartPercent(run)) : loot.quantityMult(DropType.STAR_CHART);
        ChartDrops.Mults mults = new ChartDrops.Mults(chartMult * chartShare, 0f, loot.quantityMult(DropType.CATALYST) * chartShare);
        ChartDrops.Rolled chartLoot = ChartDrops.roll(claim.boss() ? ChartDrops.Source.CHART_BOSS : ChartDrops.Source.CHART_FLEET, loot, mults,
                random);
        return chartLoot.isEmpty() ? Loot.NONE : chartLoot.asLoot();
    }

    static Map<String, Integer> craftingItems(ChartLootContext loot, float lootDeploymentPoints, Battle battle, Random random) {
        BattleLootRates.Rate rate = BattleLootRates.rate(BattleLootRates.CRAFTING_ITEM_RATE);
        float baseCount = rate == null ? 0f : rate.expectedAtDifficulty(lootDeploymentPoints, battle.deploymentPoints(), battle.difficultyMultiplier());
        return loot.craftingItems(baseCount, ChartCraftingPools.ANY_ITEM, random);
    }
}
