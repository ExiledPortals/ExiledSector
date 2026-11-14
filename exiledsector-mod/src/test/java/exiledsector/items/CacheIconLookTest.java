package exiledsector.items;

import exiledsector.ProjectFiles;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheIconLookTest {

    private static final String CACHE = "cache_icon_test";

    @BeforeEach
    void setUp() throws Exception {
        JSONArray rows = new JSONArray().put(ItemFixtures.subroutineRow(CACHE, SubroutineKind.MODSPEC_CACHE)
                .put(ModspecCaches.NODE_COUNT_CHANCES_COLUMN, "0;0;1"));
        ItemDefinitions.register(rows);
        ModspecCaches.register(rows);
    }

    @AfterEach
    void tearDown() {
        ModspecCaches.clear();
        ItemDefinitions.clear();
    }

    @Test
    void aCacheShowsItsStillIconInTheCorruptedRarityLook() {
        CargoIconLook look = CargoIconLooks.forItem(new ItemStore().add(ItemDefinitions.get(CACHE), 11L));

        assertFalse(look.ownArt());
    }

    @Test
    void theShippedCacheIconExists() throws Exception {
        JSONArray subroutines = HullItemRows.shippedRows(SubroutineDefinitions.dataPath());
        int caches = 0;
        for (int row = 0; row < subroutines.length(); row++) {
            JSONObject subroutine = subroutines.getJSONObject(row);
            if (SubroutineKind.MODSPEC_CACHE.id().equals(subroutine.optString(SubroutineDefinitions.SUBROUTINE_KIND_COLUMN))) {
                assertTrue(Files.isRegularFile(ProjectFiles.projectRoot().resolve(subroutine.getString("icon"))), subroutine.getString("id"));
                caches++;
            }
        }
        assertEquals(1, caches);
    }
}
