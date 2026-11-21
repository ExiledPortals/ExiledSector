package exiledsector;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public final class ModJson {

    @FunctionalInterface
    public interface JsonHandler {
        void accept(JSONObject json) throws JSONException;
    }

    private ModJson() {
    }

    public static JSONObject json(String path, boolean merged) throws IOException, JSONException {
        return merged ? Global.getSettings().getMergedJSONForMod(path, ExiledSector.MOD_ID) : Global.getSettings().loadJSON(path);
    }

    public static JSONObject jsonOrNull(String path, boolean merged, Logger log) {
        try {
            return json(path, merged);
        } catch (IOException | JSONException | RuntimeException e) {
            log.error("Failed to load " + path, e);
            return null;
        }
    }

    public static void load(String path, boolean merged, Logger log, JsonHandler handler) {
        try {
            handler.accept(json(path, merged));
        } catch (IOException | JSONException | RuntimeException e) {
            log.error("Failed to load " + path, e);
        }
    }
}
