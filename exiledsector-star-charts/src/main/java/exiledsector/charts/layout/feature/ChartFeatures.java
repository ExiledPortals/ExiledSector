package exiledsector.charts.layout.feature;

import exiledsector.ModCsv;
import exiledsector.charts.table.BuiltInTable;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.Set;

public final class ChartFeatures {

    public static final String PATH = "data/config/exiledSector/atlas_features.csv";
    static final String ID_COLUMN = "id";
    static final List<ChartFeature> BUILT_IN = List.of(RingedGiant.INSTANCE, DebrisRing.INSTANCE, TwinWorlds.INSTANCE, RoguePlanet.INSTANCE);
    private static final Logger LOG = Logger.getLogger(ChartFeatures.class);
    private static volatile Set<String> listed = Set.of();

    private ChartFeatures() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, ChartFeatures::register);
    }

    public static List<String> register(JSONArray rows) throws JSONException {
        CsvRows csv = new CsvRows(PATH);
        Set<String> loaded = BuiltInTable.rows(rows, csv, "feature", ChartFeatures::isKnown, (rowCsv, row, id) -> id).keySet();
        csv.report(LOG);
        listed = Set.copyOf(loaded);
        return csv.problems();
    }

    public static ChartFeature byId(String id) {
        return id != null && listed.contains(id) ? built(id) : null;
    }

    public static boolean isKnown(String id) {
        return built(id) != null;
    }

    private static ChartFeature built(String id) {
        return BuiltInTable.find(BUILT_IN, ChartFeature::id, id);
    }

    static void useForTests(Set<String> ids) {
        listed = Set.copyOf(ids);
    }

    static void resetForTests() {
        listed = Set.of();
    }
}
