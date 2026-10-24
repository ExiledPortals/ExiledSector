package exiledsector.charts;

import exiledlib.ProjectFiles;
import exiledsector.charts.expansion.ChartExpansion;
import exiledsector.charts.expansion.ExpansionSite;
import exiledsector.charts.expansion.SpatialBreach;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartExpansionsTest {

    static final class PlacesNothing implements ChartExpansion {

        final List<ExpansionSite> sites = new ArrayList<>();

        @Override
        public String id() {
            return SpatialBreach.EXPANSION_ID;
        }

        @Override
        public void generate(ExpansionSite site) {
            sites.add(site);
        }
    }

    @AfterEach
    void tearDown() {
        ChartExpansions.resetForTests();
    }

    private static ChartGenContext withBreachChance(float percent) {
        return ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_CHANCE, percent)));
    }

    private static void useTestExpansion(int minTier, int maxTier, String requiredMod) {
        ChartExpansions.useForTests(List.of(new PlacesNothing()),
                List.of(new ChartExpansions.Definition(SpatialBreach.EXPANSION_ID, 1f, minTier, maxTier, requiredMod, "")));
    }

    @Test
    void theShippedExpansionTableParsesWithoutProblemsAndKnowsTheBreach() throws Exception {
        String text = Files.readString(ProjectFiles.projectRoot().resolve(ChartExpansions.PATH), StandardCharsets.UTF_8);

        List<String> problems = ChartExpansions.register(CDL.toJSONArray(text.replace("\r\n", "\n")));

        assertEquals(List.of(), problems);
        ChartExpansions.Definition breach = ChartExpansions.definition(SpatialBreach.EXPANSION_ID);
        assertNotNull(breach);
        assertTrue(breach.allowedAt(ChartTiers.LOWEST_TIER) && breach.allowedAt(ChartTiers.HIGHEST_TIER));
    }

    @Test
    void rowsWithoutAnExpansionClassAreSkipped() throws Exception {
        JSONArray rows = new JSONArray().put(new JSONObject().put("id", "not_built_yet").put("weight", "1"));

        List<String> problems = ChartExpansions.register(rows);

        assertEquals(1, problems.size());
        assertNull(ChartExpansions.definition("not_built_yet"));
    }

    @Test
    void aCertainChanceAlwaysPlansTheExpansionAndNoChanceNever() {
        useTestExpansion(1, 15, "");

        for (long seed = 0; seed < 20; seed++) {
            assertEquals(List.of(SpatialBreach.EXPANSION_ID), ChartExpansions.planned(seed, 5, withBreachChance(100f)));
            assertEquals(List.of(), ChartExpansions.planned(seed, 5, ChartGenContext.none()));
        }
    }

    @Test
    void aPartialChanceRollsPerSeedAndRepeatsForTheSameSeed() {
        useTestExpansion(1, 15, "");
        int planned = 0;

        for (long seed = 0; seed < 400; seed++) {
            List<String> first = ChartExpansions.planned(seed, 5, withBreachChance(50f));
            assertEquals(first, ChartExpansions.planned(seed, 5, withBreachChance(50f)));
            planned += first.size();
        }

        assertTrue(planned > 140 && planned < 260, "about half the seeds plan the breach, got " + planned);
    }

    @Test
    void anExpansionOutsideItsTiersOrWithoutItsModIsNeverPlanned() {
        useTestExpansion(6, 10, "");
        assertEquals(List.of(), ChartExpansions.planned(1L, 5, withBreachChance(100f)));
        assertEquals(List.of(SpatialBreach.EXPANSION_ID), ChartExpansions.planned(1L, 6, withBreachChance(100f)));
        assertEquals(List.of(), ChartExpansions.planned(1L, 11, withBreachChance(100f)));

        useTestExpansion(1, 15, "some_missing_mod");
        assertEquals(List.of(), ChartExpansions.planned(1L, 5, withBreachChance(100f)));
    }

    @Test
    void anExpansionWithNoTableRowIsNeverPlanned() {
        assertEquals(List.of(), ChartExpansions.planned(1L, 5, withBreachChance(100f)));
    }

    @Test
    void breachTuningLinesReachTheContext() {
        ChartGenContext context = ChartGenContext.of(List.of(
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_GROWTH_PERCENT, 40f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_CACHE_PERCENT, 50f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_CORE_ITEM_PERCENT, 25f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_CHANCE, 100f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_BREACH_CHANCE, 100f)));

        assertEquals(1.4f, context.expansionMult(SpatialBreach.GROWTH_KEY), 0.001f);
        assertEquals(1.5f, context.expansionMult(SpatialBreach.CACHE_KEY), 0.001f);
        assertEquals(1.25f, context.expansionMult(SpatialBreach.CORE_ITEM_KEY), 0.001f);
        assertEquals(200f, context.expansionChance(SpatialBreach.EXPANSION_ID), 0.001f);
        assertEquals(1f, ChartGenContext.none().expansionMult(SpatialBreach.GROWTH_KEY));
    }
}
