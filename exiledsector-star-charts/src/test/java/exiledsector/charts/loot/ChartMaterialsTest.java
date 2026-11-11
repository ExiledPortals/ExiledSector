package exiledsector.charts.loot;

import exiledsector.items.BiomeNanoswarm;
import exiledsector.items.ItemModifiers;
import exiledsector.items.ModifierSources;
import exiledsector.items.StarChartFixtures;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChartMaterialsTest {

    private static final String ADAPTIVE = "exiledSector_adaptive_nanoswarm";
    private static final String GRAFTING = "exiledSector_grafting_nanoswarm";
    private static final String RIFT = "abyssal_rift";

    @BeforeEach
    void setUp() throws Exception {
        BiomeNanoswarm.registerAll();
        ItemModifiers.register(new JSONArray().put(new JSONObject().put("id", "TIMEFLOW_PERCENT_ABYSSAL_RIFT").put("affix", "prefix")
                .put("scopes", "hull_item").put("lines", "TIMEFLOW_PERCENT:3:5").put("name", "Chronal")));
        BiomeModifiers.register(new JSONArray().put(new JSONObject().put("modifier", "TIMEFLOW_PERCENT_ABYSSAL_RIFT").put("biome", RIFT)
                .put("item", "bridge")));
        ChartMaterials.register(new JSONArray()
                .put(row(ADAPTIVE))
                .put(row(GRAFTING))
                .put(row("exiledSector_augmentation_nanoswarm")));
    }

    @AfterEach
    void tearDown() {
        ChartMaterials.clear();
        BiomeModifiers.unregisterAll();
        ModifierSources.clear();
        ItemModifiers.clear();
        StarChartFixtures.unregisterCraftingItems();
    }

    private static JSONObject row(String drop) throws Exception {
        return new JSONObject().put("drop", drop).put("tier_1", "4").put("tier_15", "4").put("difficulty_scaling", "0");
    }

    @Test
    void biomeNanoswarmsDropOnlyInARegionWithBiomeModifiers() {
        assertEquals(Map.of(ADAPTIVE, 4, GRAFTING, 4, "exiledSector_augmentation_nanoswarm", 4),
                ChartMaterials.roll(5, 1f, 1f, RIFT, new Random(1L)));
        assertEquals(Map.of("exiledSector_augmentation_nanoswarm", 4), ChartMaterials.roll(5, 1f, 1f, "normal", new Random(1L)));
        assertEquals(Map.of("exiledSector_augmentation_nanoswarm", 4), ChartMaterials.roll(5, 1f, 1f, null, new Random(1L)));
    }
}
