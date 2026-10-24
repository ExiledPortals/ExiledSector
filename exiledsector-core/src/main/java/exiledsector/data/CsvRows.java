package exiledsector.data;

import exiledlib.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CsvRows {

    public static final String LIST_SEPARATOR = ";";
    public static final String WEIGHT_SEPARATOR = ":";
    static final Set<String> YES = Set.of("yes", "true", "y", "1");
    static final Set<String> NO = Set.of("no", "false", "n", "0");
    static final int MAX_LOGGED_PROBLEMS = 20;

    private final String path;
    private final List<String> problems = new ArrayList<>();

    public interface IdRowHandler {
        void accept(String id, JSONObject row) throws JSONException;
    }

    public CsvRows(String path) {
        this.path = path;
    }

    public static void forEachIdRow(JSONArray rows, String idColumn, IdRowHandler handler) throws JSONException {
        if (rows == null) {
            return;
        }
        ModCsv.forEach(rows, (index, row) -> {
            String id = ModCsv.text(row, idColumn);
            if (!id.isEmpty()) {
                handler.accept(id, row);
            }
        });
    }

    public static void forEachRow(JSONArray rows, String path, Logger log, ModCsv.RowHandler parser) throws JSONException {
        ModCsv.forEach(rows == null ? new JSONArray() : rows, (index, row) -> {
            try {
                parser.accept(index, row);
            } catch (IllegalArgumentException e) {
                log.error("Skipping row " + (index + 1) + " of " + path + ": " + e.getMessage());
            }
        });
    }

    public static List<String> entries(String listText) {
        List<String> entries = new ArrayList<>();
        for (String entry : listText.split(LIST_SEPARATOR)) {
            if (!entry.isBlank()) {
                entries.add(entry.trim());
            }
        }
        return entries;
    }

    public static float numberOrNaN(String numberText) {
        try {
            return Float.parseFloat(numberText);
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    public static boolean isValidChance(String chanceText) {
        float chance = numberOrNaN(chanceText);
        return chanceText.isEmpty() || chance >= 0f && !Float.isInfinite(chance);
    }

    public static float chance(String chanceText, String rowId, String column, String path, Logger log) {
        if (chanceText.isEmpty()) {
            return 0f;
        }
        if (!isValidChance(chanceText)) {
            log.error(rowId + " in " + path + " has the " + column + " \"" + chanceText + "\"; expected a number of 0 or more, so it is never used");
            return 0f;
        }
        return numberOrNaN(chanceText);
    }

    public int integer(JSONObject row, String column, int fallback, String rowLabel) {
        String text = ModCsv.text(row, column);
        if (text.isEmpty()) {
            return fallback;
        }
        try {
            return Math.round(Float.parseFloat(text));
        } catch (NumberFormatException e) {
            problem(rowLabel, column, text);
            return fallback;
        }
    }

    public float number(JSONObject row, String column, float fallback, String rowLabel) {
        String text = ModCsv.text(row, column);
        if (text.isEmpty()) {
            return fallback;
        }
        float parsed = numberOrNaN(text);
        if (Float.isNaN(parsed) || Float.isInfinite(parsed)) {
            problem(rowLabel, column, text);
            return fallback;
        }
        return parsed;
    }

    public boolean flag(JSONObject row, String column, boolean fallback, String rowLabel) {
        String text = ModCsv.text(row, column).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return fallback;
        }
        if (YES.contains(text)) {
            return true;
        }
        if (NO.contains(text)) {
            return false;
        }
        problem(rowLabel, column, text);
        return fallback;
    }

    public Map<String, Float> weights(JSONObject row, String column, String rowLabel, Set<String> knownIds) {
        return weights(row, column, rowLabel, knownIds, false);
    }

    public Map<String, Float> weights(JSONObject row, String column, String rowLabel, Set<String> knownIds, boolean negativeAllowed) {
        Map<String, Float> weights = new LinkedHashMap<>();
        for (String entry : entries(ModCsv.text(row, column))) {
            int separator = entry.indexOf(WEIGHT_SEPARATOR);
            String id = separator < 0 ? entry : entry.substring(0, separator).trim();
            float weight = separator < 0 ? 1f : numberOrNaN(entry.substring(separator + 1).trim());
            if (id.isEmpty() || Float.isNaN(weight) || Float.isInfinite(weight) || weight < 0f && !negativeAllowed) {
                problem(rowLabel, column, entry);
            } else if (knownIds != null && !knownIds.contains(id)) {
                problem(rowLabel, column, entry + " (unknown id)");
            } else {
                weights.put(id, weight);
            }
        }
        return weights;
    }

    public void problem(String rowLabel, String column, String text) {
        problems.add(rowLabel + " " + column + " = '" + text + "'");
    }

    public void skipRow(String rowLabel, String reason) {
        problems.add(rowLabel + " skipped: " + reason);
    }

    public List<String> problems() {
        return List.copyOf(problems);
    }

    public void report(Logger log) {
        if (problems.isEmpty()) {
            return;
        }
        List<String> shown = problems.size() > MAX_LOGGED_PROBLEMS ? problems.subList(0, MAX_LOGGED_PROBLEMS) : problems;
        log.warn("[ExiledSector] " + path + " has " + problems.size() + " problem(s); those values fall back to defaults: " + String.join("; ", shown)
                + (problems.size() > MAX_LOGGED_PROBLEMS ? "; ..." : ""));
    }
}
