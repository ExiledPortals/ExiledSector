package exiledsector.charts.effect;

import exiledsector.ModCsv;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.data.CsvRows;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ScaleContext;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChartPrefixRewards {

    public static final String PATH = "data/config/exiledSector/star_chart_prefix_rewards.csv";
    private static final String ID_COLUMN = "id";
    private static final Map<String, ChartModifier> REWARD_COLUMNS = rewardColumns();
    private static final Logger LOG = Logger.getLogger(ChartPrefixRewards.class);
    private static volatile Map<String, List<Reward>> rewardsByPrefix = Map.of();

    private record Reward(ChartModifier effect, ModifierLine values) {
    }

    private ChartPrefixRewards() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, ChartPrefixRewards::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, List<Reward>> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(PATH);
        CsvRows.forEachIdRow(rows, ID_COLUMN, (prefixId, row) -> {
            List<Reward> rewards = new ArrayList<>();
            REWARD_COLUMNS.forEach((column, effect) -> {
                String values = ModCsv.text(row, column);
                if (values.isEmpty()) {
                    return;
                }
                try {
                    rewards.add(new Reward(effect, ModifierLine.parse(effect.name() + ":" + values).line()));
                } catch (IllegalArgumentException e) {
                    csv.problem(prefixId, column, values + " (" + e.getMessage() + ")");
                }
            });
            loaded.put(prefixId, List.copyOf(rewards));
        });
        csv.report(LOG);
        rewardsByPrefix = Map.copyOf(loaded);
    }

    public static List<AppliedEffect> rewards(String prefixId, int tier) {
        List<Reward> rewards = prefixId == null ? List.of() : rewardsByPrefix.getOrDefault(prefixId, List.of());
        if (rewards.isEmpty()) {
            return List.of();
        }
        ScaleContext scale = ScaleContext.ofTier(tier);
        List<AppliedEffect> applied = new ArrayList<>(rewards.size());
        for (Reward reward : rewards) {
            if (reward.values().hasValueFor(scale)) {
                applied.add(new AppliedEffect(reward.effect(), reward.values().valueFor(reward.values().min(), scale)));
            }
        }
        return applied;
    }

    public static void resetForTests() {
        rewardsByPrefix = Map.of();
    }

    private static Map<String, ChartModifier> rewardColumns() {
        Map<String, ChartModifier> columns = new LinkedHashMap<>();
        columns.put("item_quantity", ChartModifier.CHART_QUANTITY_PERCENT);
        columns.put("item_rarity", ChartModifier.CHART_ITEM_RARITY_PERCENT);
        columns.put("fleet_size", ChartModifier.CHART_FLEET_SIZE_PERCENT);
        columns.put("hull_items", ChartModifier.CHART_ITEM_PERCENT);
        columns.put("crafting_items", ChartModifier.CHART_CRAFTING_ITEM_PERCENT);
        columns.put("star_charts", ChartModifier.CHART_STAR_CHART_PERCENT);
        columns.put("catalysts", ChartModifier.CHART_CATALYST_PERCENT);
        return Collections.unmodifiableMap(columns);
    }
}
