package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.charts.ChartGenContext;
import exiledsector.charts.ChartIds;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChartSitesTest {

    private FakeChartContentEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        engine = FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing());
        ChartPois.register(new JSONArray()
                .put(new JSONObject().put("id", "supply").put("kind", "cache").put("entity", "supply_cache").put("cost", "1").put("weight", "1")
                        .put("min_tier", "1").put("max_tier", "15"))
                .put(new JSONObject().put("id", "derelict").put("kind", "derelict").put("entity", "wreck").put("derelict_type", "SMALL")
                        .put("cost", "1").put("weight", "1")));
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        ChartPois.resetForTests();
    }

    @Test
    void aCacheIsPlacedAtTheSpotWithItsSalvageSpecAndRunTags() {
        StarSystemAPI system = mock(StarSystemAPI.class);

        SectorEntityToken cache = ChartSites.addCache(system, "run1", 4, ChartGenContext.none(), new Vector2f(120f, -80f), new Random(3L));

        assertNotNull(cache);
        assertEquals("supply_cache", engine.salvageTypes.get(0));
        verify(cache).setFixedLocation(120f, -80f);
        assertEquals("supply_cache", cache.getMemoryWithoutUpdate().getString(MemFlags.SALVAGE_SPEC_ID_OVERRIDE));
        assertTrue(cache.hasTag(ChartIds.runTag("run1")));
        assertTrue(cache.hasTag(ChartIds.CONTENT_TAG));
    }

    @Test
    void withNoCacheForTheTierNothingIsPlaced() {
        ChartPois.resetForTests();

        assertNull(ChartSites.addCache(mock(StarSystemAPI.class), "run1", 4, ChartGenContext.none(), new Vector2f(), new Random(3L)));
        assertTrue(engine.salvageEntities.isEmpty());
    }
}
