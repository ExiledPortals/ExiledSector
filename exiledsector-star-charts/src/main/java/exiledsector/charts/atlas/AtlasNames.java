package exiledsector.charts.atlas;

import exiledsector.i18n.Translation;

import java.util.Locale;

public final class AtlasNames {

    private AtlasNames() {
    }

    public static String name(String category, String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        String key = "chart.atlas." + category + "." + id;
        return Translation.has(key) ? Translation.text(key) : tidy(id);
    }

    public static String anomaly(String anomalyId) {
        return name("anomaly", anomalyId);
    }

    static String tidy(String id) {
        String[] words = id.replace('-', '_').split("_");
        StringBuilder tidied = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (tidied.length() > 0) {
                tidied.append(' ');
            }
            tidied.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return tidied.toString();
    }
}
