package exiledsector.charts;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import exiledsector.ModCsv;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemUnlock;
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
    static final int MAX_CHARTS_PER_DROP = 4;
    static final int NO_TIER = 0;
    private static final Logger LOG = Logger.getLogger(ChartDrops.class);
    private static volatile Map<Source, Rule> rules = Map.of();

    public enum Source {
        RUN_REWARD(true),
        POCKET_SALVAGE(true),
        POCKET_BATTLE(true),
        BATTLE(false),
        SALVAGE(false),
        TECHMINING_FIRST_FIND(false),
        TECHMINING_MONTHLY(false);

        private final boolean insidePocket;

        Source(boolean insidePocket) {
            this.insidePocket = insidePocket;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public boolean insidePocket() {
            return insidePocket;
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

    record Rule(float chance, float extraChance, float tierUpChance, float currencyChance, float minStrength, float strengthPerTier,
                int maxTier) {

        Rule {
            chance = Math.max(0f, chance);
            extraChance = Math.max(0f, extraChance);
            tierUpChance = Math.max(0f, Math.min(1f, tierUpChance));
            currencyChance = Math.max(0f, currencyChance);
            minStrength = Math.max(0f, minStrength);
            strengthPerTier = Math.max(0f, strengthPerTier);
            maxTier = ChartTiers.clamp(maxTier);
        }
    }

    public record Loot(List<ItemData> charts, Map<String, Integer> currencies) {

        public static final Loot NONE = new Loot(List.of(), Map.of());

        public Loot {
            charts = List.copyOf(charts);
            currencies = Map.copyOf(currencies);
        }

        public boolean isEmpty() {
            return charts.isEmpty() && currencies.isEmpty();
        }
    }

    private ChartDrops() {
    }

    public static void load() {
        ModCsv.load(SOURCE_COLUMN, PATH, LOG, ChartDrops::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<Source, Rule> loaded = new EnumMap<>(Source.class);
        ChartCsv csv = new ChartCsv(PATH);
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
                if (currencyChance > 0f && !source.insidePocket()) {
                    csv.problem(rowLabel, "currency_chance", sourceText + " is outside a pocket, where chart currencies never drop");
                    currencyChance = 0f;
                }
                loaded.put(source, new Rule(csv.number(row, "chance", 0f, sourceText), csv.number(row, "extra_chance", 0f, sourceText),
                        csv.number(row, "tier_up_chance", 0f, sourceText), currencyChance, csv.number(row, "min_strength", 0f, sourceText),
                        csv.number(row, "strength_per_tier", 0f, sourceText), csv.integer(row, "max_tier", ChartTiers.HIGHEST_TIER, sourceText)));
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
        Rule rule = rule(source);
        if (rule == null || tier == NO_TIER || !(chanceMult > 0f)) {
            return Loot.NONE;
        }
        int baseTier = Math.min(rule.maxTier(), ChartTiers.clamp(tier));
        List<ItemData> charts = new ArrayList<>();
        int chartCount = chartCount(rule, chanceMult, random);
        for (int i = 0; i < chartCount; i++) {
            int dropTier = random.nextFloat() < rule.tierUpChance() ? Math.min(rule.maxTier(), baseTier + 1) : baseTier;
            ItemDefinition definition = ItemDrops.pickChartAtTier(random, dropTier, allowedUniques);
            if (definition != null) {
                charts.add(ItemData.rolledAtTier(definition, random.nextLong(), dropTier));
            }
        }
        Map<String, Integer> currencies = new LinkedHashMap<>();
        if (source.insidePocket() && random.nextFloat() < rule.currencyChance() * chanceMult) {
            String currencyId = ItemDrops.pickChartCurrency(random);
            if (currencyId != null) {
                currencies.merge(currencyId, 1, Integer::sum);
            }
        }
        return charts.isEmpty() && currencies.isEmpty() ? Loot.NONE : new Loot(charts, currencies);
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
        return Math.min(MAX_CHARTS_PER_DROP, count);
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
        return run == null ? Loot.NONE : roll(Source.RUN_REWARD, run.tier(), rewardMult, random, uniquesFor(sector));
    }

    public static Loot forBattle(SectorAPI sector, LocationAPI battleLocation, float defeatedDeploymentPoints, Random random) {
        if (isPocket(battleLocation)) {
            return forPocket(sector, Source.POCKET_BATTLE, livePocketRun(battleLocation), random);
        }
        return roll(Source.BATTLE, tierForStrength(Source.BATTLE, defeatedDeploymentPoints), 1f, random, uniquesFor(sector));
    }

    public static Loot forSalvage(SectorAPI sector, LocationAPI salvageLocation, Random random) {
        if (isPocket(salvageLocation)) {
            return forPocket(sector, Source.POCKET_SALVAGE, livePocketRun(salvageLocation), random);
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

    private static Loot forPocket(SectorAPI sector, Source source, ChartRun pocketRun, Random random) {
        if (pocketRun == null) {
            return Loot.NONE;
        }
        return roll(source, pocketRun.tier(), ChartRunEffects.contextOf(pocketRun).chartDropMult(), random, uniquesFor(sector));
    }

    static boolean isPocket(LocationAPI location) {
        return location instanceof StarSystemAPI system && system.hasTag(ChartIds.POCKET_TAG);
    }

    static ChartRun livePocketRun(LocationAPI location) {
        if (!(location instanceof StarSystemAPI system) || !system.hasTag(ChartIds.POCKET_TAG)) {
            return null;
        }
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null || !run.runState().isLive() || !run.runId().equals(ChartIds.runIdOf(system))) {
            return null;
        }
        return run;
    }
}
