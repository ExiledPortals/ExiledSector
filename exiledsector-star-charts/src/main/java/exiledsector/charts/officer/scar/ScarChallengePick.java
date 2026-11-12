package exiledsector.charts.officer.scar;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScarChallengePick {

    static final String FOCUS_PATH = ExiledSector.configPath("npc_faction_focus.csv");
    private static final String FACTION_COLUMN = "faction";
    private static final String FOCUS_COLUMN = "focus";
    private static final Logger LOG = Logger.getLogger(ScarChallengePick.class);
    private static volatile Map<String, List<String>> tagsByFaction = Map.of();

    private ScarChallengePick() {
    }

    public static void load() {
        ModCsv.load(FACTION_COLUMN, FOCUS_PATH, LOG, ScarChallengePick::register);
    }

    public static List<String> register(JSONArray rows) throws JSONException {
        Map<String, List<String>> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(FOCUS_PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String factionId = ModCsv.text(row, FACTION_COLUMN);
                if (!factionId.isEmpty()) {
                    loaded.put(factionId, strongestFirst(csv.weights(row, FOCUS_COLUMN, factionId, null)));
                }
            });
        }
        tagsByFaction = Map.copyOf(loaded);
        return csv.problems();
    }

    static List<String> strongestFirst(Map<String, Float> weightsByTag) {
        List<Map.Entry<String, Float>> entries = new ArrayList<>(weightsByTag.entrySet());
        entries.sort((first, second) -> Float.compare(second.getValue(), first.getValue()));
        List<String> tags = new ArrayList<>();
        entries.forEach(entry -> tags.add(entry.getKey()));
        return List.copyOf(tags);
    }

    public static void clear() {
        tagsByFaction = Map.of();
    }

    public static String pick(String winnerFactionId, Map<String, Integer> currentScars, HullFacts hull) {
        return pick(tagsByFaction.getOrDefault(winnerFactionId == null ? "" : winnerFactionId, List.of()), ScarTable.rows(), currentScars, hull);
    }

    static String pick(List<String> focusTags, List<ScarRow> rows, Map<String, Integer> currentScars, HullFacts hull) {
        for (String tag : focusTags) {
            for (ScarRow row : rows) {
                if (tag.equals(row.themeTag()) && ScarScoring.eligible(row, currentScars, hull)) {
                    return row.id();
                }
            }
        }
        return ScarTable.FALLBACK_SCAR_ID;
    }
}
