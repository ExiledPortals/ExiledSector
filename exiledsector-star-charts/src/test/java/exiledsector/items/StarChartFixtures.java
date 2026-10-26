package exiledsector.items;

import exiledsector.charts.effect.ChartEffects;
import exiledsector.charts.content.ChartFactions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public final class StarChartFixtures {

    static final String CHART_FACTIONS_PATH = "data/config/exiledSector/chart_factions.csv";
    public static final String TEST_UNIQUE = "test_unique_chart";
    public static final String TEST_UNIQUE_NAME = "Test Unique Chart";
    public static final String TEST_GATED_UNIQUE = "test_gated_unique_chart";
    public static final String TEST_GATED_UNIQUE_NAME = "Test Gated Unique Chart";
    public static final int TEST_GATED_UNIQUE_MIN_TIER = 4;

    private StarChartFixtures() {
    }

    public static void registerShipped() throws Exception {
        ChartEffects.register();
        ChartFactions.register(HullItemRows.shippedRows(CHART_FACTIONS_PATH));
        StarChartItems.register(withTestUniqueModifiers(HullItemRows.shippedRows(StarChartItems.MODIFIERS_PATH)),
                HullItemRows.shippedRows(StarChartImplicits.DATA_PATH), withTestUniques(HullItemRows.shippedRows(StarChartDefinitions.DATA_PATH)));
    }

    private static JSONArray withTestUniqueModifiers(JSONArray rows) throws JSONException {
        return rows.put(HullItemRows.modifierRow("TEST_UNIQUE_SENSOR_RANGE", "prefix", "star_chart", "CHART_SENSOR_RANGE_PERCENT:t1=-50", ""))
                .put(HullItemRows.modifierRow("TEST_UNIQUE_EXTRA_POIS", "suffix", "star_chart", "CHART_EXTRA_POIS:t1=4/t8=6", ""))
                .put(HullItemRows.modifierRow("TEST_GATED_UNIQUE_BOSS_SIZE", "prefix", "star_chart", "CHART_BOSS_SIZE_PERCENT:t1=200/t10=700", ""))
                .put(HullItemRows.modifierRow("TEST_GATED_UNIQUE_BOSS_SPOILS", "suffix", "star_chart", "CHART_BOSS_SPOILS_PERCENT:t1=100/t10=150", ""));
    }

    private static JSONArray withTestUniques(JSONArray rows) throws JSONException {
        return rows.put(uniqueRow(TEST_UNIQUE, "BINARY_FAR", TEST_UNIQUE_NAME, 1, "@TEST_UNIQUE_SENSOR_RANGE; @HYBRID_CHART_FLEET_QUALITY",
                        "@TEST_UNIQUE_EXTRA_POIS; @CHART_ITEM_PERCENT", ""))
                .put(uniqueRow(TEST_GATED_UNIQUE, "TRINARY_2FAR", TEST_GATED_UNIQUE_NAME, TEST_GATED_UNIQUE_MIN_TIER,
                        "@TEST_GATED_UNIQUE_BOSS_SIZE; @HYBRID_CHART_OFFICER_LEVEL", "@TEST_GATED_UNIQUE_BOSS_SPOILS; @CHART_REWARD_PERCENT",
                        ItemUnlock.CLEARED_CHART_TIER_PREFIX + "3"));
    }

    private static JSONObject uniqueRow(String id, String systemType, String name, int minTier, String prefixes, String suffixes, String unlock)
            throws JSONException {
        return new JSONObject().put("id", id).put("system_type", systemType).put("name", name).put("icon", "graphics/icons/colony_management.png")
                .put("rarity", "1").put("unique", "true").put("min_tier", String.valueOf(minTier)).put("description", "")
                .put("prefixes", prefixes).put("suffixes", suffixes).put("unlock", unlock);
    }

    public static ItemDefinition definition(String definitionId) {
        return ItemDefinitions.get(definitionId);
    }

    public static StarChart chart(String definitionId, long seed, int tier) {
        return (StarChart) ItemData.rolledAtTier(ItemDefinitions.get(definitionId), seed, tier).preview();
    }

    public static void clear() {
        StarChartImplicits.registerSharedChoice(List.of());
        StarChartItems.clear();
    }
}
