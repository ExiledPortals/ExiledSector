package exiledsector.charts;

import exiledlib.ProjectFiles;
import org.json.CDL;
import org.json.JSONArray;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class ChartTables {

    private ChartTables() {
    }

    public static JSONArray csvRows(String dataPath) throws Exception {
        String csv = Files.readString(ProjectFiles.projectRoot().resolve(dataPath), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }

    public static void loadShipped() throws Exception {
        ChartTiers.register(csvRows(ChartTiers.PATH));
        ChartPois.register(csvRows(ChartPois.PATH));
        ChartFactions.register(csvRows(ChartFactions.PATH));
    }
}
