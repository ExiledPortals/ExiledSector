package exiledsector.items;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

public final class StarChartItems {

    static final String MODIFIERS_PATH = "data/config/exiledSector/star_chart_modifiers.csv";
    private static final Logger LOG = Logger.getLogger(StarChartItems.class);

    private StarChartItems() {
    }

    public static void load() {
        ModCsv.load("id", MODIFIERS_PATH, LOG, rows -> ItemModifiers.registerStarCharts(rows, MODIFIERS_PATH));
        StarChartImplicits.load();
        StarChartDefinitions.load();
    }

    public static void register(JSONArray modifierRows, JSONArray implicitRows, JSONArray definitionRows) throws JSONException {
        ItemModifiers.registerStarCharts(modifierRows, MODIFIERS_PATH);
        StarChartImplicits.register(implicitRows);
        StarChartDefinitions.register(definitionRows);
    }

    public static void clear() {
        StarChartDefinitions.clear();
        StarChartImplicits.clear();
        ItemModifiers.clearStarCharts();
    }
}
