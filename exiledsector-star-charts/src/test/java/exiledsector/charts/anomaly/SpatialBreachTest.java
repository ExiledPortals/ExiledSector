package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import exiledsector.charts.ChartEffect;
import exiledsector.charts.ChartGenContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpatialBreachTest {

    private StarSystemAPI system;
    private SectorEntityToken terrain;
    private CustomCampaignEntityAPI core;

    @BeforeEach
    void setUp() {
        system = mock(StarSystemAPI.class);
        terrain = mock(SectorEntityToken.class);
        core = mock(CustomCampaignEntityAPI.class);
        when(system.addTerrain(eq(SpatialBreach.TERRAIN_ID), any())).thenReturn(terrain);
        when(system.addCustomEntity(anyString(), any(), eq(SpatialBreach.CORE_TYPE), anyString())).thenReturn(core);
    }

    private AnomalyContentSite site(ChartGenContext context, Predicate<Vector2f> clear) {
        return site(context, null, clear);
    }

    private AnomalyContentSite site(ChartGenContext context, Vector2f entry, Predicate<Vector2f> clear) {
        return new AnomalyContentSite(system, "run1", 5, "pirates", context, List.of(), entry, spot -> clear.test(spot.position()), Random::new);
    }

    @Test
    void theBreachOpensAtAClearSpotWithAnUnclickableCoreAtTheCentre() {
        Predicate<Vector2f> clear = point -> point.length() > 3000f;

        SpatialBreach.INSTANCE.generate(site(ChartGenContext.none(), clear));

        ArgumentCaptor<Float> x = ArgumentCaptor.forClass(Float.class);
        ArgumentCaptor<Float> y = ArgumentCaptor.forClass(Float.class);
        verify(terrain).setFixedLocation(x.capture(), y.capture());
        assertTrue(new Vector2f(x.getValue(), y.getValue()).length() > 3000f);
        verify(core).setFixedLocation(x.getValue(), y.getValue());
        verify(core).addTag(SpatialBreach.CORE_TAG);
        verify(core).addTag(Tags.NON_CLICKABLE);
        verify(system).addCustomEntity(eq(SpatialBreach.coreId("run1")), any(), eq(SpatialBreach.CORE_TYPE), anyString());
    }

    @Test
    void theBreachTuningLinesShapeItsGrowthAndCaches() {
        ChartGenContext context = ChartGenContext.of(List.of(
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_GROWTH_PERCENT, 100f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_CACHE_PERCENT, 100f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_CACHE_ITEM_PERCENT, 50f)));

        SpatialBreach.INSTANCE.generate(site(context, point -> true));

        ArgumentCaptor<Object> params = ArgumentCaptor.forClass(Object.class);
        verify(system).addTerrain(eq(SpatialBreach.TERRAIN_ID), params.capture());
        BreachTerrainPlugin.Params opened = (BreachTerrainPlugin.Params) params.getValue();
        assertEquals("run1", opened.runId());
        assertEquals(5, opened.tier());
        assertEquals(BreachRules.growth(5, 2f), opened.growth());
        assertEquals(BreachRules.cacheCount(5, 2f), opened.caches().size());
        assertEquals(1.5f, opened.cacheItemMult(), 0.001f);
    }

    @Test
    void theFullyGrownBreachNeverReachesTheEntryWormhole() {
        Vector2f entry = new Vector2f(2600f, 0f);
        float maxRadius = BreachRules.growth(5, 1f).maxRadius();

        for (long seed = 0; seed < 20; seed++) {
            Vector2f centre = SpatialBreach.pickCentre(site(ChartGenContext.none(), entry, point -> true), maxRadius, new Random(seed));
            if (centre != null) {
                assertTrue(Vector2f.sub(centre, entry, new Vector2f()).length() > maxRadius + SpatialBreach.ENTRY_MARGIN);
            }
        }
    }

    @Test
    void aCoreThatCannotBePlacedTakesTheFieldWithIt() {
        when(system.addCustomEntity(anyString(), any(), eq(SpatialBreach.CORE_TYPE), anyString())).thenThrow(new IllegalStateException("no core"));

        assertThrows(IllegalStateException.class, () -> SpatialBreach.INSTANCE.generate(site(ChartGenContext.none(), point -> true)));
        verify(system).removeEntity(terrain);
    }

    @Test
    void withNowhereClearTheBreachIsNotOpened() {
        assertThrows(IllegalStateException.class, () -> SpatialBreach.INSTANCE.generate(site(ChartGenContext.none(), point -> false)));
        verify(system, never()).addTerrain(anyString(), any());
    }
}
