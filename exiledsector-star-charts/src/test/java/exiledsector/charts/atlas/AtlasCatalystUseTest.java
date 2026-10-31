package exiledsector.charts.atlas;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.AsteroidStorm;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.StarAnomalies;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.charts.anomaly.hypershunt.CoronalHypershunt;
import exiledsector.items.Consumables;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.UseVerdict;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasCatalystUseTest {

    private static final long SEED = 0x51A7E5L;
    private static final List<AtlasRegion> REGIONS = List.of(new AtlasRegion(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of()));
    private static final AtlasCatalysts.Catalyst BREACH_TERMINUS = new AtlasCatalysts.Catalyst("test_breach_terminus", SpatialBreach.ANOMALY_ID,
            AtlasCatalysts.Family.TERMINUS, 1f, 1);
    private static final AtlasCatalysts.Catalyst HYPERSHUNT_SURVEY = new AtlasCatalysts.Catalyst("test_hypershunt_survey", CoronalHypershunt.ANOMALY_ID,
            AtlasCatalysts.Family.SURVEY, 0f, 1, AtlasCatalysts.Effect.STAR, "");

    private static AtlasGenerator generator(AtlasState state) {
        AtlasRules rules = new AtlasRules(REGIONS, Map.of("compact", 1f), Map.of("pirates", 1f), new AtlasMarkerRule(0.3f, 0f, 0.3f),
                AtlasMarkerRule.NEVER, List.of(), List.of());
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

    private static CraftingMaterials holding(int catalysts) {
        return CraftingMaterials.storedForTests(new HashMap<>(Map.of(BREACH_TERMINUS.itemId(), catalysts)));
    }

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadAnomalies();
    }

    @AfterEach
    void tearDown() {
        ChartTables.resetAnomalies();
    }

    @Test
    void aCatalystInsertsItsAnomalyAsThePlayersAndIsSpentOnlyWhenItTakes() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell cell = system(generator, 0);
        AtlasCatalystUse use = new AnomalyCatalystUse(BREACH_TERMINUS, generator, state, null, Set.of(cell));
        CraftingMaterials materials = holding(2);

        AtlasCatalystUse.Outcome outcome = Consumables.use(use, cell, materials);

        assertNotNull(outcome);
        assertEquals(Map.of("anomaly", AtlasCatalystUse.anomalyName(SpatialBreach.ANOMALY_ID)), outcome.args());
        assertEquals(List.of(new AtlasAnomalyRecord(state.anomalyRecords(cell).get(0).recordId(), cell, SpatialBreach.ANOMALY_ID,
                AtlasAnomalyRecord.Source.PLAYER)), state.anomalyRecords(cell));
        assertEquals(1, materials.count(BREACH_TERMINUS.itemId()));
        assertEquals("catalyst.refused.already_present", use.check(cell).refusalKey());
        assertNull(Consumables.use(use, cell, materials));
        assertEquals(1, materials.count(BREACH_TERMINUS.itemId()));
    }

    @Test
    void aCatalystRefusesFoggedSpentAndHeldSystemsWithTheirReasons() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell open = system(generator, 0);
        AtlasCell completed = system(generator, 1);
        AtlasCell fogged = system(generator, 2);
        state.add(completed, AtlasState.COMPLETED);
        AtlasCatalystUse use = new AnomalyCatalystUse(BREACH_TERMINUS, generator, state, open, Set.of(open, completed, AtlasCell.ORIGIN));

        assertEquals(AtlasCatalystUse.FOGGED_KEY, use.check(fogged).refusalKey());
        assertEquals("catalyst.refused.completed", use.check(completed).refusalKey());
        assertEquals("catalyst.refused.open_run", use.check(open).refusalKey());
        assertEquals("catalyst.refused.no_system", use.check(AtlasCell.ORIGIN).refusalKey());
        assertNull(Consumables.use(use, open, holding(1)));
    }

    @Test
    void aConflictNamesTheAnomalyAlreadyThere() {
        ChartTables.useAnomalies(List.of(definition(SpatialBreach.ANOMALY_ID, Set.of(AsteroidStorm.ANOMALY_ID)), definition(AsteroidStorm.ANOMALY_ID, Set.of())));
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell cell = system(generator, 0);
        AtlasAnomalies.insert(generator, state, cell, AsteroidStorm.ANOMALY_ID, AtlasAnomalyRecord.Source.WORLD, null);

        UseVerdict verdict = new AnomalyCatalystUse(BREACH_TERMINUS, generator, state, null, Set.of(cell)).check(cell);

        assertEquals("catalyst.refused.conflict", verdict.refusalKey());
        assertEquals(AtlasCatalystUse.anomalyName(AsteroidStorm.ANOMALY_ID), verdict.args().get("other"));
        assertEquals(AtlasCatalystUse.anomalyName(SpatialBreach.ANOMALY_ID), verdict.args().get("anomaly"));
    }

    @Test
    void aStarCatalystTurnsAFarSystemsStarIntoItsAnomaly() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell far = farSystem(generator);
        AtlasCell near = system(generator, 0);
        AtlasCatalystUse use = new AnomalyCatalystUse(HYPERSHUNT_SURVEY, generator, state, null, Set.of(far, near));
        AtlasCatalysts.Catalyst plain = new AtlasCatalysts.Catalyst("test_plain", CoronalHypershunt.ANOMALY_ID, AtlasCatalysts.Family.SURVEY, 0f, 1);

        assertEquals("catalyst.refused.not_insertable", new AnomalyCatalystUse(plain, generator, state, null, Set.of(far)).check(far).refusalKey());
        assertEquals(CoronalHypershunt.TOO_CLOSE_KEY, use.check(near).refusalKey());
        assertTrue(use.check(far).allowed());
        assertNotNull(use.apply(far));
        assertEquals(List.of(CoronalHypershunt.ANOMALY_ID), AtlasAnomalies.effective(generator, state, far));
        assertSame(CoronalHypershunt.INSTANCE, ChartAnomalies.starOf(AtlasAnomalies.effective(generator, state, far)));
        assertEquals("catalyst.refused.already_present", use.check(far).refusalKey());
    }

    @Test
    void aStarCatalystNeverReplacesAnotherStarAnomaly() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell far = farSystem(generator);
        assertTrue(AtlasAnomalies.insertStar(generator, state, far, StarAnomalies.BLACK_HOLE.id(), AtlasAnomalyRecord.Source.WORLD, null).check().allows());

        UseVerdict verdict = new AnomalyCatalystUse(HYPERSHUNT_SURVEY, generator, state, null, Set.of(far)).check(far);

        assertEquals("catalyst.refused.conflict", verdict.refusalKey());
        assertEquals(AtlasCatalystUse.anomalyName(StarAnomalies.BLACK_HOLE.id()), verdict.args().get("other"));
        assertEquals(List.of(StarAnomalies.BLACK_HOLE.id()), AtlasAnomalies.effective(generator, state, far));
    }
}
