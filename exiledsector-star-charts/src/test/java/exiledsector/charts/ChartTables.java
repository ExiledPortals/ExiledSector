package exiledsector.charts;

import exiledlib.ProjectFiles;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.content.ChartPois;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.layout.feature.ChartFeatures;
import org.json.CDL;
import org.json.JSONArray;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class ChartTables {

    private ChartTables() {
    }

    public static JSONArray csvRows(String dataPath) throws Exception {
        String csv = Files.readString(ProjectFiles.projectRoot().resolve(dataPath), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
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
