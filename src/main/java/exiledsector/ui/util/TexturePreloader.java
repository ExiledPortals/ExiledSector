package exiledsector.ui.util;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONArray;

public final class TexturePreloader {

    public static final String LIST_PATH = "data/config/exiledSector/texture_preload.csv";
    private static final Logger LOG = Logger.getLogger(TexturePreloader.class);

    @FunctionalInterface
    interface TextureLoader {
        void load(String path) throws Exception;
    }

    private TexturePreloader() {
    }

    public static void preloadAll(String modId) {
        JSONArray rows;
        try {
            rows = Global.getSettings().loadCSV(LIST_PATH, modId);
        } catch (Exception e) {
            LOG.warn("No texture preload list at " + LIST_PATH + "; textures will load the first time they are drawn", e);
            return;
        }
        if (rows == null) {
            return;
        }
        long start = System.nanoTime();
        int loaded = preload(rows, path -> Global.getSettings().loadTexture(path));
        LOG.info("Preloaded " + loaded + " of " + rows.length() + " textures in " + (System.nanoTime() - start) / 1_000_000L + " ms");
    }

    static int preload(JSONArray rows, TextureLoader loader) {
        int loaded = 0;
        for (int i = 0; i < rows.length(); i++) {
            String path = rows.optJSONObject(i) == null ? "" : rows.optJSONObject(i).optString("path", "").trim();
            if (path.isEmpty()) {
                continue;
            }
            try {
                loader.load(path);
                loaded++;
            } catch (Exception e) {
                LOG.warn("Failed to preload texture " + path, e);
            }
        }
        return loaded;
    }
}
