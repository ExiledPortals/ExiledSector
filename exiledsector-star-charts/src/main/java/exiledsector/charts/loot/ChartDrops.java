package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.data.EnumKeys;
import exiledsector.data.RandomRounding;
import exiledsector.items.DroppableDefinitions;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemType;
import exiledsector.items.ItemTypes;
import exiledsector.loot.FleetDifficulties;
import exiledsector.loot.Loot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ChartDrops {

    static final int NO_TIER = 0;
    static final float BATTLE_DEPLOYMENT_POINTS_PER_CHANCE = 100f;

    public enum Source {
        RUN_REWARD(true),
        SYSTEM_SALVAGE(true),
        CHART_FLEET(true),
        CHART_BOSS(true),
        BATTLE(false),
        SALVAGE(false),
        TECHMINING_FIRST_FIND(false),
        TECHMINING_MONTHLY(false);

        private final boolean insideSystem;

        Source(boolean insideSystem) {
            this.insideSystem = insideSystem;
        }

        public String id() {
            return EnumKeys.key(this);
        }

        public boolean insideSystem() {
            return insideSystem;
        }

        static Source byId(String id) {
            return EnumKeys.parse(Source.class, id);
        }
    }

    public record Mults(float chart, float cipher, float catalyst) {

        public static Mults uniform(float mult) {
            return new Mults(mult, mult, mult);
        }

        boolean any() {
            return chart > 0f || cipher > 0f || catalyst > 0f;
        }
    }

    public record Rolled(List<ItemData> charts, Map<String, Integer> ciphers, Map<String, Integer> catalysts) {

        public static final Rolled NONE = new Rolled(List.of(), Map.of(), Map.of());

        public Rolled {
            charts = List.copyOf(charts);
            ciphers = Map.copyOf(ciphers);
            catalysts = Map.copyOf(catalysts);
        }

        public Map<String, Integer> materials() {
            if (catalysts.isEmpty()) {
                return ciphers;
            }
            Map<String, Integer> materials = new LinkedHashMap<>(ciphers);
            catalysts.forEach((itemId, quantity) -> materials.merge(itemId, quantity, Integer::sum));
            return materials;
        }

        public boolean isEmpty() {
            return charts.isEmpty() && ciphers.isEmpty() && catalysts.isEmpty();
        }

        public Loot asLoot() {
            return new Loot(charts, List.of(), materials());
        }
    }

    private record RunExtras(Map<String, Integer> ciphers, Map<String, Integer> catalysts) {

        static final RunExtras NONE = new RunExtras(Map.of(), Map.of());
    }

    private ChartDrops() {
    }

    public static Rolled roll(Source source, int tier, float chanceMult, Random random) {
        return roll(source, tier, Mults.uniform(chanceMult), random);
    }

    public static Rolled roll(Source source, int tier, Mults mults, Random random) {
        return roll(source, ChartLootContext.atTier(tier), mults, random);
    }

    public static Rolled roll(Source source, ChartLootContext context, Mults mults, Random random) {
        ChartDropRules.Rule rule = ChartDropRules.rule(source);
        if (!StarChartSettings.enabled() || rule == null || context.tier() == NO_TIER || !mults.any()) {
            return Rolled.NONE;
        }
        int naturalTier = ChartTiers.clampDropTier(context.tier());
        int baseTier = source.insideSystem() ? naturalTier : Math.min(rule.maxTier(), naturalTier);
        List<ItemData> charts = mults.chart() > 0f ? rollCharts(rule, baseTier, mults.chart(), random) : List.of();
        RunExtras extras = source.insideSystem() ? rollRunExtras(rule, context, mults, random) : RunExtras.NONE;
        return charts.isEmpty() && extras.ciphers().isEmpty() && extras.catalysts().isEmpty() ? Rolled.NONE
                : new Rolled(charts, extras.ciphers(), extras.catalysts());
    }

    private static List<ItemData> rollCharts(ChartDropRules.Rule rule, int baseTier, float chartMult, Random random) {
        List<ItemData> charts = new ArrayList<>();
        int chartCount = chartCount(rule, chartMult, random);
        for (int i = 0; i < chartCount; i++) {
            int dropTier = dropTier(rule, baseTier, rule.pickTierOffset(random));
            ItemDefinition definition = pickChartAtTier(random, dropTier);
            if (definition != null) {
                charts.add(ItemData.rolledAtTier(definition, random.nextLong(), dropTier));
            }
        }
        return charts;
    }

    private static RunExtras rollRunExtras(ChartDropRules.Rule rule, ChartLootContext run, Mults mults, Random random) {
        float expectedCiphers = rule.cipherChance() * Math.max(0f, mults.cipher());
        Map<String, Integer> ciphers = run.craftingItems(expectedCiphers, ChartCraftingPools.CIPHERS, ChartLootContext.PlayerFindBonus.APPLIES,
                random);
        Map<String, Integer> catalysts = new LinkedHashMap<>();
        int catalystCount = AtlasCatalysts.all().isEmpty() ? 0 : RandomRounding.round(rule.catalystChance() * Math.max(0f, mults.catalyst()), random);
        int catalystTier = ChartTiers.clampDropTier(run.tier());
        for (int i = 0; i < catalystCount; i++) {
            AtlasCatalysts.Catalyst catalyst = AtlasCatalysts.pickDrop(catalystTier, random);
            if (catalyst != null) {
                catalysts.merge(catalyst.itemId(), 1, Integer::sum);
            }
        }
        return new RunExtras(ciphers, catalysts);
    }

    static int dropTier(ChartDropRules.Rule rule, int baseTier, int offset) {
        return Math.max(ChartTiers.LOWEST_TIER, Math.min(Math.max(baseTier, rule.maxTier()), baseTier + offset));
    }

    public static ItemDefinition pickChartAtTier(Random random, int tier) {
        return DroppableDefinitions.pickOfType(random, ItemType.STAR_CHART, definition -> ItemTypes.rollsAtTier(definition, tier));
    }

    static int chartCount(ChartDropRules.Rule rule, float chanceMult, Random random) {
        float expected = rule.chance() * chanceMult;
        int count = (int) expected;
        if (random.nextFloat() < expected - count) {
            count++;
        }
        if (count > 0 && random.nextFloat() < rule.extraChance() * chanceMult) {
            count++;
        }
        return count;
    }

    public static int tierForDefeatedDp(Source source, float defeatedDp) {
        ChartDropRules.Rule rule = ChartDropRules.rule(source);
        if (rule == null || !(defeatedDp >= rule.minDefeatedDp()) || defeatedDp <= 0f) {
            return NO_TIER;
        }
        int steps = rule.defeatedDpPerTier() > 0f ? (int) ((defeatedDp - rule.minDefeatedDp()) / rule.defeatedDpPerTier()) : 0;
        return Math.min(rule.maxTier(), ChartTiers.clamp(ChartTiers.LOWEST_TIER + steps));
    }

    public static Rolled forRunReward(ChartLootContext run, Random random) {
        if (run.isOutsideRuns()) {
            return Rolled.NONE;
        }
        return roll(Source.RUN_REWARD, run, run.rewardChartDropMults(), random);
    }

    public static Rolled forBattle(LocationAPI battleLocation, float difficultyScaledDeploymentPoints, int highestDefeatedLevel,
                                   Random random) {
        if (ChartIds.isChartSystem(battleLocation)) {
            return Rolled.NONE;
        }
        return roll(Source.BATTLE, battleTier(difficultyScaledDeploymentPoints, highestDefeatedLevel),
                difficultyScaledDeploymentPoints / BATTLE_DEPLOYMENT_POINTS_PER_CHANCE, random);
    }

    static int battleTier(float defeatedDeploymentPoints, int highestDefeatedLevel) {
        int defeatedDpTier = tierForDefeatedDp(Source.BATTLE, defeatedDeploymentPoints);
        if (defeatedDpTier == NO_TIER) {
            return NO_TIER;
        }
        return FleetDifficulties.shipLevelsRated() ? ChartTiers.tierForLevel(highestDefeatedLevel) : defeatedDpTier;
    }

    public static Rolled forSalvage(SectorAPI sector, LocationAPI salvageLocation, Random random) {
        if (ChartIds.isChartSystem(salvageLocation)) {
            return forSystem(Source.SYSTEM_SALVAGE, ChartRuns.liveIn(salvageLocation), random);
        }
        return roll(Source.SALVAGE, tierForDefeatedDp(Source.SALVAGE, playerLevel(sector)), 1f, random);
    }

    public static Rolled forTechMining(SectorAPI sector, boolean firstFind, float chanceMult, Random random) {
        Source source = firstFind ? Source.TECHMINING_FIRST_FIND : Source.TECHMINING_MONTHLY;
        return roll(source, tierForDefeatedDp(source, playerLevel(sector)), chanceMult, random);
    }

    static int playerLevel(SectorAPI sector) {
        MutableCharacterStatsAPI playerStats = sector == null ? null : sector.getPlayerStats();
        return playerStats == null ? 0 : playerStats.getLevel();
    }

    private static Rolled forSystem(Source source, ChartRun systemRun, Random random) {
        if (systemRun == null) {
            return Rolled.NONE;
        }
        ChartLootContext run = ChartLootContext.of(systemRun);
        return roll(source, run, run.chartDropMults(1f), random);
    }
}
