package exiledsector.charts;

import exiledlib.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class ChartCsv {

    static final Set<String> YES = Set.of("yes", "true", "y", "1");
    static final Set<String> NO = Set.of("no", "false", "n", "0");
    static final int MAX_LOGGED_PROBLEMS = 20;

    private final String path;
    private final List<String> problems = new ArrayList<>();

    ChartCsv(String path) {
        this.path = path;
    }

    int integer(JSONObject row, String column, int fallback, String rowLabel) {
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

    float number(JSONObject row, String column, float fallback, String rowLabel) {
        String text = ModCsv.text(row, column);
        if (text.isEmpty()) {
            return fallback;
        }
        try {
            float parsed = Float.parseFloat(text);
            if (Float.isNaN(parsed) || Float.isInfinite(parsed)) {
                problem(rowLabel, column, text);
                return fallback;
            }
            return parsed;
        } catch (NumberFormatException e) {
            problem(rowLabel, column, text);
            return fallback;
        }
    }

    boolean flag(JSONObject row, String column, boolean fallback, String rowLabel) {
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

    void problem(String rowLabel, String column, String text) {
        problems.add(rowLabel + " " + column + " = '" + text + "'");
    }

    void skipRow(String rowLabel, String reason) {
        problems.add(rowLabel + " skipped: " + reason);
    }

    List<String> problems() {
        return List.copyOf(problems);
    }

    void report(Logger log) {
        if (problems.isEmpty()) {
            return;
        }
        List<String> shown = problems.size() > MAX_LOGGED_PROBLEMS ? problems.subList(0, MAX_LOGGED_PROBLEMS) : problems;
        log.warn("[ExiledSector] " + path + " has " + problems.size() + " problem(s); those values fall back to defaults: " + String.join("; ", shown)
                + (problems.size() > MAX_LOGGED_PROBLEMS ? "; ..." : ""));
    }
}
