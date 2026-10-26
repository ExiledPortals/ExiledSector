package exiledsector.charts.atlas;

import exiledsector.charts.ChartAnomalies;
import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.AsteroidStorm;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.anomaly.SpatialBreach;
import exiledsector.charts.anomaly.StarAnomalies;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AtlasAnomaliesTest {

    private static final long SEED = 0x51A7E5L;
    private static final List<AtlasRegion> REGIONS = List.of(new AtlasRegion(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of()));

    private static AtlasRules rules(AtlasMarkerRule anomalyRule, List<AtlasAnomalyCandidate> anomalies) {
        return new AtlasRules(REGIONS, Map.of("compact", 1f), Map.of("pirates", 1f), new AtlasMarkerRule(0.3f, 0f, 0.3f), AtlasMarkerRule.NEVER,
                anomalyRule, List.of(), anomalies);
    }

    private static AtlasGenerator generator(AtlasState state) {
        return new AtlasGenerator(SEED, rules(AtlasMarkerRule.NEVER, List.of()), AtlasUnlocks.NONE, state);
    }

    private static ChartAnomalies.Definition definition(String id, boolean insertable, Set<String> excludes, String requiredMod) {
        return new ChartAnomalies.Definition(id, 1f, 1, 15, requiredMod, "", insertable, excludes, "black_hole".equals(id));
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

    private static AtlasCell emptySpace(AtlasGenerator generator) {
        for (AtlasCell cell : AtlasCell.ORIGIN.around(6)) {
            if (generator.isVoid(cell)) {
                return cell;
            }
        }
        throw new AssertionError("no empty space");
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
    void anInsertedAnomalyJoinsTheSystemAndIsRecordedWithItsSource() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell cell = system(generator, 0);

        AtlasAnomalies.Insertion insertion = AtlasAnomalies.insert(generator, state, cell, SpatialBreach.ID, AtlasAnomalyRecord.Source.WORLD, null);

        assertEquals(AtlasAnomalies.InsertCheck.ALLOWED, insertion.check());
        assertEquals(List.of(SpatialBreach.ID), AtlasAnomalies.effective(generator, state, cell));
        assertEquals(List.of(new AtlasAnomalyRecord(insertion.recordId(), cell, SpatialBreach.ID, AtlasAnomalyRecord.Source.WORLD)),
                state.anomalyRecords(cell));
        assertEquals(AtlasAnomalies.InsertCheck.ALREADY_PRESENT, AtlasAnomalies.insert(generator, state, cell, SpatialBreach.ID,
                AtlasAnomalyRecord.Source.PLAYER, null).check());
        assertEquals(List.of(), AtlasAnomalies.effective(generator, state, system(generator, 1)));
    }

    @Test
    void insertingRefusesWithTheReason() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell cell = system(generator, 0);
        AtlasCell completed = system(generator, 1);
        state.add(completed, AtlasState.COMPLETED);

        assertEquals(AtlasAnomalies.InsertCheck.UNKNOWN, AtlasAnomalies.canInsert(generator, state, cell, "ion_storm", null));
        assertEquals(AtlasAnomalies.InsertCheck.NOT_INSERTABLE, AtlasAnomalies.canInsert(generator, state, cell, "black_hole", null));
        assertEquals(AtlasAnomalies.InsertCheck.NO_SYSTEM, AtlasAnomalies.canInsert(generator, state, AtlasCell.ORIGIN, SpatialBreach.ID, null));
        assertEquals(AtlasAnomalies.InsertCheck.NO_SYSTEM, AtlasAnomalies.canInsert(generator, state, emptySpace(generator), SpatialBreach.ID, null));
        assertEquals(AtlasAnomalies.InsertCheck.COMPLETED, AtlasAnomalies.canInsert(generator, state, completed, SpatialBreach.ID, null));
        assertEquals(AtlasAnomalies.InsertCheck.OPEN_RUN, AtlasAnomalies.canInsert(generator, state, cell, SpatialBreach.ID, cell));
        assertEquals(List.of(), state.anomalyRecords());
    }

    @Test
    void insertingRefusesMissingModsConflictsAndSystemsTheAnomalyDoesNotFit() {
        ChartTables.useAnomalies(List.of(definition(SpatialBreach.ID, true, Set.of(AsteroidStorm.ID), ""), definition(AsteroidStorm.ID, true, Set.of(), ""),
                definition("black_hole", true, Set.of(), "a_mod_that_is_not_here")));
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell cell = system(generator, 0);

        assertEquals(AtlasAnomalies.InsertCheck.NOT_INSERTABLE, AtlasAnomalies.canInsert(generator, state, cell, "black_hole", null));
        assertEquals(AtlasAnomalies.InsertCheck.ALLOWED, AtlasAnomalies.insert(generator, state, cell, AsteroidStorm.ID, AtlasAnomalyRecord.Source.PLAYER, null).check());
        assertEquals(AtlasAnomalies.InsertCheck.CONFLICT, AtlasAnomalies.canInsert(generator, state, cell, SpatialBreach.ID, null));

        ChartAnomaly nowhere = new ChartAnomaly() {
            @Override
            public String id() {
                return SpatialBreach.ID;
            }

            @Override
            public boolean fitsAt(AtlasTraits traits) {
                return false;
            }
        };
        ChartTables.useAnomalies(List.of(nowhere), List.of(definition(SpatialBreach.ID, true, Set.of(), "")));
        assertEquals(AtlasAnomalies.InsertCheck.DOES_NOT_FIT, AtlasAnomalies.canInsert(generator, state, system(generator, 1), SpatialBreach.ID, null));
        ChartTables.useAnomalies(List.of(SpatialBreach.INSTANCE), List.of(definition(SpatialBreach.ID, true, Set.of(), "a_mod_that_is_not_here")));
        assertEquals(AtlasAnomalies.InsertCheck.MOD_MISSING, AtlasAnomalies.canInsert(generator, state, system(generator, 1), SpatialBreach.ID, null));
    }

    @Test
    void aRecordMovesBetweenSystemsButNeverOutOfTheOpenRun() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell from = system(generator, 0);
        AtlasCell to = system(generator, 1);
        int recordId = AtlasAnomalies.insert(generator, state, from, SpatialBreach.ID, AtlasAnomalyRecord.Source.WORLD, null).recordId();

        assertEquals(AtlasAnomalies.InsertCheck.OPEN_RUN, AtlasAnomalies.move(generator, state, recordId, to, from));
        assertEquals(AtlasAnomalies.InsertCheck.ALREADY_PRESENT, AtlasAnomalies.move(generator, state, recordId, from, null));
        assertEquals(AtlasAnomalies.InsertCheck.UNKNOWN, AtlasAnomalies.move(generator, state, recordId + 1, to, null));
        assertEquals(AtlasAnomalies.InsertCheck.ALLOWED, AtlasAnomalies.move(generator, state, recordId, to, null));

        assertEquals(List.of(), AtlasAnomalies.effective(generator, state, from));
        assertEquals(List.of(SpatialBreach.ID), AtlasAnomalies.effective(generator, state, to));
        assertEquals(to, state.anomalyRecord(recordId).cell());
        assertEquals(1, state.anomalyRecords().size());
    }

    @Test
    void closingARunSpendsInsertedAnomaliesAndFailingAlsoSpendsAllRolledOnesButTheStar() {
        AtlasState state = new AtlasState(SEED);
        AtlasRules rolled = rules(new AtlasMarkerRule(1f, 0f, 1f), List.of(new AtlasAnomalyCandidate("black_hole", 1f, "", true, Set.of(), null),
                new AtlasAnomalyCandidate(AsteroidStorm.ID, 1f, "")));
        AtlasGenerator generator = new AtlasGenerator(SEED, rolled, AtlasUnlocks.NONE, state);
        AtlasCell failed = system(generator, 0);
        AtlasCell completed = system(generator, 1);
        AtlasAnomalies.insert(generator, state, failed, SpatialBreach.ID, AtlasAnomalyRecord.Source.PLAYER, null);
        AtlasAnomalies.insert(generator, state, completed, SpatialBreach.ID, AtlasAnomalyRecord.Source.PLAYER, null);
        List<String> before = AtlasAnomalies.effective(generator, state, failed);
        assertEquals(Set.of("black_hole", AsteroidStorm.ID), Set.copyOf(before.subList(0, 2)));
        assertEquals(SpatialBreach.ID, before.get(2));

        AtlasProgress.fail(state, failed);
        AtlasProgress.complete(generator, state, completed, 3);

        assertEquals(List.of("black_hole"), AtlasAnomalies.effective(generator, state, failed));
        assertEquals(StarAnomalies.BLACK_HOLE, ChartAnomalies.starOf(AtlasAnomalies.effective(generator, state, failed)));
        assertEquals(List.of(), state.anomalyRecords());
        assertEquals(AtlasAnomalies.InsertCheck.ALLOWED, AtlasAnomalies.canInsert(generator, state, failed, SpatialBreach.ID, null));
    }

    @Test
    void recordsSurviveEncodingAndUnreadableOnesAreIgnored() {
        AtlasAnomalyRecord record = new AtlasAnomalyRecord(3, new AtlasCell(-4, 9), SpatialBreach.ID, AtlasAnomalyRecord.Source.PLAYER);

        assertEquals(record, AtlasAnomalyRecord.decode(record.recordId(), record.encode()));
        assertNull(AtlasAnomalyRecord.decode(1, "garbage"));
        assertNull(AtlasAnomalyRecord.decode(1, "12,spatial_breach,ALIEN"));
        assertNull(AtlasAnomalyRecord.decode(1, null));
        assertNotNull(AtlasAnomalyRecord.decode(1, "12,spatial_breach,world"));
    }
}
