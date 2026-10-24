package exiledsector.charts;

import exiledlib.ModCsv;
import exiledsector.charts.feature.ChartFeature;
import exiledsector.charts.feature.DebrisRing;
import exiledsector.charts.feature.RingedGiant;
import exiledsector.charts.feature.RoguePlanet;
import exiledsector.charts.feature.TwinWorlds;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ChartFeatures {

    static final String PATH = "data/config/exiledSector/atlas_features.csv";
    static final String ID_COLUMN = "id";
    static final List<ChartFeature> BUILT_IN = List.of(RingedGiant.INSTANCE, DebrisRing.INSTANCE, TwinWorlds.INSTANCE, RoguePlanet.INSTANCE);
    private static final Logger LOG = Logger.getLogger(ChartFeatures.class);
    private static volatile Set<String> listed = Set.of();

    private ChartFeatures() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, ChartFeatures::register);
    }

    static List<String> register(JSONArray rows) throws JSONException {
        Set<String> loaded = new LinkedHashSet<>();
        CsvRows csv = new CsvRows(PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String id = ModCsv.text(row, ID_COLUMN);
                if (id.isEmpty()) {
                    return;
                }
                if (built(id) == null) {
                    csv.skipRow(id, "no feature has this id");
                    return;
                }
                loaded.add(id);
            });
        }
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
        for (ChartFeature feature : BUILT_IN) {
            if (feature.id().equals(id)) {
                return feature;
            }
        }
        return null;
    }

    static void useForTests(Set<String> ids) {
        listed = Set.copyOf(ids);
    }

    static void resetForTests() {
        listed = Set.of();
    }
}
