package exiledsector.ui.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TexturePreloaderTest {

    @Test
    void loadsEveryListedTextureAndCarriesOnPastFailures() throws Exception {
        JSONArray rows = new JSONArray()
                .put(new JSONObject().put("path", "graphics/icons/a.png"))
                .put(new JSONObject().put("path", "  "))
                .put(new JSONObject().put("path", "graphics/icons/missing.png"))
                .put(new JSONObject().put("path", "graphics/icons/b.png"));
        List<String> attempted = new ArrayList<>();

        int loaded = TexturePreloader.preload(rows, path -> {
            attempted.add(path);
            if (path.contains("missing")) {
                throw new IOException("not found");
            }
        });

        assertEquals(2, loaded);
        assertEquals(List.of("graphics/icons/a.png", "graphics/icons/missing.png", "graphics/icons/b.png"), attempted);
    }
}
