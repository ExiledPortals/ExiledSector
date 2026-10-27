package exiledsector.charts.anomaly.relay;

import exiledsector.charts.ChartTables;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasMarkerRule;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRules;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelayChainTest {

    private static final long SEED = 0x7e1a7L;

    private AtlasState state;
    private AtlasGenerator generator;
    private AtlasCell start;

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadAnomalies();
        AtlasData.registerNodeEffects(new JSONArray().put(new JSONObject().put("id", RelayChain.STAGE_EFFECTS_ID)
                .put("effects", "CHART_FLEET_SIZE_PERCENT:20; CHART_QUANTITY_PERCENT:25")));
        state = new AtlasState(SEED);
        List<AtlasRegion> regions = List.of(new AtlasRegion(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of()));
        AtlasRules rules = new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 1f), AtlasMarkerRule.NEVER, AtlasMarkerRule.NEVER,
                AtlasMarkerRule.NEVER, List.of(), List.of());
        generator = new AtlasGenerator(SEED, rules, AtlasUnlocks.NONE, state);
        start = generator.lanes(AtlasCell.ORIGIN).get(0);
    }

    @AfterEach
    void tearDown() throws Exception {
        AtlasData.registerNodeEffects(new JSONArray());
        ChartTables.resetAnomalies();
    }

    private int insertAtStart(AtlasAnomalyRecord.Chain chain) {
        return AtlasAnomalies.insert(generator, state, start, RelayNetwork.ID, AtlasAnomalyRecord.Source.WORLD, null, chain).recordId();
    }

    @Test
    void theRelayNetworkCanBeInsertedSoTheChainCanMoveIt() {
        assertTrue(AtlasAnomalies.canInsert(generator, state, start, RelayNetwork.ID, null).allows());
    }

    @Test
    void aChainRollsThreeOrFourStagesOnTheCellHash() {
        for (AtlasCell cell : AtlasCell.ORIGIN.around(4)) {
            int stages = RelayChain.rolledStages(state, cell);
            assertTrue(stages >= RelayRules.MIN_STAGES && stages <= RelayRules.MAX_STAGES, "stages " + stages);
            assertEquals(stages, RelayChain.rolledStages(state, cell));
        }
    }

    @Test
    void completingAFirstStageWithEveryRelayAwakeStartsTheChainFurtherOut() {
        state.add(start, AtlasState.COMPLETED);

        AtlasCell target = RelayChain.settle(generator, state, start, true, true, 0);

        assertNotNull(target);
        AtlasAnomalyRecord moved = RelayChain.recordAt(state, target);
        assertEquals(new AtlasAnomalyRecord.Chain(2, RelayChain.rolledStages(state, start), start), moved.chain());
        assertEquals(AtlasAnomalyRecord.Source.WORLD, moved.source());
        assertEquals(moved.chain(), RelayChain.stageAt(generator, state, target));
        assertTrue(target.centreDistance() > start.centreDistance());
    }

    @Test
    void theRecordItselfMovesOnAndCarriesTheNextStage() {
        int recordId = insertAtStart(new AtlasAnomalyRecord.Chain(2, 4, AtlasCell.ORIGIN));
        state.add(start, AtlasState.COMPLETED);

        AtlasCell target = RelayChain.settle(generator, state, start, true, true, 1);

        assertEquals(target, state.anomalyRecord(recordId).cell());
        assertEquals(new AtlasAnomalyRecord.Chain(3, 5, start), state.anomalyRecord(recordId).chain());
        assertEquals(1, state.anomalyRecords().size());
    }

    @Test
    void aDormantRelayOrAFailedRunLosesTheTrace() {
        int recordId = insertAtStart(new AtlasAnomalyRecord.Chain(2, 4, AtlasCell.ORIGIN));

        assertNull(RelayChain.settle(generator, state, start, true, false, 0));
        assertNull(state.anomalyRecord(recordId));

        insertAtStart(new AtlasAnomalyRecord.Chain(2, 4, AtlasCell.ORIGIN));
        assertNull(RelayChain.settle(generator, state, start, false, true, 0));
        assertEquals(List.of(), state.anomalyRecords());
    }

    @Test
    void theLastStageEndsTheChain() {
        insertAtStart(new AtlasAnomalyRecord.Chain(3, 3, AtlasCell.ORIGIN));
        state.add(start, AtlasState.COMPLETED);

        assertNull(RelayChain.settle(generator, state, start, true, true, 0));
        assertEquals(List.of(), state.anomalyRecords());
    }

    @Test
    void theStageOfASystemComesFromItsRecordOrStartsAtOne() {
        assertNull(RelayChain.stageAt(generator, state, start));

        insertAtStart(null);
        assertEquals(new AtlasAnomalyRecord.Chain(1, RelayChain.rolledStages(state, start), null), RelayChain.stageAt(generator, state, start));
    }

    @Test
    void eachStagePastTheFirstAddsTheStageLinesOnceMore() {
        assertEquals(List.of(), RelayChain.stageEffects(1));
        assertEquals(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_FLEET_SIZE_PERCENT, 40f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_QUANTITY_PERCENT, 50f)), RelayChain.stageEffects(3));
    }

    @Test
    void openingAChainSystemAddsItsStageLinesAndTheLastStageGuaranteesAUnique() {
        AtlasRunNode node = new AtlasRunNode(start.pack(), "pirates", List.of(), List.of(RelayNetwork.ID), false);
        insertAtStart(new AtlasAnomalyRecord.Chain(3, 3, AtlasCell.ORIGIN));

        AtlasRunNode last = RelayChain.join(node, generator, state, start, 0);
        AtlasRunNode extended = RelayChain.join(node, generator, state, start, 1);

        assertEquals(RelayChain.stageEffects(3), last.effects());
        assertTrue(last.guaranteedBossUnique());
        assertFalse(extended.guaranteedBossUnique());
        AtlasRunNode plain = new AtlasRunNode(start.pack(), "pirates", List.of(), List.of(), false);
        assertSame(plain, RelayChain.join(plain, generator, state, start, 0));
    }

    @Test
    void aFirstStageAddsNothingAndASystemWithoutTheNetworkIsLeftAlone() {
        AtlasRunNode node = new AtlasRunNode(start.pack(), "pirates", List.of(), List.of(RelayNetwork.ID), false);
        assertSame(node, RelayChain.join(node, generator, state, start, 0));

        insertAtStart(null);
        AtlasRunNode first = RelayChain.join(node, generator, state, start, 0);
        assertEquals(List.of(), first.effects());
        assertFalse(first.guaranteedBossUnique());
    }
}
