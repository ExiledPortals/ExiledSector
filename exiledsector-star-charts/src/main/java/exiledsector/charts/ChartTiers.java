package exiledsector.charts;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.data.CsvRows;
import exiledlib.ModCsv;
import exiledsector.loot.FleetLevelRange;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChartTiers {

    public static final int LOWEST_TIER = 1;
    public static final int HIGHEST_TIER = 15;
    static final String PATH = "data/config/exiledSector/star_chart_tiers.csv";
    static final String TIER_COLUMN = "tier";
    private static final Logger LOG = Logger.getLogger(ChartTiers.class);
    static final float MAX_COLONY_ITEM_CHANCE = 1f;
    static final int BOSS_EXTRA_LEVELS = 1;
    private static volatile Map<Integer, ChartTier> tiers = Map.of();
    private static volatile Map<Integer, Float> colonyItemChances = Map.of();

    private ChartTiers() {
    }

    public static void load() {
        ModCsv.load(TIER_COLUMN, PATH, LOG, ChartTiers::register);
    }

    static void register(JSONArray rows) throws JSONException {
        Map<Integer, ChartTier> loaded = new HashMap<>();
        Map<Integer, Float> loadedColonyItemChances = new HashMap<>();
        CsvRows csv = new CsvRows(PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String rowLabel = "row " + (index + 1);
                int tier = csv.integer(row, TIER_COLUMN, 0, rowLabel);
                if (tier < LOWEST_TIER || tier > HIGHEST_TIER) {
                    if (!ModCsv.text(row, TIER_COLUMN).isEmpty()) {
                        csv.skipRow(rowLabel, "tier must be " + LOWEST_TIER + " to " + HIGHEST_TIER);
                    }
                    return;
                }
                ChartTier fallback = ChartTier.fallback(tier);
                String label = "tier " + tier;
                loaded.put(tier, new ChartTier(tier,
                        csv.number(row, "fleet_size", fallback.fleetSize(), label),
                        csv.integer(row, "fleets_min", fallback.fleetsMin(), label),
                        csv.integer(row, "fleets_max", fallback.fleetsMax(), label),
                        csv.integer(row, "officer_level_bonus", fallback.officerLevelBonus(), label),
                        csv.integer(row, "officer_count_bonus", fallback.officerCountBonus(), label),
                        csv.number(row, "fleet_quality", fallback.fleetQuality(), label),
                        csv.integer(row, "level_min", fallback.levelMin(), label),
                        csv.integer(row, "level_max", fallback.levelMax(), label),
                        csv.number(row, "cache_budget", fallback.cacheBudget(), label),
                        csv.number(row, "domain_budget", fallback.domainBudget(), label),
                        csv.number(row, "station_budget", fallback.stationBudget(), label),
                        csv.number(row, "derelict_budget", fallback.derelictBudget(), label),
                        csv.flag(row, "boss", fallback.boss(), label),
                        csv.number(row, "boss_size", fallback.bossSize(), label),
                        csv.number(row, "weight_clear", fallback.clearWeight(), label),
                        csv.number(row, "weight_boss", fallback.bossWeight(), label)));
                float colonyItemChance = csv.number(row, "colony_item_chance", 0f, label);
                if (colonyItemChance < 0f || colonyItemChance > MAX_COLONY_ITEM_CHANCE) {
                    csv.problem(label, "colony_item_chance", colonyItemChance + " is outside 0 to 1 (a fraction, so 0.04 is 4%)");
                }
                loadedColonyItemChances.put(tier, Math.max(0f, Math.min(MAX_COLONY_ITEM_CHANCE, colonyItemChance)));
            });
        }
        csv.report(LOG);
        List<Integer> missing = new ArrayList<>();
        for (int tier = LOWEST_TIER; tier <= HIGHEST_TIER; tier++) {
            if (!loaded.containsKey(tier)) {
                missing.add(tier);
            }
        }
        if (!missing.isEmpty()) {
            LOG.warn("[ExiledSector] " + PATH + " has no row for tier(s) " + missing + "; those tiers use built-in values and have no colony item chance");
        }
        tiers = Map.copyOf(loaded);
        colonyItemChances = Map.copyOf(loadedColonyItemChances);
    }

    public static float colonyItemChance(int tier) {
        return colonyItemChances.getOrDefault(clamp(tier), 0f);
    }

    public static int firstColonyItemTier() {
        for (int tier = LOWEST_TIER; tier <= HIGHEST_TIER; tier++) {
            if (colonyItemChance(tier) > 0f) {
                return tier;
            }
        }
        return 0;
    }

    public static int tierForLevel(int level) {
        int matchingTier = LOWEST_TIER;
        for (int tier = LOWEST_TIER; tier <= HIGHEST_TIER; tier++) {
            if (forTier(tier).levelMin() <= level) {
                matchingTier = tier;
            }
        }
        return matchingTier;
    }

    public static int clamp(int tier) {
        return Math.max(LOWEST_TIER, Math.min(HIGHEST_TIER, tier));
    }

    public static boolean isValid(int tier) {
        return tier >= LOWEST_TIER && tier <= HIGHEST_TIER;
    }

    public static ChartTier forTier(int tier) {
        int clamped = clamp(tier);
        ChartTier loaded = tiers.get(clamped);
        return loaded != null ? loaded : ChartTier.fallback(clamped);
    }

    public static FleetLevelRange fleetStrengthRangeFor(MemoryAPI fleetMemory) {
        Object storedTier = fleetMemory == null ? null : fleetMemory.get(ChartIds.TIER_MEMORY_KEY);
        if (!(storedTier instanceof Number tierNumber)) {
            return null;
        }
        ChartTier tier = forTier(tierNumber.intValue());
        int bonus = fleetMemory.get(ChartIds.STRENGTH_BONUS_MEMORY_KEY) instanceof Number storedBonus ? Math.max(0, storedBonus.intValue()) : 0;
        int extraLevels = Boolean.TRUE.equals(fleetMemory.get(ChartIds.BOSS_MEMORY_KEY)) ? BOSS_EXTRA_LEVELS : 0;
        return new FleetLevelRange(scaledLevel(tier.levelMin(), bonus), scaledLevel(tier.levelMax(), bonus), extraLevels);
    }

    static int scaledLevel(int level, int strengthBonusPercent) {
        return Math.round(level * (1f + strengthBonusPercent / ChartTier.PERCENT));
    }

    public static void resetForTests() {
        tiers = Map.of();
        colonyItemChances = Map.of();
    }
}
