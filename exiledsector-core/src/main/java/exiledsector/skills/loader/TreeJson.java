package exiledsector.skills.loader;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class TreeJson {

    public interface EntryParser {
        void parse(JSONObject entryJson) throws JSONException;
    }

    private TreeJson() {
    }

    public static String entryLabel(JSONArray entries, int entryIndex) {
        JSONObject entry = entries.optJSONObject(entryIndex);
        String entryId = entry == null ? null : entry.optString("id", null);
        return entryId != null ? "\"" + entryId + "\"" : "#" + entryIndex;
    }

    public static List<String> stringList(JSONArray stringArray) throws JSONException {
        List<String> values = new ArrayList<>();
        if (stringArray == null) {
            return values;
        }
        for (int i = 0; i < stringArray.length(); i++) {
            values.add(stringArray.getString(i));
        }
        return values;
    }

    public static void forEachEntry(JSONArray entries, String entryName, String sourceName, EntryParser parser) {
        if (entries == null) {
            return;
        }
        for (int i = 0; i < entries.length(); i++) {
            try {
                parser.parse(entries.getJSONObject(i));
            } catch (JSONException e) {
                Logger.getLogger(TreeJson.class).error("Skipping " + entryName + " " + entryLabel(entries, i) + " in " + sourceName + ": " + e.getMessage());
            }
        }
    }
}
