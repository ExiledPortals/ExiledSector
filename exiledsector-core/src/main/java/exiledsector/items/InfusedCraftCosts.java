package exiledsector.items;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import exiledsector.skills.tags.ThemeTags;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class InfusedCraftCosts {

    static final String DATA_FILE = "infused_crafts.csv";
    static final String TAG_COLUMN = "tag";
    static final String FEEDSTOCK_COLUMN = "feedstock";
    private static final Logger LOG = Logger.getLogger(InfusedCraftCosts.class);
    private static final AtomicReference<Map<String, TagCosts>> COSTS_BY_TAG = new AtomicReference<>(Map.of());

    public record Cost(String feedstockId, int amount) {
    }

    private record TagCosts(String feedstockId, Map<InfusedCraft, Integer> amounts) {
    }

    private InfusedCraftCosts() {
    }

    public static String dataPath() {
        return ExiledSector.configPath(DATA_FILE);
    }

    public static void load() {
        ModCsv.load(TAG_COLUMN, dataPath(), LOG, InfusedCraftCosts::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        CsvRows csv = new CsvRows(dataPath());
        Map<String, TagCosts> loaded = new LinkedHashMap<>();
        CsvRows.forEachIdRow(rows, TAG_COLUMN, (tag, row) -> {
            TagCosts costs = parse(csv, tag, row);
            if (costs != null) {
                loaded.put(tag, costs);
            }
        });
        COSTS_BY_TAG.set(Collections.unmodifiableMap(loaded));
        csv.report(LOG);
    }

    private static TagCosts parse(CsvRows csv, String tag, JSONObject row) {
        if (!ThemeTags.isThemeTag(tag)) {
            csv.skipRow(tag, "it is not a theme tag in " + ThemeTags.PATH);
            return null;
        }
        String feedstockId = ModCsv.text(row, FEEDSTOCK_COLUMN);
        if (feedstockId.isEmpty()) {
            csv.skipRow(tag, "it names no feedstock");
            return null;
        }
        Map<InfusedCraft, Integer> amounts = new EnumMap<>(InfusedCraft.class);
        for (InfusedCraft craft : InfusedCraft.values()) {
            int amount = csv.integer(row, craft.id(), 0, tag);
            if (amount > 0) {
                amounts.put(craft, amount);
            }
        }
        return new TagCosts(feedstockId, Map.copyOf(amounts));
    }

    public static Cost cost(InfusedCraft craft, String tag) {
        TagCosts costs = tag == null ? null : COSTS_BY_TAG.get().get(tag);
        Integer amount = costs == null ? null : costs.amounts().get(craft);
        return amount == null ? null : new Cost(costs.feedstockId(), amount);
    }

    public static List<String> tags(InfusedCraft craft) {
        return COSTS_BY_TAG.get().entrySet().stream().filter(entry -> entry.getValue().amounts().containsKey(craft)).map(Map.Entry::getKey).toList();
    }

    public static String feedstockFor(String tag) {
        TagCosts costs = tag == null ? null : COSTS_BY_TAG.get().get(tag);
        return costs == null ? null : costs.feedstockId();
    }

    public static void clear() {
        COSTS_BY_TAG.set(Map.of());
    }
}
