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
        JSONArray textureRows;
        try {
            textureRows = Global.getSettings().loadCSV(LIST_PATH, modId);
        } catch (Exception e) {
            LOG.warn("No texture preload list at " + LIST_PATH + "; textures will load the first time they are drawn", e);
            return;
        }
        if (textureRows == null) {
            return;
        }
        long startNanos = System.nanoTime();
        int loadedCount = preload(textureRows, path -> Global.getSettings().loadTexture(path));
        LOG.info("Preloaded " + loadedCount + " of " + textureRows.length() + " textures in " + (System.nanoTime() - startNanos) / 1_000_000L
                + " ms");
    }

    static int preload(JSONArray textureRows, TextureLoader loader) {
        int loadedCount = 0;
        for (int i = 0; i < textureRows.length(); i++) {
            String path = textureRows.optJSONObject(i) == null ? "" : textureRows.optJSONObject(i).optString("path", "").trim();
            if (path.isEmpty()) {
                continue;
            }
            try {
                loader.load(path);
                loadedCount++;
            } catch (Exception e) {
                LOG.warn("Failed to preload texture " + path, e);
            }
        }
        return loadedCount;
    }
}
