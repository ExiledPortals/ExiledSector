package exiledsector;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static exiledsector.ExiledSectorModPlugin.MOD_ID;

public final class ModCsv {

    @FunctionalInterface
    public interface RowsHandler {
        void accept(JSONArray rows) throws JSONException;
    }

    @FunctionalInterface
    public interface RowHandler {
        void accept(int index, JSONObject row) throws JSONException;
    }

    private ModCsv() {
    }

    public static JSONArray rows(String idColumn, String path) throws IOException, JSONException {
        return Global.getSettings().getMergedSpreadsheetDataForMod(idColumn, path, MOD_ID);
    }

    public static void load(String idColumn, String path, Logger log, RowsHandler handler) {
        try {
            handler.accept(rows(idColumn, path));
        } catch (IOException | JSONException e) {
            log.error("Failed to load " + path, e);
        }
    }

    public static void forEach(JSONArray rows, RowHandler handler) throws JSONException {
        for (int i = 0; i < rows.length(); i++) {
            handler.accept(i, rows.getJSONObject(i));
        }
    }

    public static String text(JSONObject row, String column) {
        return row.optString(column, "").trim();
    }
}
