package exiledsector.items;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;

public final class StarChartItems {

    static final String MODIFIERS_PATH = "data/config/exiledSector/star_chart_modifiers.csv";
    private static final Logger LOG = Logger.getLogger(StarChartItems.class);

    private StarChartItems() {
    }

    public static void load() {
        ModCsv.load("id", MODIFIERS_PATH, LOG, rows -> ItemModifiers.registerPart(ModifierScope.STAR_CHART, rows, MODIFIERS_PATH));
        StarChartImplicits.load();
        StarChartDefinitions.load();
        StarChartNames.load();
    }
}
