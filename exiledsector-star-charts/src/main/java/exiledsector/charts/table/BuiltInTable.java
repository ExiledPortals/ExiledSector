package exiledsector.charts.table;

import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.layout.ChartLayoutGenerator;
import exiledsector.data.CsvRows;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

public final class BuiltInTable {

    public static final String ID_COLUMN = "id";

    public interface RowParser<D> {
        D parse(CsvRows csv, JSONObject row, String id);
    }

    public record TierRange(int minTier, int maxTier) {

        public static TierRange parse(CsvRows csv, JSONObject row, String id) {
            int min = ChartTiers.clamp(csv.integer(row, "min_tier", ChartTiers.LOWEST_TIER, id));
            int max = ChartTiers.clamp(csv.integer(row, "max_tier", ChartTiers.CORRUPTED_TIER, id));
            return new TierRange(Math.min(min, max), Math.max(min, max));
        }
    }

    private BuiltInTable() {
    }

    public static <T> T find(Collection<T> builtIn, Function<T, String> idOf, String id) {
        if (id == null) {
            return null;
        }
        for (T candidate : builtIn) {
            if (idOf.apply(candidate).equals(id)) {
                return candidate;
            }
        }
        return null;
    }

    public static <D> Map<String, D> rows(JSONArray rows, CsvRows csv, String noun, Predicate<String> builtIn, RowParser<D> parser) throws JSONException {
        Map<String, D> loaded = new LinkedHashMap<>();
        CsvRows.forEachIdRow(rows, ID_COLUMN, (id, row) -> {
            if (!builtIn.test(id)) {
                csv.skipRow(id, "no " + noun + " has this id");
                return;
            }
            D parsed = parser.parse(csv, row, id);
            if (parsed != null) {
                loaded.put(id, parsed);
            }
        });
        return loaded;
    }

    public static Random rollFor(long seed, String id, long salt) {
        return ChartLayoutGenerator.derivedRandom(seed ^ ((long) id.hashCode() << 32), salt);
    }
}
