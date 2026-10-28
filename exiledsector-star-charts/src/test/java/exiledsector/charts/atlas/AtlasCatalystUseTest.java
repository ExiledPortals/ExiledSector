package exiledsector.charts.atlas;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.AsteroidStorm;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.breach.SpatialBreach;
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

class AtlasCatalystUseTest {

    private static final long SEED = 0x51A7E5L;
    private static final List<AtlasRegion> REGIONS = List.of(new AtlasRegion(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of()));
    private static final AtlasCatalysts.Catalyst BREACH_TERMINUS = new AtlasCatalysts.Catalyst("test_breach_terminus", SpatialBreach.ID,
            AtlasCatalysts.Family.TERMINUS, 1f, 1);

    private static AtlasGenerator generator(AtlasState state) {
        AtlasRules rules = new AtlasRules(REGIONS, Map.of("compact", 1f), Map.of("pirates", 1f), new AtlasMarkerRule(0.3f, 0f, 0.3f),
                AtlasMarkerRule.NEVER, AtlasMarkerRule.NEVER, List.of(), List.of());
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
        AtlasCatalystUse use = new AtlasCatalystUse(BREACH_TERMINUS, generator, state, null, Set.of(cell));
        CraftingMaterials materials = holding(2);

        AtlasAnomalies.Insertion insertion = Consumables.use(use, cell, materials);

        assertNotNull(insertion);
        assertEquals(List.of(new AtlasAnomalyRecord(insertion.recordId(), cell, SpatialBreach.ID, AtlasAnomalyRecord.Source.PLAYER)),
                state.anomalyRecords(cell));
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
        AtlasCatalystUse use = new AtlasCatalystUse(BREACH_TERMINUS, generator, state, open, Set.of(open, completed, AtlasCell.ORIGIN));

        assertEquals(AtlasCatalystUse.FOGGED_KEY, use.check(fogged).refusalKey());
        assertEquals("catalyst.refused.completed", use.check(completed).refusalKey());
        assertEquals("catalyst.refused.open_run", use.check(open).refusalKey());
        assertEquals("catalyst.refused.no_system", use.check(AtlasCell.ORIGIN).refusalKey());
        assertNull(Consumables.use(use, open, holding(1)));
    }

    @Test
    void aConflictNamesTheAnomalyAlreadyThere() {
        ChartTables.useAnomalies(List.of(definition(SpatialBreach.ID, Set.of(AsteroidStorm.ID)), definition(AsteroidStorm.ID, Set.of())));
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell cell = system(generator, 0);
        AtlasAnomalies.insert(generator, state, cell, AsteroidStorm.ID, AtlasAnomalyRecord.Source.WORLD, null);

        UseVerdict verdict = new AtlasCatalystUse(BREACH_TERMINUS, generator, state, null, Set.of(cell)).check(cell);

        assertEquals("catalyst.refused.conflict", verdict.refusalKey());
        assertEquals(AtlasCatalystUse.anomalyName(AsteroidStorm.ID), verdict.args().get("other"));
        assertEquals(AtlasCatalystUse.anomalyName(SpatialBreach.ID), verdict.args().get("anomaly"));
    }
}
