package exiledsector.items;

import exiledsector.Seeds;
import exiledsector.charts.effect.ChartEffects;
import exiledsector.charts.content.ChartFactions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class StarChartFixtures {

    static final String CHART_FACTIONS_PATH = "data/config/exiledSector/chart_factions.csv";
    public static final String TEST_FIXED = "test_fixed_chart";
    public static final String TEST_FIXED_NAME = "Test Fixed Chart";
    public static final String TEST_GATED = "test_gated_chart";
    public static final String TEST_GATED_NAME = "Test Gated Chart";
    public static final int TEST_GATED_MIN_TIER = 4;

    private StarChartFixtures() {
    }

    public static void registerShipped() throws Exception {
        ChartEffects.register();
        ChartFactions.register(HullItemRows.shippedRows(CHART_FACTIONS_PATH));
        StarChartItems.register(withTestModifiers(HullItemRows.shippedRows(StarChartItems.MODIFIERS_PATH)),
                HullItemRows.shippedRows(StarChartImplicits.DATA_PATH), withTestCharts(HullItemRows.shippedRows(StarChartDefinitions.DATA_PATH)));
    }

    private static JSONArray withTestModifiers(JSONArray rows) throws JSONException {
        return rows.put(HullItemRows.modifierRow("TEST_SENSOR_RANGE", "prefix", "star_chart", "CHART_SENSOR_RANGE_PERCENT:t1=-50", ""))
                .put(HullItemRows.modifierRow("TEST_EXTRA_POIS", "suffix", "star_chart", "CHART_EXTRA_POIS:t1=4/t8=6", ""))
                .put(HullItemRows.modifierRow("TEST_GATED_BOSS_SIZE", "prefix", "star_chart", "CHART_BOSS_SIZE_PERCENT:t1=200/t10=700", ""))
                .put(HullItemRows.modifierRow("TEST_GATED_BOSS_SPOILS", "suffix", "star_chart", "CHART_BOSS_SPOILS_PERCENT:t1=100/t10=150", ""));
    }

    private static JSONArray withTestCharts(JSONArray rows) throws JSONException {
        return rows.put(chartRow(TEST_FIXED, "BINARY_FAR", TEST_FIXED_NAME, 1, "@TEST_SENSOR_RANGE; @HYBRID_CHART_FLEET_QUALITY",
                        "@TEST_EXTRA_POIS; @CHART_ITEM_PERCENT"))
                .put(chartRow(TEST_GATED, "TRINARY_2FAR", TEST_GATED_NAME, TEST_GATED_MIN_TIER,
                        "@TEST_GATED_BOSS_SIZE; @HYBRID_CHART_OFFICER_LEVEL", "@TEST_GATED_BOSS_SPOILS; @CHART_REWARD_PERCENT"));
    }

    private static JSONObject chartRow(String id, String systemType, String name, int minTier, String prefixes, String suffixes)
            throws JSONException {
        return new JSONObject().put("id", id).put("system_type", systemType).put("name", name).put("icon", "graphics/icons/colony_management.png")
                .put("rarity", "1").put("unique", "").put("min_tier", String.valueOf(minTier)).put("description", "")
                .put("prefixes", prefixes).put("suffixes", suffixes).put("unlock", "");
    }

    public static ItemDefinition definition(String definitionId) {
        return ItemDefinitions.get(definitionId);
    }

    public static StarChart chart(String definitionId, long seed, int tier) {
        return (StarChart) ItemData.rolledAtTier(ItemDefinitions.get(definitionId), seed, tier).preview();
    }

    public static ItemData withEveryModifier(String definitionId, long seed, int tier) {
        ItemDefinition definition = ItemDefinitions.get(definitionId);
        ItemData rolled = ItemData.rolledAtTier(definition, seed, tier);
        Random random = Seeds.random(seed);
        List<RolledModifier> explicits = new ArrayList<>(definition.pool().size());
        for (PoolEntry entry : definition.pool()) {
            explicits.add(RolledModifier.roll(entry.modifier(), random, ItemRoller.averageRolls()));
        }
        return new ItemData(definition.id(), seed, explicits, ItemNames.freeze(definition, seed, explicits), rolled.traits());
    }

    public static StarChart fullChart(String definitionId, long seed, int tier) {
        return (StarChart) withEveryModifier(definitionId, seed, tier).preview();
    }

    public static void clear() {
        StarChartImplicits.registerSharedChoice(List.of());
        StarChartItems.clear();
    }
}
