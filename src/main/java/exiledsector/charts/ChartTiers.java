package exiledsector.charts;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.ModCsv;
import exiledsector.skills.npc.NpcTreeSize;
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
    private static volatile Map<Integer, ChartTier> tiers = Map.of();

    private ChartTiers() {
    }

    public static void load() {
        ModCsv.load(TIER_COLUMN, PATH, LOG, ChartTiers::register);
    }

    static void register(JSONArray rows) throws JSONException {
        Map<Integer, ChartTier> loaded = new HashMap<>();
        ChartCsv csv = new ChartCsv(PATH);
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
                        csv.number(row, "fleet_power", fallback.fleetPower(), label),
                        csv.integer(row, "fleets_min", fallback.fleetsMin(), label),
                        csv.integer(row, "fleets_max", fallback.fleetsMax(), label),
                        csv.integer(row, "officer_level_bonus", fallback.officerLevelBonus(), label),
                        csv.integer(row, "officer_count_bonus", fallback.officerCountBonus(), label),
                        csv.number(row, "quality", fallback.quality(), label),
                        csv.integer(row, "average_smods", fallback.averageSMods(), label),
                        csv.integer(row, "tree_percent_min", fallback.treePercentMin(), label),
                        csv.integer(row, "tree_percent_max", fallback.treePercentMax(), label),
                        csv.number(row, "loot_budget", fallback.lootBudget(), label),
                        csv.flag(row, "boss", fallback.boss(), label),
                        csv.number(row, "boss_power", fallback.bossPower(), label),
                        csv.number(row, "weight_clear", fallback.clearWeight(), label),
                        csv.number(row, "weight_boss", fallback.bossWeight(), label),
                        csv.number(row, "weight_vault", fallback.vaultWeight(), label)));
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
            LOG.warn("[ExiledSector] " + PATH + " has no row for tier(s) " + missing + "; those tiers use built-in values");
        }
        tiers = Map.copyOf(loaded);
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

    public static NpcTreeSize.PercentRange treePercentRangeFor(MemoryAPI fleetMemory) {
        Object storedTier = fleetMemory == null ? null : fleetMemory.get(ChartIds.TIER_MEMORY_KEY);
        if (!(storedTier instanceof Number tierNumber)) {
            return null;
        }
        ChartTier tier = forTier(tierNumber.intValue());
        int lowest = Math.max(NpcTreeSize.LOWEST_PERCENT, Math.min(NpcTreeSize.HIGHEST_PERCENT, tier.treePercentMin()));
        int highest = Math.max(lowest, Math.min(NpcTreeSize.HIGHEST_PERCENT, tier.treePercentMax()));
        return new NpcTreeSize.PercentRange(lowest, highest);
    }

    static void resetForTests() {
        tiers = Map.of();
    }
}
