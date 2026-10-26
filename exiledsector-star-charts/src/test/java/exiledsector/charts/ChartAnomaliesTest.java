package exiledsector.charts;

import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AsteroidStorm;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.anomaly.MagneticStorm;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.charts.anomaly.StarAnomalies;
import exiledsector.charts.anomaly.StellarFlares;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.loot.DropType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChartAnomaliesTest {

    public static final class PlacesNothing implements ChartAnomaly {

        public final List<AnomalyContentSite> sites = new ArrayList<>();

        @Override
        public String id() {
            return SpatialBreach.ID;
        }

        @Override
        public void generate(AnomalyContentSite site) {
            sites.add(site);
        }
    }

    @AfterEach
    void tearDown() {
        ChartAnomalies.resetForTests();
    }

    private static ChartAnomalies.Definition definition(String id, int minTier, int maxTier, String requiredMod, Set<String> excludes) {
        ChartAnomaly anomaly = ChartAnomalies.byId(id);
        return new ChartAnomalies.Definition(id, 1f, minTier, maxTier, requiredMod, "", true, excludes, anomaly != null && anomaly.replacesStar());
    }

    public static ChartAnomalies.Definition definition(String id) {
        return definition(id, 1, 15, "", Set.of());
    }

    private static void useTestAnomaly(int minTier, int maxTier, String requiredMod) {
        ChartAnomalies.useForTests(List.of(new PlacesNothing()), List.of(definition(SpatialBreach.ID, minTier, maxTier, requiredMod, Set.of())));
    }

    @Test
    void theShippedTableParsesWithoutProblemsAndListsEveryAnomaly() throws Exception {
        assertEquals(List.of(), ChartAnomalies.register(ChartTables.csvRows(ChartAnomalies.PATH)));

        for (ChartAnomaly anomaly : ChartAnomalies.BUILT_IN) {
            ChartAnomalies.Definition definition = ChartAnomalies.definition(anomaly.id());
            assertNotNull(definition, anomaly.id());
            assertEquals(anomaly.replacesStar(), definition.replacesStar(), anomaly.id());
        }
        ChartAnomalies.Definition breach = ChartAnomalies.definition(SpatialBreach.ID);
        assertTrue(breach.insertable());
        assertTrue(breach.allowedAt(ChartTiers.LOWEST_TIER) && breach.allowedAt(ChartTiers.HIGHEST_TIER));
        assertFalse(ChartAnomalies.definition("black_hole").insertable());
        Set<DropType> lootKinds = new HashSet<>();
        for (String id : List.of("dense_nebula", MagneticStorm.ID, StellarFlares.ID)) {
            List<ChartGenContext.AppliedEffect> effects = ChartAnomalies.definition(id).effects();
            assertEquals(1, effects.size(), id);
            assertNotNull(effects.get(0).effect().dropType(), id);
            lootKinds.add(effects.get(0).effect().dropType());
        }
        assertEquals(3, lootKinds.size());
    }

    @Test
    void anAnomalysEffectsSkipUnknownLines() throws Exception {
        List<String> problems = ChartAnomalies.register(new JSONArray().put(new JSONObject().put("id", MagneticStorm.ID)
                .put("effects", "CHART_CURRENCY_PERCENT:20; CHART_BREACH_CHANCE:100; NOT_AN_EFFECT:5")));

        assertEquals(2, problems.size());
        assertEquals(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_CURRENCY_PERCENT, 20f)),
                ChartAnomalies.definition(MagneticStorm.ID).effects());
    }

    @Test
    void theRunsAnomaliesAddTheirEffectsToTheContextOnTopOfTheChart() {
        ChartAnomalies.useForTests(List.of(
                new ChartAnomalies.Definition(MagneticStorm.ID, 1f, 1, 15, "", "", false, Set.of(), false,
                        List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_CURRENCY_PERCENT, 50f))),
                new ChartAnomalies.Definition(StellarFlares.ID, 1f, 1, 15, "", "", false, Set.of(), false,
                        List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_FEEDSTOCK_PERCENT, 30f))),
                definition(AsteroidStorm.ID)));
        ChartGenContext chart = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_CURRENCY_PERCENT, 20f)))
                .withAnomalies(List.of(AsteroidStorm.ID));

        ChartGenContext context = chart.withAnomalyEffects(List.of(MagneticStorm.ID, StellarFlares.ID, AsteroidStorm.ID));

        assertEquals(70f, context.dropPercent(DropType.CURRENCY), 0.001f);
        assertEquals(30f, context.dropPercent(DropType.FEEDSTOCK), 0.001f);
        assertEquals(List.of(AsteroidStorm.ID), context.forcedAnomalies());
        assertSame(chart, chart.withAnomalyEffects(List.of(AsteroidStorm.ID)));
        assertSame(chart, chart.withAnomalyEffects(null));
    }

    @Test
    void unknownRowsAreSkippedAndBadInsertableOrExcludesAreReported() throws Exception {
        List<String> problems = ChartAnomalies.register(new JSONArray()
                .put(new JSONObject().put("id", "ion_storm"))
                .put(new JSONObject().put("id", "black_hole").put("insertable", "yes"))
                .put(new JSONObject().put("id", "spatial_breach").put("insertable", "yes").put("excludes", "magnetic_storm; ion_storm; spatial_breach")));

        assertEquals(4, problems.size());
        assertNull(ChartAnomalies.definition("ion_storm"));
        assertFalse(ChartAnomalies.definition("black_hole").insertable());
        assertEquals(Set.of("magnetic_storm"), ChartAnomalies.definition("spatial_breach").excludes());
    }

    @Test
    void starAnomaliesConflictWithEachOtherAndExcludesWorkBothWays() {
        ChartAnomalies.useForTests(List.of(definition("black_hole"), definition("dense_nebula"),
                definition(SpatialBreach.ID, 1, 15, "", Set.of(MagneticStorm.ID)), definition(MagneticStorm.ID), definition(AsteroidStorm.ID)));

        assertTrue(ChartAnomalies.conflicts("black_hole", "dense_nebula"));
        assertTrue(ChartAnomalies.conflicts(SpatialBreach.ID, MagneticStorm.ID));
        assertTrue(ChartAnomalies.conflicts(MagneticStorm.ID, SpatialBreach.ID));
        assertFalse(ChartAnomalies.conflicts(SpatialBreach.ID, AsteroidStorm.ID));
        assertFalse(ChartAnomalies.conflicts("black_hole", AsteroidStorm.ID));
        assertFalse(ChartAnomalies.accepts(List.of("black_hole"), "dense_nebula"));
        assertFalse(ChartAnomalies.accepts(List.of("black_hole"), "black_hole"));
        assertTrue(ChartAnomalies.accepts(List.of("black_hole"), AsteroidStorm.ID));
    }

    @Test
    void withoutAtlasAnomaliesNothingIsPlannedWhateverTheChartCarries() {
        useTestAnomaly(1, 15, "");
        ChartGenContext chart = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_GROWTH_PERCENT, 50f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_CURRENCY_PERCENT, 20f)));

        assertEquals(List.of(), ChartAnomalies.planned(chart));
        assertEquals(List.of(), ChartAnomalies.planned(ChartGenContext.none()));
        assertEquals(List.of(), ChartAnomalies.planned(null));
        assertEquals(List.of(SpatialBreach.ID), ChartAnomalies.planned(chart.withAnomalies(List.of(SpatialBreach.ID))));
    }

    @Test
    void anAnomalyWithNoTableRowOrWithoutItsModIsNeverPlanned() {
        assertEquals(List.of(), ChartAnomalies.planned(ChartGenContext.none().withAnomalies(List.of(SpatialBreach.ID))));

        useTestAnomaly(1, 15, "some_missing_mod");
        assertEquals(List.of(), ChartAnomalies.planned(ChartGenContext.none().withAnomalies(List.of(SpatialBreach.ID))));
    }

    @Test
    void forcedAnomaliesKeepTheirOrderIgnoreTheTierRangeAndDropConflicts() {
        ChartAnomalies.useForTests(List.of(definition("black_hole", 10, 15, "", Set.of()), definition("dense_nebula"),
                definition(MagneticStorm.ID, 1, 15, "", Set.of(AsteroidStorm.ID)), definition(AsteroidStorm.ID)));
        ChartGenContext context = ChartGenContext.none().withAnomalies(List.of("black_hole", "dense_nebula", MagneticStorm.ID, AsteroidStorm.ID));

        assertEquals(List.of("black_hole", MagneticStorm.ID), ChartAnomalies.planned(context));
        assertSame(StarAnomalies.BLACK_HOLE, ChartAnomalies.starOf(ChartAnomalies.planned(context)));
    }

    @Test
    void theAtlasNodesAnomaliesArriveAsForcedAnomalies() {
        ChartAnomalies.useForTests(List.of(definition(SpatialBreach.ID), definition(AsteroidStorm.ID)));
        AtlasRunNode node = new AtlasRunNode(7L, "pirates", List.of(), List.of(AsteroidStorm.ID, SpatialBreach.ID), false);

        ChartGenContext context = ChartGenContext.none().withAtlasNode(node);

        assertEquals(List.of(AsteroidStorm.ID, SpatialBreach.ID), context.forcedAnomalies());
        assertEquals(List.of(AsteroidStorm.ID, SpatialBreach.ID), ChartAnomalies.planned(context));
    }

    @Test
    void breachTuningLinesReachTheContext() {
        ChartGenContext context = ChartGenContext.of(List.of(
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_GROWTH_PERCENT, 40f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_CACHE_PERCENT, 50f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_CACHE_ITEM_PERCENT, 25f)));

        assertEquals(1.4f, context.anomalyMult(SpatialBreach.GROWTH_KEY), 0.001f);
        assertEquals(1.5f, context.anomalyMult(SpatialBreach.CACHE_KEY), 0.001f);
        assertEquals(1.25f, context.anomalyMult(SpatialBreach.CACHE_ITEM_KEY), 0.001f);
        assertEquals(1f, ChartGenContext.none().anomalyMult(SpatialBreach.GROWTH_KEY));
    }
}
