package exiledsector.charts.loot;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.subroutine.ModspecCaches;
import exiledsector.items.subroutine.SubroutineKind;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.Loot;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

class ChartFleetSubroutinesTest {

    private static final String CACHE = "test_modspec_cache";
    private static final int TRIALS = 20000;
    private static final float LOOT_DEPLOYMENT_POINTS = 100f;

    private MockedStatic<LunaSettings> lunaSettings;

    @BeforeEach
    void setUp() throws Exception {
        lunaSettings = Mockito.mockStatic(LunaSettings.class);
        lunaSettings.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        JSONArray rows = new JSONArray().put(new JSONObject().put("id", CACHE).put("name", "Test Cache").put("icon", "graphics/icons/cargo/chip1.png")
                .put("rarity", "20").put("kind", SubroutineKind.MODSPEC_CACHE.id()).put("node_count_chances", "1").put("salvage_chance", "0.5"));
        ItemDefinitions.register(rows);
        ModspecCaches.register(rows);
    }

    @AfterEach
    void tearDown() {
        BattleLootRates.clear();
        ModspecCaches.clear();
        ItemDefinitions.clear();
        lunaSettings.close();
    }

    private static void registerCacheRate(String perHundredDeploymentPoints) throws Exception {
        BattleLootRates.register(new JSONArray().put(new JSONObject().put("drop", ChartFleetLoot.MODSPEC_CACHE_RATE)
                .put("per_100_dp", perHundredDeploymentPoints).put("curve", "linear").put("difficulty_scaling", "0")));
    }

    private static ChartLootContext withLine(ChartModifier modifier, float value) {
        return new ChartLootContext(ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(modifier, value))), 5);
    }

    private static float cachesPerFleet(ChartLootContext loot) {
        Random random = new Random(5L);
        int caches = 0;
        for (int trial = 0; trial < TRIALS; trial++) {
            List<ItemData> items = ChartFleetLoot.modspecCaches(loot, LOOT_DEPLOYMENT_POINTS, ChartFleetLoot.Battle.of(LOOT_DEPLOYMENT_POINTS), random).items();
            assertTrue(items.stream().allMatch(item -> CACHE.equals(item.definitionId())));
            caches += items.size();
        }
        return caches / (float) TRIALS;
    }

    @Test
    void chartFleetsDropCachesAtTheRateTimesEachCachesSalvageChanceAndTheSubroutineQuantityButNotTheHullItemLine() throws Exception {
        registerCacheRate("0.4");

        assertEquals(0.4f * 0.5f, cachesPerFleet(ChartLootContext.atTier(5)), 0.01f);
        assertEquals(0.4f * 0.5f, cachesPerFleet(withLine(ChartModifier.CHART_ITEM_PERCENT, 50f)), 0.01f);
        assertEquals(0.4f * 0.5f * 1.5f, cachesPerFleet(withLine(ChartModifier.CHART_SUBROUTINE_PERCENT, 50f)), 0.01f);
        assertEquals(0.4f * 0.5f * 1.5f, cachesPerFleet(withLine(ChartModifier.CHART_QUANTITY_PERCENT, 50f)), 0.01f);
    }

    @Test
    void withoutTheCacheRateChartFleetsDropNoCaches() {
        assertSame(Loot.NONE, ChartFleetLoot.modspecCaches(ChartLootContext.atTier(5), LOOT_DEPLOYMENT_POINTS,
                ChartFleetLoot.Battle.of(LOOT_DEPLOYMENT_POINTS), new Random(1L)));
    }

    @Test
    void doctrineArchivesReadItemQuantityAndTheSubroutineQuantityButNotTheHullItemLine() {
        ChartLootContext bossLoot = new ChartLootContext(ChartGenContext.of(List.of(
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_QUANTITY_PERCENT, 10f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_ITEM_PERCENT, 20f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_SUBROUTINE_PERCENT, 30f))), 5);

        assertEquals(1.4f, ChartFleetLoot.doctrineArchiveQuantity(bossLoot, 1f).mult(), 1e-5f);
        assertEquals(0.7f, ChartFleetLoot.doctrineArchiveQuantity(bossLoot, 0.5f).mult(), 1e-5f);
    }
}
