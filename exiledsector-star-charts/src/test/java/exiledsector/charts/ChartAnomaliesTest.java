package exiledsector.charts;

import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AsteroidStorm;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.anomaly.MagneticStorm;
import exiledsector.charts.anomaly.SpatialBreach;
import exiledsector.charts.anomaly.StarAnomalies;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartAnomaliesTest {

    static final class PlacesNothing implements ChartAnomaly {

        final List<AnomalyContentSite> sites = new ArrayList<>();

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

    private static ChartGenContext withBreachChance(float percent) {
        return ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_CHANCE, percent)));
    }

    private static ChartGenContext withChances(String... anomalyIds) {
        ChartGenContext context = ChartGenContext.of(List.of(), 1);
        for (String anomalyId : anomalyIds) {
            context.addAnomalyChance(anomalyId, ChartGenContext.PERCENT);
        }
        return context;
    }

    private static ChartAnomalies.Definition definition(String id, int minTier, int maxTier, String requiredMod, Set<String> excludes) {
        ChartAnomaly anomaly = ChartAnomalies.byId(id);
        return new ChartAnomalies.Definition(id, 1f, minTier, maxTier, requiredMod, "", true, excludes, anomaly != null && anomaly.replacesStar());
    }

    static ChartAnomalies.Definition definition(String id) {
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
    void aCertainChanceAlwaysPlansTheAnomalyAndNoChanceNever() {
        useTestAnomaly(1, 15, "");

        for (long seed = 0; seed < 20; seed++) {
            assertEquals(List.of(SpatialBreach.ID), ChartAnomalies.planned(seed, 5, withBreachChance(100f)));
            assertEquals(List.of(), ChartAnomalies.planned(seed, 5, ChartGenContext.none()));
        }
    }

    @Test
    void aPartialChanceRollsPerSeedAndRepeatsForTheSameSeed() {
        useTestAnomaly(1, 15, "");
        int planned = 0;

        for (long seed = 0; seed < 400; seed++) {
            List<String> first = ChartAnomalies.planned(seed, 5, withBreachChance(50f));
            assertEquals(first, ChartAnomalies.planned(seed, 5, withBreachChance(50f)));
            planned += first.size();
        }

        assertTrue(planned > 140 && planned < 260, "about half the seeds plan the breach, got " + planned);
    }

    @Test
    void aChanceLineOutsideItsTiersOrWithoutItsModIsNeverPlanned() {
        useTestAnomaly(6, 10, "");
        assertEquals(List.of(), ChartAnomalies.planned(1L, 5, withBreachChance(100f)));
        assertEquals(List.of(SpatialBreach.ID), ChartAnomalies.planned(1L, 6, withBreachChance(100f)));
        assertEquals(List.of(), ChartAnomalies.planned(1L, 11, withBreachChance(100f)));

        useTestAnomaly(1, 15, "some_missing_mod");
        assertEquals(List.of(), ChartAnomalies.planned(1L, 5, withBreachChance(100f)));
    }

    @Test
    void anAnomalyWithNoTableRowIsNeverPlanned() {
        assertEquals(List.of(), ChartAnomalies.planned(1L, 5, withBreachChance(100f)));
        assertEquals(List.of(), ChartAnomalies.planned(1L, 5, ChartGenContext.none().withAnomalies(List.of(SpatialBreach.ID))));
    }

    @Test
    void chanceLinesAddSeveralAnomaliesButNeverAStarOne() {
        ChartAnomalies.useForTests(List.of(definition("black_hole"), definition(AsteroidStorm.ID), definition(MagneticStorm.ID)));

        for (long seed = 0; seed < 10; seed++) {
            List<String> planned = ChartAnomalies.planned(seed, 5, withChances("black_hole", AsteroidStorm.ID, MagneticStorm.ID));
            assertEquals(Set.of(AsteroidStorm.ID, MagneticStorm.ID), Set.copyOf(planned));
        }
    }

    @Test
    void forcedAnomaliesComeFirstIgnoreTheTierRangeAndBeatConflictingChanceLines() {
        ChartAnomalies.useForTests(List.of(definition("black_hole", 10, 15, "", Set.of()), definition("dense_nebula"),
                definition(MagneticStorm.ID, 1, 15, "", Set.of(AsteroidStorm.ID)), definition(AsteroidStorm.ID)));
        ChartGenContext context = withChances(AsteroidStorm.ID).withAnomalies(List.of("black_hole", "dense_nebula", MagneticStorm.ID));

        assertEquals(List.of("black_hole", MagneticStorm.ID), ChartAnomalies.planned(3L, 2, context));
        assertSame(StarAnomalies.BLACK_HOLE, ChartAnomalies.starOf(ChartAnomalies.planned(3L, 2, context)));
    }

    @Test
    void theAtlasNodesAnomaliesArriveAsForcedAnomalies() {
        ChartAnomalies.useForTests(List.of(definition(SpatialBreach.ID), definition(AsteroidStorm.ID)));
        AtlasRunNode node = new AtlasRunNode(7L, "pirates", List.of(), List.of(AsteroidStorm.ID, SpatialBreach.ID), false);

        ChartGenContext context = ChartGenContext.none().withAtlasNode(node);

        assertEquals(List.of(AsteroidStorm.ID, SpatialBreach.ID), context.forcedAnomalies());
        assertEquals(List.of(AsteroidStorm.ID, SpatialBreach.ID), ChartAnomalies.planned(1L, 5, context));
    }

    @Test
    void breachTuningLinesReachTheContext() {
        ChartGenContext context = ChartGenContext.of(List.of(
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_GROWTH_PERCENT, 40f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_CACHE_PERCENT, 50f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_CORE_ITEM_PERCENT, 25f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_CHANCE, 100f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_CHANCE, 100f)));

        assertEquals(1.4f, context.anomalyMult(SpatialBreach.GROWTH_KEY), 0.001f);
        assertEquals(1.5f, context.anomalyMult(SpatialBreach.CACHE_KEY), 0.001f);
        assertEquals(1.25f, context.anomalyMult(SpatialBreach.CORE_ITEM_KEY), 0.001f);
        assertEquals(200f, context.anomalyChance(SpatialBreach.ID), 0.001f);
        assertEquals(1f, ChartGenContext.none().anomalyMult(SpatialBreach.GROWTH_KEY));
    }
}
