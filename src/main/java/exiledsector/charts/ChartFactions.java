package exiledsector.charts;

import exiledsector.ModCsv;
import exiledsector.items.WeightedPick;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public final class ChartFactions {

    static final String PATH = "data/config/exiledSector/chart_factions.csv";
    static final String FACTION_COLUMN = "faction";
    private static final Logger LOG = Logger.getLogger(ChartFactions.class);
    private static volatile List<ChartFaction> factions = List.of();

    private ChartFactions() {
    }

    public static void load() {
        ModCsv.load(FACTION_COLUMN, PATH, LOG, ChartFactions::register);
    }

    static void register(JSONArray rows) throws JSONException {
        Map<String, ChartFaction> loaded = new LinkedHashMap<>();
        ChartCsv csv = new ChartCsv(PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String factionId = ModCsv.text(row, FACTION_COLUMN);
                if (factionId.isEmpty()) {
                    return;
                }
                int minTier = ChartTiers.clamp(csv.integer(row, "min_tier", ChartTiers.LOWEST_TIER, factionId));
                loaded.put(factionId, new ChartFaction(factionId, csv.number(row, "weight", 1f, factionId), minTier,
                        csv.number(row, "fleet_size_bonus", 0f, factionId),
                        csv.number(row, "fleet_quality_bonus", 0f, factionId)));
            });
        }
        csv.report(LOG);
        factions = List.copyOf(loaded.values());
    }

    public static List<ChartFaction> all() {
        return factions;
    }

    static List<ChartFaction> availableAt(List<ChartFaction> candidates, int tier, Predicate<String> factionLoaded) {
        List<ChartFaction> available = new ArrayList<>();
        for (ChartFaction faction : candidates) {
            if (faction.allowedAt(tier) && faction.weight() > 0f && factionLoaded.test(faction.factionId())) {
                available.add(faction);
            }
        }
        return available;
    }

    static ChartFaction pick(List<ChartFaction> candidates, int tier, Predicate<String> factionLoaded, Random random) {
        List<ChartFaction> available = availableAt(candidates, tier, factionLoaded);
        ChartFaction picked = available.isEmpty() ? null : WeightedPick.pick(available, ChartFaction::weight, random);
        return picked != null ? picked : ChartFaction.FALLBACK;
    }

    static void resetForTests() {
        factions = List.of();
    }
}
