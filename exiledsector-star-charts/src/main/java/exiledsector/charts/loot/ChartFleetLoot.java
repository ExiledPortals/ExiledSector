package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.content.ChartFleetSpread;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.run.ChartRun;
import exiledsector.data.RandomRounding;
import exiledsector.items.DroppableDefinitions;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemOvercharge;
import exiledsector.items.ItemType;
import exiledsector.items.ItemTypes;
import exiledsector.items.ItemUnlock;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.Loot;
import exiledsector.loot.LootSources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public final class ChartFleetLoot {

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
        float battleLootMult = battleLootMult(battle.anomalies(), loot.effects());
        FleetClaim claim = ChartFleetLedger.claim(fleet, run, defeatedDeploymentPoints, loot.effects(), battleLootMult);
        if (claim == null) {
            return Loot.NONE;
        }
        Loot fleetLoot = subroutines(loot, claim.lootDeploymentPoints(), battle, random)
                .plus(List.of(), List.of(), craftingItems(loot, claim.lootDeploymentPoints(), battle, random))
                .plus(chartDrops(run, loot, claim, random))
                .plus(anomalyFleetLoot(run, loot, claim, battle, random));
        if (overchargesHullItems(fleet, run)) {
            fleetLoot = overcharged(fleetLoot, random);
        }
        return claim.boss() ? withBossLoot(sector, run, loot, claim, fleetLoot, random) : fleetLoot;
    }

    public static float battleLootMult(List<LootShaper> battleAnomalies, ChartGenContext context) {
        float quantityMult = 1f;
        for (LootShaper anomaly : battleAnomalies) {
            quantityMult *= anomaly.battleLootMult(context);
        }
        return quantityMult;
    }

    private static Loot anomalyFleetLoot(ChartRun run, ChartLootContext loot, FleetClaim claim, Battle battle, Random random) {
        Loot anomalyLoot = Loot.NONE;
        for (LootShaper anomaly : ChartAnomalies.inRun(run, LootShaper.class)) {
            anomalyLoot = anomalyLoot.plus(anomaly.fleetLoot(claim, loot, battle, random));
        }
        return anomalyLoot;
    }

    private static Loot withBossLoot(SectorAPI sector, ChartRun run, ChartLootContext loot, FleetClaim claim, Loot fleetLoot, Random random) {
        Loot bossLoot = fleetLoot.plus(bossChest(sector, loot, claim.bossShare(), random));
        List<ItemData> bossDrops = LootSources.bossDrops(run.tier(), run.contentFaction(), claim.bossShare(), random,
                definition -> ItemUnlock.canDrop(definition, sector));
        if (!bossDrops.isEmpty()) {
            bossLoot = bossLoot.plus(bossDrops, List.of(), Map.of());
        }
        ItemData guaranteedUnique = guaranteedUnique(sector, run, loot, random);
        if (guaranteedUnique != null) {
            bossLoot = bossLoot.plus(List.of(guaranteedUnique), List.of(), Map.of());
        }
        return withBossGuarantees(sector, run, loot, bossLoot, random);
    }

    public static boolean overchargesHullItems(CampaignFleetAPI fleet, ChartRun run) {
        return ChartAnomalies.inRun(run, LootShaper.class).stream().anyMatch(anomaly -> anomaly.overchargesHullItems(fleet));
    }

    private static Loot overcharged(Loot fleetLoot, Random random) {
        List<ItemData> items = fleetLoot.items().stream().map(item -> ItemOvercharge.overchargeHullItem(item, random)).toList();
        return new Loot(items, fleetLoot.schematics(), fleetLoot.materials());
    }

    static Loot withBossGuarantees(SectorAPI sector, ChartRun run, ChartLootContext loot, Loot bossLoot, Random random) {
        if (run.isBossLootGuaranteed()) {
            return bossLoot;
        }
        run.guaranteeBossLoot();
        if (!ItemUnlock.available(ItemType.HULL_ITEM, sector) || bossLoot.items().stream().anyMatch(ItemData::isHullItem)) {
            return bossLoot;
        }
        ItemDefinition definition = DroppableDefinitions.pickHullItemBasic(random);
        return definition == null ? bossLoot
                : bossLoot.plus(List.of(loot.rolledWithDropLimits(definition, random.nextLong())), List.of(), Map.of());
    }

    static ItemData guaranteedUnique(SectorAPI sector, ChartRun run, ChartLootContext loot, Random random) {
        if (!loot.effects().guaranteedBossUnique() || run.isBossUniqueGranted()
                || ChartAnomalies.inRun(run, LootShaper.class).stream().anyMatch(anomaly -> anomaly.holdsBackBossUnique(run))) {
            return null;
        }
        ItemDefinition unique = DroppableDefinitions.pickUniqueFromAnySource(random, definition -> GUARANTEED_UNIQUE_TYPES.contains(definition.type())
                && ItemUnlock.available(definition.type(), sector) && ItemUnlock.canDrop(definition, sector));
        if (unique == null) {
            return null;
        }
        run.grantBossUnique();
        return ItemData.rolled(unique, random.nextLong());
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

    static Loot subroutines(ChartLootContext loot, float lootDeploymentPoints, Battle battle, Random random) {
        BattleLootRates.Rate rate = BattleLootRates.rate(SUBROUTINE_RATE);
        if (rate == null) {
            return Loot.NONE;
        }
        float expected = rate.expectedAtDifficulty(lootDeploymentPoints, battle.deploymentPoints(), battle.difficultyMultiplier());
        ItemChances chances = loot.itemChances();
        List<ItemData> items = new ArrayList<>();
        addRolled(items, RandomRounding.round(expected * chances.general(), random), DroppableDefinitions::pickBasic, loot, random);
        if (DroppableDefinitions.hasDroppableBasic(ItemKind.SUBROUTINE)) {
            addRolled(items, RandomRounding.round(expected * chances.targeted(ItemKind.SUBROUTINE), random),
                    picker -> DroppableDefinitions.pickBasicOfKind(ItemKind.SUBROUTINE, picker), loot, random);
        }
        items.addAll(ItemTypes.rollSalvage(random, expected * chances.general()));
        return items.isEmpty() ? Loot.NONE : new Loot(items, List.of(), Map.of());
    }

    private static void addRolled(List<ItemData> items, int count, Function<Random, ItemDefinition> picker, ChartLootContext loot, Random random) {
        for (int i = 0; i < count; i++) {
            ItemDefinition definition = picker.apply(random);
            if (definition != null) {
                items.add(loot.rolledWithDropLimits(definition, random.nextLong()));
            }
        }
    }

    private static Loot chartDrops(ChartRun run, ChartLootContext loot, FleetClaim claim, Random random) {
        float chartShare = claim.chartShare(loot.tier());
        float chartMult = claim.boss() ? loot.effects().bossStarChartMult(ChartAnomalies.bossStarChartPercent(run)) : loot.dropMult(DropType.STAR_CHART);
        ChartDrops.Mults mults = new ChartDrops.Mults(chartMult * chartShare, 0f, loot.dropMult(DropType.CATALYST) * chartShare);
        ChartDrops.Rolled chartLoot = ChartDrops.roll(claim.boss() ? ChartDrops.Source.CHART_BOSS : ChartDrops.Source.CHART_FLEET, loot, mults,
                random);
        return chartLoot.isEmpty() ? Loot.NONE : chartLoot.asLoot();
    }

    static Map<String, Integer> craftingItems(ChartLootContext loot, float lootDeploymentPoints, Battle battle, Random random) {
        BattleLootRates.Rate rate = BattleLootRates.rate(BattleLootRates.CRAFTING_ITEM_RATE);
        float expectedCount = rate == null ? 0f : rate.expectedAtDifficulty(lootDeploymentPoints, battle.deploymentPoints(), battle.difficultyMultiplier())
                * loot.dropMult(DropType.CRAFTING_ITEM);
        return loot.craftingItems(expectedCount, ChartCraftingPools.ANY_ITEM, ChartLootContext.PlayerFindBonus.APPLIES, random);
    }

    private static Loot bossChest(SectorAPI sector, ChartLootContext loot, float chanceMult, Random random) {
        return LootSources.rollSite(sector, ChartIds.BOSS_LOOT_SITE, random, loot.multipliers(chanceMult).withCraftingItemMult(0f));
    }
}
