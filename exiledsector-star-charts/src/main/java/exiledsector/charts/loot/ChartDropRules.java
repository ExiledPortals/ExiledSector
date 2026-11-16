package exiledsector.charts.loot;

import exiledsector.ModCsv;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.data.CsvRows;
import exiledsector.data.WeightedPick;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class ChartDropRules {

    static final String PATH = "data/config/exiledSector/chart_drops.csv";
    static final String SOURCE_COLUMN = "source";
    static final String CIPHER_CHANCE_COLUMN = "cipher_chance";
    static final String CATALYST_CHANCE_COLUMN = "catalyst_chance";
    static final List<String> TIER_OFFSET_COLUMNS = List.of("minus_2", "minus_1", "same_tier", "plus_1", "plus_2");
    static final int LOWEST_TIER_OFFSET = -2;
    static final List<Float> SAME_TIER_ONLY = List.of(0f, 0f, 1f, 0f, 0f);
    private static final Logger LOG = Logger.getLogger(ChartDropRules.class);
    private static volatile Map<ChartDrops.Source, Rule> rules = Map.of();

    record Rule(int guaranteed, float chance, float extraChance, List<Float> tierWeights, float cipherChance, float catalystChance, float minDefeatedDp,
                float defeatedDpPerTier, int maxTier) {

        Rule {
            guaranteed = Math.max(0, guaranteed);
            chance = Math.max(0f, chance);
            extraChance = Math.max(0f, extraChance);
            tierWeights = tierWeights == null || tierWeights.size() != TIER_OFFSET_COLUMNS.size()
                    || tierWeights.stream().noneMatch(weight -> weight > 0f) ? SAME_TIER_ONLY : List.copyOf(tierWeights);
            cipherChance = Math.max(0f, cipherChance);
            catalystChance = Math.max(0f, catalystChance);
            minDefeatedDp = Math.max(0f, minDefeatedDp);
            defeatedDpPerTier = Math.max(0f, defeatedDpPerTier);
            maxTier = ChartTiers.clampDropTier(maxTier);
        }

        int pickTierOffset(Random random) {
            int index = WeightedPick.index(tierWeights, weight -> Math.max(0f, weight), random.nextFloat());
            return index + LOWEST_TIER_OFFSET;
        }
    }

    private ChartDropRules() {
    }

    public static void load() {
        ModCsv.load(SOURCE_COLUMN, PATH, LOG, ChartDropRules::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<ChartDrops.Source, Rule> loaded = new EnumMap<>(ChartDrops.Source.class);
        CsvRows csv = new CsvRows(PATH);
        ModCsv.forEach(rows, (index, row) -> {
            String sourceText = ModCsv.text(row, SOURCE_COLUMN);
            if (sourceText.isEmpty()) {
                return;
            }
            String rowLabel = "row " + (index + 1);
            ChartDrops.Source source = ChartDrops.Source.byId(sourceText.toLowerCase(Locale.ROOT));
            if (source == null) {
                csv.skipRow(rowLabel, "unknown source " + sourceText);
                return;
            }
            float cipherChance = csv.number(row, CIPHER_CHANCE_COLUMN, 0f, sourceText);
            if (cipherChance > 0f && !source.insideSystem()) {
                csv.problem(rowLabel, CIPHER_CHANCE_COLUMN, sourceText + " is outside a chart system, where Star Chart ciphers never drop");
                cipherChance = 0f;
            }
            float catalystChance = csv.number(row, CATALYST_CHANCE_COLUMN, 0f, sourceText);
            if (catalystChance > 0f && !source.insideSystem()) {
                csv.problem(rowLabel, CATALYST_CHANCE_COLUMN, sourceText + " is outside a chart system, where Atlas Catalysts never drop");
                catalystChance = 0f;
            }
            List<Float> tierWeights = new ArrayList<>();
            for (String column : TIER_OFFSET_COLUMNS) {
                tierWeights.add(csv.number(row, column, 0f, sourceText));
            }
            loaded.put(source, new Rule(csv.integer(row, "guaranteed", 0, sourceText), csv.number(row, "chance", 0f, sourceText),
                    csv.number(row, "extra_chance", 0f, sourceText),
                    tierWeights, cipherChance, catalystChance,
                    csv.number(row, "min_defeated_dp", 0f, sourceText), csv.number(row, "defeated_dp_per_tier", 0f, sourceText),
                    csv.integer(row, "max_tier", ChartTiers.HIGHEST_DROP_TIER, sourceText)));
        });
        csv.report(LOG);
        rules = Map.copyOf(loaded);
    }

    public static void clear() {
        rules = Map.of();
    }

    static Rule rule(ChartDrops.Source source) {
        return source == null ? null : rules.get(source);
    }

    public static boolean hasRule(ChartDrops.Source source) {
        return rule(source) != null;
    }
}
