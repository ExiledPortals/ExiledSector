package exiledsector.charts.atlas;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.AsteroidStorm;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.StarAnomalies;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.charts.anomaly.hypershunt.CoronalHypershunt;
import exiledsector.items.UseVerdict;
import exiledsector.items.crafting.material.CraftingMaterials;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveCatalystsTest {

    private static final long SEED = 0x51A7E5L;
    private static final List<AtlasRegion> REGIONS = List.of(new AtlasRegion(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of()));
    private static final AtlasCatalysts.Catalyst BREACH_TERMINUS = new AtlasCatalysts.Catalyst("test_breach_terminus", SpatialBreach.ANOMALY_ID,
            AtlasCatalysts.Family.TERMINUS, 1f, 1);
    private static final AtlasCatalysts.Catalyst STORM_TERMINUS = new AtlasCatalysts.Catalyst("test_storm_terminus", AsteroidStorm.ANOMALY_ID,
            AtlasCatalysts.Family.TERMINUS, 1f, 1);
    private static final AtlasCatalysts.Catalyst HYPERSHUNT_SURVEY = new AtlasCatalysts.Catalyst("test_hypershunt_survey", CoronalHypershunt.ANOMALY_ID,
            AtlasCatalysts.Family.SURVEY, 0f, 1, AtlasCatalysts.Effect.STAR, "");
    private static final AtlasCatalysts.Catalyst PLAIN_HYPERSHUNT = new AtlasCatalysts.Catalyst("test_plain", CoronalHypershunt.ANOMALY_ID,
            AtlasCatalysts.Family.SURVEY, 0f, 1);

    private static AtlasGenerator generator(AtlasState state) {
        AtlasRules rules = new AtlasRules(REGIONS, Map.of("compact", 1f), Map.of("pirates", 1f), new AtlasAnomalyChance(0.3f, 0f, 0.3f),
                AtlasAnomalyChance.NEVER, List.of(), List.of());
        return new AtlasGenerator(SEED, rules, AtlasUnlocks.NONE, state);
    }

    private static AtlasCell system(AtlasGenerator generator, int skip) {
        int found = 0;
        for (AtlasCell cell : AtlasCell.ORIGIN.around(6)) {
            if (!cell.equals(AtlasCell.ORIGIN) && !generator.isVoid(cell) && found++ == skip) {
                return cell;
            }
        }
        throw new AssertionError("no system");
    }

    private static AtlasCell farSystem(AtlasGenerator generator) {
        for (AtlasCell cell : new AtlasCell(20, 0).around(3)) {
            if (!generator.isVoid(cell) && generator.traits(cell).distance() >= CoronalHypershunt.MIN_DISTANCE) {
                return cell;
            }
        }
        throw new AssertionError("no far system");
    }

    private static ChartAnomalies.Definition definition(String id, Set<String> excludes) {
        return new ChartAnomalies.Definition(id, 1f, 1, 15, "", "", true, excludes, false);
    }

    private static CraftingMaterials holding(Map<String, Integer> counts) {
        return CraftingMaterials.storedForTests(new HashMap<>(counts));
    }

    private static UseVerdict onlyVerdict(List<ActiveCatalysts.Planned> planned) {
        assertEquals(1, planned.size());
        return planned.get(0).verdict();
    }

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadAnomalies();
        AtlasCatalysts.useForTests(List.of(BREACH_TERMINUS, STORM_TERMINUS, HYPERSHUNT_SURVEY, PLAIN_HYPERSHUNT));
    }

    @AfterEach
    void tearDown() {
        AtlasCatalysts.useForTests(List.of());
        ChartTables.resetAnomalies();
    }

    @Test
    void aCatalystTurnsOnOnlyWhileOneIsCarriedAndTurnsOffWhenClickedAgain() {
        AtlasState state = new AtlasState(SEED);

        assertEquals(ActiveCatalysts.Toggle.NONE_OWNED, ActiveCatalysts.toggle(state, BREACH_TERMINUS, 0));
        assertFalse(ActiveCatalysts.isOn(state, BREACH_TERMINUS.itemId()));
        assertEquals(ActiveCatalysts.Toggle.ON, ActiveCatalysts.toggle(state, BREACH_TERMINUS, 1));
        assertTrue(ActiveCatalysts.isOn(state, BREACH_TERMINUS.itemId()));
        assertEquals(ActiveCatalysts.Toggle.OFF, ActiveCatalysts.toggle(state, BREACH_TERMINUS, 0));
        assertFalse(ActiveCatalysts.isOn(state, BREACH_TERMINUS.itemId()));
    }

    @Test
    void catalystsNoLongerCarriedOrNoLongerLoadedTurnOff() {
        AtlasState state = new AtlasState(SEED);
        state.setCatalystOn(BREACH_TERMINUS.itemId(), true);
        state.setCatalystOn(STORM_TERMINUS.itemId(), true);
        state.setCatalystOn("test_removed_catalyst", true);

        ActiveCatalysts.turnOffMissing(state, holding(Map.of(STORM_TERMINUS.itemId(), 1, "test_removed_catalyst", 3))::count);

        assertEquals(List.of(STORM_TERMINUS.itemId()), state.activeCatalystIds());
    }

    @Test
    void openingAChartAddsEachFittingCatalystsAnomalyAndSpendsOneOfEach() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell cell = system(generator, 0);
        CraftingMaterials materials = holding(Map.of(BREACH_TERMINUS.itemId(), 2, STORM_TERMINUS.itemId(), 1));
        state.setCatalystOn(BREACH_TERMINUS.itemId(), true);
        state.setCatalystOn(STORM_TERMINUS.itemId(), true);
        List<String> before = AtlasAnomalies.effective(generator, state, cell);

        List<ActiveCatalysts.Planned> planned = ActiveCatalysts.plan(generator, state, cell, materials::count);
        ActiveCatalysts.spend(state, planned, materials);

        assertEquals(List.of(SpatialBreach.ANOMALY_ID, AsteroidStorm.ANOMALY_ID), ActiveCatalysts.anomalyIds(planned));
        assertEquals(1, materials.count(BREACH_TERMINUS.itemId()));
        assertEquals(0, materials.count(STORM_TERMINUS.itemId()));
        assertEquals(List.of(BREACH_TERMINUS.itemId()), state.activeCatalystIds());
        assertEquals(before, AtlasAnomalies.effective(generator, state, cell));
    }

    @Test
    void aCatalystThatDoesNotFitIsSkippedAndNotSpent() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell completed = system(generator, 0);
        state.add(completed, AtlasState.COMPLETED);
        CraftingMaterials materials = holding(Map.of(BREACH_TERMINUS.itemId(), 1));
        state.setCatalystOn(BREACH_TERMINUS.itemId(), true);

        List<ActiveCatalysts.Planned> planned = ActiveCatalysts.plan(generator, state, completed, materials::count);
        ActiveCatalysts.spend(state, planned, materials);

        assertEquals("catalyst.refused.completed", onlyVerdict(planned).refusalKey());
        assertEquals(List.of(), ActiveCatalysts.anomalyIds(planned));
        assertEquals(1, materials.count(BREACH_TERMINUS.itemId()));
        assertTrue(ActiveCatalysts.isOn(state, BREACH_TERMINUS.itemId()));
        assertEquals("catalyst.refused.no_system", onlyVerdict(ActiveCatalysts.plan(generator, state, AtlasCell.ORIGIN, materials::count)).refusalKey());
    }

    @Test
    void aConflictNamesTheAnomalyAlreadyThere() {
        ChartTables.useAnomalies(List.of(definition(SpatialBreach.ANOMALY_ID, Set.of(AsteroidStorm.ANOMALY_ID)), definition(AsteroidStorm.ANOMALY_ID, Set.of())));
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell cell = system(generator, 0);
        AtlasAnomalies.insert(generator, state, cell, AsteroidStorm.ANOMALY_ID, AtlasAnomalyRecord.Source.WORLD, null);
        state.setCatalystOn(BREACH_TERMINUS.itemId(), true);

        UseVerdict verdict = onlyVerdict(ActiveCatalysts.plan(generator, state, cell, itemId -> 1));

        assertEquals("catalyst.refused.conflict", verdict.refusalKey());
        assertEquals(AtlasNames.anomaly(AsteroidStorm.ANOMALY_ID), verdict.args().get("other"));
        assertEquals(AtlasNames.anomaly(SpatialBreach.ANOMALY_ID), verdict.args().get("anomaly"));
    }

    @Test
    void aLaterCatalystConflictingWithAnEarlierOneIsSkipped() {
        ChartTables.useAnomalies(List.of(definition(SpatialBreach.ANOMALY_ID, Set.of(AsteroidStorm.ANOMALY_ID)), definition(AsteroidStorm.ANOMALY_ID, Set.of())));
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell cell = system(generator, 0);
        state.setCatalystOn(STORM_TERMINUS.itemId(), true);
        state.setCatalystOn(BREACH_TERMINUS.itemId(), true);

        List<ActiveCatalysts.Planned> planned = ActiveCatalysts.plan(generator, state, cell, itemId -> 1);

        assertEquals(List.of(AsteroidStorm.ANOMALY_ID), ActiveCatalysts.anomalyIds(planned));
        assertEquals("catalyst.refused.conflict", planned.get(1).verdict().refusalKey());
        assertEquals(AtlasNames.anomaly(AsteroidStorm.ANOMALY_ID), planned.get(1).verdict().args().get("other"));
    }

    @Test
    void aCatalystThatOnlyFitsAfterThePreviousRunClosesIsNotApplied() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell cell = system(generator, 0);
        AtlasCell completed = system(generator, 1);
        state.add(completed, AtlasState.COMPLETED);
        state.setCatalystOn(BREACH_TERMINUS.itemId(), true);
        List<ActiveCatalysts.Planned> refusedBefore = ActiveCatalysts.plan(generator, state, completed, itemId -> 1);
        List<ActiveCatalysts.Planned> appliesNow = ActiveCatalysts.plan(generator, state, cell, itemId -> 1);

        assertEquals(List.of(), ActiveCatalysts.anomalyIds(ActiveCatalysts.alsoApplyingIn(appliesNow, refusedBefore)));
        assertEquals(List.of(SpatialBreach.ANOMALY_ID), ActiveCatalysts.anomalyIds(ActiveCatalysts.alsoApplyingIn(appliesNow, appliesNow)));
    }

    @Test
    void uncarriedCatalystsAreLeftOutOfThePlan() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        state.setCatalystOn(BREACH_TERMINUS.itemId(), true);

        assertEquals(List.of(), ActiveCatalysts.plan(generator, state, system(generator, 0), itemId -> 0));
    }

    @Test
    void aStarCatalystFitsOnlyFarSystemsAndNeedsItsStarEffect() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell far = farSystem(generator);
        AtlasCell near = system(generator, 0);
        state.setCatalystOn(PLAIN_HYPERSHUNT.itemId(), true);
        assertEquals("catalyst.refused.not_insertable", onlyVerdict(ActiveCatalysts.plan(generator, state, far, itemId -> 1)).refusalKey());
        state.setCatalystOn(PLAIN_HYPERSHUNT.itemId(), false);
        state.setCatalystOn(HYPERSHUNT_SURVEY.itemId(), true);

        assertEquals(CoronalHypershunt.TOO_CLOSE_KEY, onlyVerdict(ActiveCatalysts.plan(generator, state, near, itemId -> 1)).refusalKey());
        assertEquals(List.of(CoronalHypershunt.ANOMALY_ID), ActiveCatalysts.anomalyIds(ActiveCatalysts.plan(generator, state, far, itemId -> 1)));
    }

    @Test
    void aStarCatalystNeverReplacesAnotherStarAnomaly() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell far = farSystem(generator);
        assertTrue(AtlasAnomalies.insert(generator, state, far, StarAnomalies.BLACK_HOLE.id(), AtlasAnomalyRecord.Source.WORLD, null, null, true).check().allows());
        state.setCatalystOn(HYPERSHUNT_SURVEY.itemId(), true);

        UseVerdict verdict = onlyVerdict(ActiveCatalysts.plan(generator, state, far, itemId -> 1));

        assertEquals("catalyst.refused.conflict", verdict.refusalKey());
        assertEquals(AtlasNames.anomaly(StarAnomalies.BLACK_HOLE.id()), verdict.args().get("other"));
    }

    @Test
    void theTogglesSurviveASaveRepairAndAMissingSetIsRepaired() throws Exception {
        AtlasState state = new AtlasState(SEED);
        state.setCatalystOn(BREACH_TERMINUS.itemId(), true);
        AtlasState blank = new AtlasState(SEED);
        Field field = AtlasState.class.getDeclaredField("activeCatalystIds");
        field.setAccessible(true);
        field.set(blank, null);

        assertEquals(List.of(BREACH_TERMINUS.itemId()), state.repaired().activeCatalystIds());
        assertEquals(List.of(), blank.repaired().activeCatalystIds());
    }
}
