package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.StarChartSettings;
import exiledsector.data.CsvRows;
import exiledsector.ModCsv;
import exiledsector.items.ItemCurrencies;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemType;
import exiledsector.items.ItemTypes;
import exiledsector.items.ItemUnlock;
import exiledsector.items.WeightedPick;
import exiledsector.loot.LootSources;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public final class ChartDrops {

    static final String PATH = "data/config/exiledSector/chart_drops.csv";
    static final String SOURCE_COLUMN = "source";
    static final int NO_TIER = 0;
    static final float BATTLE_DEPLOYMENT_POINTS_PER_CHANCE = 100f;
    private static final Logger LOG = Logger.getLogger(ChartDrops.class);
    private static volatile Map<Source, Rule> rules = Map.of();

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
            return name().toLowerCase(Locale.ROOT);
        }

        public boolean insideSystem() {
            return insideSystem;
        }

        static Source byId(String id) {
            for (Source source : values()) {
                if (source.id().equals(id)) {
                    return source;
                }
            }
            return null;
        }
    }

    record Rule(float chance, float extraChance, float tierUpChance, float currencyChance, float catalystChance, float minStrength,
                float strengthPerTier, int maxTier) {

        Rule {
            chance = Math.max(0f, chance);
            extraChance = Math.max(0f, extraChance);
            tierUpChance = Math.max(0f, Math.min(1f, tierUpChance));
            currencyChance = Math.max(0f, currencyChance);
            catalystChance = Math.max(0f, catalystChance);
            minStrength = Math.max(0f, minStrength);
            strengthPerTier = Math.max(0f, strengthPerTier);
            maxTier = ChartTiers.clamp(maxTier);
        }
    }

    public record Mults(float chart, float currency, float catalyst) {

        public static Mults uniform(float mult) {
            return new Mults(mult, mult, mult);
        }

        public static Mults of(ChartGenContext context, float scale) {
            return new Mults(context.dropMult(DropType.STAR_CHART) * scale, context.dropMult(DropType.CURRENCY) * scale,
                    context.dropMult(DropType.CATALYST) * scale);
        }

        boolean any() {
            return chart > 0f || currency > 0f || catalyst > 0f;
        }
    }

    public record Loot(List<ItemData> charts, Map<String, Integer> currencies, Map<String, Integer> catalysts) {

        public static final Loot NONE = new Loot(List.of(), Map.of(), Map.of());

        public Loot {
            charts = List.copyOf(charts);
            currencies = Map.copyOf(currencies);
            catalysts = Map.copyOf(catalysts);
        }

        public Map<String, Integer> materials() {
            if (catalysts.isEmpty()) {
                return currencies;
            }
            Map<String, Integer> materials = new LinkedHashMap<>(currencies);
            catalysts.forEach((itemId, quantity) -> materials.merge(itemId, quantity, Integer::sum));
            return materials;
        }

        public boolean isEmpty() {
            return charts.isEmpty() && currencies.isEmpty() && catalysts.isEmpty();
        }
    }

    private ChartDrops() {
    }

    public static void load() {
        ModCsv.load(SOURCE_COLUMN, PATH, LOG, ChartDrops::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<Source, Rule> loaded = new EnumMap<>(Source.class);
        CsvRows csv = new CsvRows(PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String sourceText = ModCsv.text(row, SOURCE_COLUMN);
                if (sourceText.isEmpty()) {
                    return;
                }
                String rowLabel = "row " + (index + 1);
                Source source = Source.byId(sourceText.toLowerCase(Locale.ROOT));
                if (source == null) {
                    csv.skipRow(rowLabel, "unknown source " + sourceText);
                    return;
                }
                float currencyChance = csv.number(row, "currency_chance", 0f, sourceText);
                if (currencyChance > 0f && !source.insideSystem()) {
                    csv.problem(rowLabel, "currency_chance", sourceText + " is outside a chart system, where chart currencies never drop");
                    currencyChance = 0f;
                }
                float catalystChance = csv.number(row, "catalyst_chance", 0f, sourceText);
                if (catalystChance > 0f && !source.insideSystem()) {
                    csv.problem(rowLabel, "catalyst_chance", sourceText + " is outside a chart system, where Atlas Catalysts never drop");
                    catalystChance = 0f;
                }
                loaded.put(source, new Rule(csv.number(row, "chance", 0f, sourceText), csv.number(row, "extra_chance", 0f, sourceText),
                        csv.number(row, "tier_up_chance", 0f, sourceText), currencyChance, catalystChance,
                        csv.number(row, "min_strength", 0f, sourceText), csv.number(row, "strength_per_tier", 0f, sourceText),
                        csv.integer(row, "max_tier", ChartTiers.HIGHEST_TIER, sourceText)));
            });
        }
        csv.report(LOG);
        rules = Map.copyOf(loaded);
    }

    public static void clear() {
        rules = Map.of();
    }

    static Rule rule(Source source) {
        return source == null ? null : rules.get(source);
    }

    public static boolean hasRule(Source source) {
        return rule(source) != null;
    }

    public static Loot roll(Source source, int tier, float chanceMult, Random random, Predicate<ItemDefinition> allowedUniques) {
        return roll(source, tier, Mults.uniform(chanceMult), random, allowedUniques);
    }

    public static Loot roll(Source source, int tier, Mults mults, Random random, Predicate<ItemDefinition> allowedUniques) {
        return roll(source, tier, mults, random, allowedUniques, false);
    }

    public static Loot roll(Source source, int tier, Mults mults, Random random, Predicate<ItemDefinition> allowedUniques, boolean guaranteedTierUp) {
        Rule rule = rule(source);
        if (!StarChartSettings.enabled() || rule == null || tier == NO_TIER || !mults.any()) {
            return Loot.NONE;
        }
        int baseTier = source.insideSystem() ? ChartTiers.clamp(tier) : Math.min(rule.maxTier(), ChartTiers.clamp(tier));
        List<ItemData> charts = new ArrayList<>();
        int chartCount = mults.chart() > 0f ? chartCount(rule, mults.chart(), random) : 0;
        if (guaranteedTierUp) {
            chartCount = Math.max(1, chartCount);
        }
        for (int i = 0; i < chartCount; i++) {
            boolean forced = guaranteedTierUp && i == 0;
            boolean tierUp = forced || random.nextFloat() < rule.tierUpChance();
            int dropTier = tierUp ? Math.max(baseTier, Math.min(forced ? ChartTiers.HIGHEST_TIER : rule.maxTier(), baseTier + 1)) : baseTier;
            ItemDefinition definition = pickChartAtTier(random, dropTier, allowedUniques);
            if (definition != null) {
                charts.add(ItemData.rolledAtTier(definition, random.nextLong(), dropTier));
            }
        }
        Map<String, Integer> currencies = new LinkedHashMap<>();
        if (source.insideSystem() && random.nextFloat() < rule.currencyChance() * Math.max(0f, mults.currency())) {
            String currencyId = pickChartCurrency(random);
            if (currencyId != null) {
                currencies.merge(currencyId, 1, Integer::sum);
            }
        }
        Map<String, Integer> catalysts = new LinkedHashMap<>();
        if (source.insideSystem() && rule.catalystChance() > 0f && !AtlasCatalysts.all().isEmpty()
                && random.nextFloat() < rule.catalystChance() * Math.max(0f, mults.catalyst())) {
            AtlasCatalysts.Catalyst catalyst = AtlasCatalysts.pickDrop(baseTier, random);
            if (catalyst != null) {
                catalysts.merge(catalyst.itemId(), 1, Integer::sum);
            }
        }
        return charts.isEmpty() && currencies.isEmpty() && catalysts.isEmpty() ? Loot.NONE : new Loot(charts, currencies, catalysts);
    }

    public static ItemDefinition pickChartAtTier(Random random, int tier, Predicate<ItemDefinition> filter) {
        return ItemDrops.pickOfType(random, ItemType.STAR_CHART, definition -> ItemTypes.rollsAtTier(definition, tier) && filter.test(definition));
    }

    public static String pickChartCurrency(Random random) {
        List<ItemCurrency> currencies = ItemCurrencies.forType(ItemType.STAR_CHART).stream()
                .filter(currency -> !currency.dropsFromShipLoot() && currency.dropWeight() > 0f)
                .toList();
        ItemCurrency picked = WeightedPick.pick(currencies, ItemCurrency::dropWeight, random);
        return picked == null ? null : picked.itemId();
    }

    static int chartCount(Rule rule, float chanceMult, Random random) {
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

    public static int tierForStrength(Source source, float strength) {
        Rule rule = rule(source);
        if (rule == null || !(strength >= rule.minStrength()) || strength <= 0f) {
            return NO_TIER;
        }
        int steps = rule.strengthPerTier() > 0f ? (int) ((strength - rule.minStrength()) / rule.strengthPerTier()) : 0;
        return Math.min(rule.maxTier(), ChartTiers.clamp(ChartTiers.LOWEST_TIER + steps));
    }

    static Predicate<ItemDefinition> uniquesFor(SectorAPI sector) {
        return definition -> ItemUnlock.canDropChart(definition, sector);
    }

    public static Loot forRunReward(SectorAPI sector, ChartRun run, float rewardMult, Random random) {
        if (run == null) {
            return Loot.NONE;
        }
        float catalystMult = rewardMult * Math.max(0f, 1f + ChartRunEffects.contextOf(run).dropPercent(DropType.CATALYST) / ChartGenContext.PERCENT);
        return roll(Source.RUN_REWARD, run.tier(), new Mults(rewardMult, rewardMult, catalystMult), random, uniquesFor(sector));
    }

    public static Loot forBattle(SectorAPI sector, LocationAPI battleLocation, float difficultyScaledDeploymentPoints, int highestDefeatedLevel,
                                 Random random) {
        if (isSystem(battleLocation)) {
            return Loot.NONE;
        }
        return roll(Source.BATTLE, battleTier(difficultyScaledDeploymentPoints, highestDefeatedLevel),
                difficultyScaledDeploymentPoints / BATTLE_DEPLOYMENT_POINTS_PER_CHANCE, random, uniquesFor(sector));
    }

    static int battleTier(float defeatedDeploymentPoints, int highestDefeatedLevel) {
        int strengthTier = tierForStrength(Source.BATTLE, defeatedDeploymentPoints);
        if (strengthTier == NO_TIER) {
            return NO_TIER;
        }
        return LootSources.shipLevelsRated() ? ChartTiers.tierForLevel(highestDefeatedLevel) : strengthTier;
    }

    public static Loot forSalvage(SectorAPI sector, LocationAPI salvageLocation, Random random) {
        if (isSystem(salvageLocation)) {
            return forSystem(sector, Source.SYSTEM_SALVAGE, liveSystemRun(salvageLocation), random);
        }
        return roll(Source.SALVAGE, tierForStrength(Source.SALVAGE, playerLevel(sector)), 1f, random, uniquesFor(sector));
    }

    public static Loot forTechMining(SectorAPI sector, boolean firstFind, float chanceMult, Random random) {
        Source source = firstFind ? Source.TECHMINING_FIRST_FIND : Source.TECHMINING_MONTHLY;
        return roll(source, tierForStrength(source, playerLevel(sector)), chanceMult, random, uniquesFor(sector));
    }

    static int playerLevel(SectorAPI sector) {
        MutableCharacterStatsAPI playerStats = sector == null ? null : sector.getPlayerStats();
        return playerStats == null ? 0 : playerStats.getLevel();
    }

    private static Loot forSystem(SectorAPI sector, Source source, ChartRun systemRun, Random random) {
        if (systemRun == null) {
            return Loot.NONE;
        }
        ChartGenContext context = ChartRunEffects.contextOf(systemRun);
        return roll(source, systemRun.tier(), Mults.of(context, 1f), random, uniquesFor(sector));
    }

    public static ChartGenContext liveContextAt(LocationAPI location) {
        ChartRun run = liveSystemRun(location);
        return run == null ? LootContexts.global() : ChartRunEffects.contextOf(run);
    }

    static boolean isSystem(LocationAPI location) {
        return location instanceof StarSystemAPI system && system.hasTag(ChartIds.SYSTEM_TAG);
    }

    static ChartRun liveSystemRun(LocationAPI location) {
        if (!(location instanceof StarSystemAPI system) || !system.hasTag(ChartIds.SYSTEM_TAG)) {
            return null;
        }
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null || !run.runState().isLive() || !run.runId().equals(ChartIds.runIdOf(system))) {
            return null;
        }
        return run;
    }
}
