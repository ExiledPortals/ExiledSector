package exiledsector.charts;

import exiledsector.ProjectFiles;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.content.ChartPois;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.layout.feature.ChartFeatures;
import org.json.JSONArray;

import java.util.List;

public final class ChartTables {

    private ChartTables() {
    }

    public static JSONArray csvRows(String dataPath) throws Exception {
        return ProjectFiles.csvRows(dataPath);
    }

    public static void loadFeatures() throws Exception {
        ChartFeatures.register(csvRows(ChartFeatures.PATH));
    }

    public static void loadShipped() throws Exception {
        ChartTiers.register(csvRows(ChartTiers.PATH));
        ChartPois.register(csvRows(ChartPois.PATH));
        ChartFactions.register(csvRows(ChartFactions.PATH));
    }

    public static void loadAnomalies() throws Exception {
        ChartAnomalies.register(csvRows(ChartAnomalies.PATH));
    }

    public static void useAnomalies(List<ChartAnomalies.Definition> definitions) {
        ChartAnomalies.useForTests(definitions);
    }

    public static void useAnomalies(List<ChartAnomaly> anomalies, List<ChartAnomalies.Definition> definitions) {
        ChartAnomalies.useForTests(anomalies, definitions);
    }

    public static void resetAnomalies() {
        ChartAnomalies.resetForTests();
    }
}
