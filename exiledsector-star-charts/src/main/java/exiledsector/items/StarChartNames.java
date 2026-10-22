package exiledsector.items;

import com.fs.starfarer.api.Global;
import exiledlib.items.ItemNames;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static exiledsector.ExiledSector.MOD_ID;

public final class StarChartNames {

    static final String DATA_PATH = "data/config/exiledSector/star_chart_names.json";
    private static final Logger LOG = Logger.getLogger(StarChartNames.class);

    private StarChartNames() {
    }

    public static void load() {
        try {
            register(Global.getSettings().getMergedJSONForMod(DATA_PATH, MOD_ID));
        } catch (IOException | JSONException | RuntimeException e) {
            LOG.error("Failed to load " + DATA_PATH, e);
        }
    }

    public static void register(JSONObject wordsJson) throws JSONException {
        if (wordsJson == null) {
            ItemNames.useStarChartWords(null);
            return;
        }
        ItemNames.useStarChartWords(new ItemNames.NameWords(style(wordsJson.optString("style", "codename")), words(wordsJson.optJSONArray("first")),
                words(wordsJson.optJSONArray("second")), words(wordsJson.optJSONArray("models"))));
    }

    private static ItemNames.Style style(String styleName) {
        try {
            return ItemNames.Style.valueOf(styleName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOG.warn("Unknown name style \"" + styleName + "\" in " + DATA_PATH + " - using codename");
            return ItemNames.Style.CODENAME;
        }
    }

    private static List<String> words(JSONArray wordsArray) throws JSONException {
        Set<String> values = new LinkedHashSet<>();
        for (int i = 0; wordsArray != null && i < wordsArray.length(); i++) {
            String word = wordsArray.getString(i).trim();
            if (!word.isEmpty()) {
                values.add(word);
            }
        }
        return List.copyOf(values);
    }

    public static void clear() {
        ItemNames.useStarChartWords(null);
    }
}
