package exiledsector.charts.anomaly.relay;

import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasAnomalyChance;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRules;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelayStagesTest {

    private static final long SEED = 0x7e1a7L;

    private AtlasState state;
    private AtlasGenerator generator;
    private AtlasCell start;

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadAnomalies();
        AtlasData.registerNodeEffects(new JSONArray().put(new JSONObject().put("id", RelayStages.STAGE_EFFECTS_ID)
                .put("effects", "CHART_FLEET_SIZE_PERCENT:20; CHART_QUANTITY_PERCENT:25")));
        state = new AtlasState(SEED);
        List<AtlasRegion> regions = List.of(new AtlasRegion(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of()));
        AtlasRules rules = new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 1f), AtlasAnomalyChance.NEVER, AtlasAnomalyChance.NEVER,
                List.of(), List.of());
        generator = new AtlasGenerator(SEED, rules, AtlasUnlocks.NONE, state);
        start = generator.lanes(AtlasCell.ORIGIN).get(0);
    }

    @AfterEach
    void tearDown() throws Exception {
        AtlasData.registerNodeEffects(new JSONArray());
        ChartTables.resetAnomalies();
    }

    private int insertAtStart(AtlasAnomalyRecord.Chain chain) {
        return AtlasAnomalies.insert(generator, state, start, RelayNetwork.ANOMALY_ID, AtlasAnomalyRecord.Source.WORLD, null, chain).recordId();
    }

    private static void everyNetworkCarriesOneExtraStage() {
        ChartTables.useAnomalies(List.of(RelayNetwork.INSTANCE), List.of(new ChartAnomalies.Definition(RelayNetwork.ANOMALY_ID, 1f, 1, 15, "", "", true, Set.of(),
                false, List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_RELAY_CHAIN_STAGES, 1f)))));
    }

    @Test
    void theRelayNetworkCanBeInsertedSoTheChainCanMoveIt() {
        assertTrue(AtlasAnomalies.canInsert(generator, state, start, RelayNetwork.ANOMALY_ID, null).allows());
    }

    @Test
    void aChainRunsThroughThreeSystems() {
        insertAtStart(null);

        assertEquals(3, RelayRules.STAGES);
        assertEquals(new AtlasAnomalyRecord.Chain(1, 3, null), RelayStages.stageAt(generator, state, start));
    }

    @Test
    void completingAFirstStageWithEveryRelayAwakeStartsTheChainFurtherOut() {
        state.add(start, AtlasState.COMPLETED);

        AtlasCell target = RelayStages.settle(generator, state, start, true, true);

        assertNotNull(target);
        AtlasAnomalyRecord moved = RelayStages.recordAt(state, target);
        assertEquals(new AtlasAnomalyRecord.Chain(2, RelayRules.STAGES, start), moved.chain());
        assertEquals(AtlasAnomalyRecord.Source.WORLD, moved.source());
        assertEquals(moved.chain(), RelayStages.stageAt(generator, state, target));
        assertTrue(target.centreDistance() > start.centreDistance());
    }

    @Test
    void theRecordItselfMovesOnAndCarriesTheNextStage() {
        int recordId = insertAtStart(new AtlasAnomalyRecord.Chain(2, 4, AtlasCell.ORIGIN));
        state.add(start, AtlasState.COMPLETED);

        AtlasCell target = RelayStages.settle(generator, state, start, true, true);

        assertEquals(target, state.anomalyRecord(recordId).cell());
        assertEquals(new AtlasAnomalyRecord.Chain(3, 4, start), state.anomalyRecord(recordId).chain());
        assertEquals(1, state.anomalyRecords().size());
    }

    @Test
    void forkedTraceMovesACompletedStageOnToTwoSystemsEachCarryingTheNextStage() {
        int recordId = insertAtStart(new AtlasAnomalyRecord.Chain(1, 3, null));
        state.add(start, AtlasState.COMPLETED);

        List<AtlasCell> targets = RelayStages.settle(generator, state, start, AtlasAnomalies.effective(generator, state, start), true, true, 2);

        assertEquals(2, targets.size());
        assertNotEquals(targets.get(0), targets.get(1));
        assertEquals(targets.get(0), state.anomalyRecord(recordId).cell());
        assertEquals(2, state.anomalyRecords().size());
        for (AtlasCell target : targets) {
            AtlasAnomalyRecord branch = RelayStages.recordAt(state, target);
            assertEquals(new AtlasAnomalyRecord.Chain(2, 3, start), branch.chain());
            assertEquals(AtlasAnomalyRecord.Source.WORLD, branch.source());
        }
    }

    @Test
    void eachForkedBranchRunsItsOwnRemainingStagesToItsOwnEnd() {
        insertAtStart(null);
        List<AtlasCell> frontier = List.of(start);
        int lastStages = 0;
        for (int stage = 1; stage <= RelayRules.STAGES; stage++) {
            List<AtlasCell> next = new ArrayList<>();
            for (AtlasCell cell : frontier) {
                AtlasAnomalyRecord.Chain chain = RelayStages.stageAt(generator, state, cell);
                assertEquals(stage, chain.stage());
                if (chain.isLast()) {
                    lastStages++;
                }
                state.add(cell, AtlasState.COMPLETED);
                next.addAll(RelayStages.settle(generator, state, cell, AtlasAnomalies.effective(generator, state, cell), true, true, 2));
            }
            frontier = next;
        }

        assertEquals(List.of(), frontier);
        assertTrue(lastStages >= 2, "branches reaching the last stage: " + lastStages);
        assertEquals(List.of(), state.anomalyRecords());
    }

    @Test
    void aNetworkThatCouldNotBePlacedDoesNotFork() {
        insertAtStart(new AtlasAnomalyRecord.Chain(2, 4, AtlasCell.ORIGIN));
        state.add(start, AtlasState.COMPLETED);

        assertEquals(1, RelayStages.settle(generator, state, start, AtlasAnomalies.effective(generator, state, start), false, false, 2).size());
        assertEquals(1, state.anomalyRecords().size());
    }

    @Test
    void aDormantRelayLosesTheTrace() {
        int recordId = insertAtStart(new AtlasAnomalyRecord.Chain(2, 4, AtlasCell.ORIGIN));

        assertNull(RelayStages.settle(generator, state, start, true, false));
        assertNull(state.anomalyRecord(recordId));
    }

    @Test
    void aMiddleStageMovesOnWithoutTheBossDying() {
        int recordId = insertAtStart(new AtlasAnomalyRecord.Chain(2, 4, AtlasCell.ORIGIN));

        AtlasCell target = RelayStages.settle(generator, state, start, true, true);

        assertNotNull(target);
        assertEquals(new AtlasAnomalyRecord.Chain(3, 4, start), state.anomalyRecord(recordId).chain());
    }

    @Test
    void aNetworkThatCouldNotBePlacedCarriesItsChainOnAtTheSameStage() {
        int recordId = insertAtStart(new AtlasAnomalyRecord.Chain(2, 4, AtlasCell.ORIGIN));
        state.add(start, AtlasState.COMPLETED);

        AtlasCell target = RelayStages.settle(generator, state, start, false, false);

        assertNotNull(target);
        assertEquals(new AtlasAnomalyRecord.Chain(2, 4, start), state.anomalyRecord(recordId).chain());
        assertEquals(target, state.anomalyRecord(recordId).cell());
    }

    @Test
    void theLastStageEndsTheChain() {
        insertAtStart(new AtlasAnomalyRecord.Chain(3, 3, AtlasCell.ORIGIN));
        state.add(start, AtlasState.COMPLETED);

        assertNull(RelayStages.settle(generator, state, start, true, true));
        assertEquals(List.of(), state.anomalyRecords());
    }

    @Test
    void theStageOfASystemComesFromItsRecordOrStartsAtOne() {
        assertNull(RelayStages.stageAt(generator, state, start));

        insertAtStart(null);
        assertEquals(new AtlasAnomalyRecord.Chain(1, RelayRules.STAGES, null), RelayStages.stageAt(generator, state, start));
    }

    @Test
    void extraStagesCountOnceWhenTheChainStartsSoAChainWhereEveryRunAddsOneStillEnds() {
        everyNetworkCarriesOneExtraStage();
        insertAtStart(null);
        int stages = RelayRules.STAGES + 1;
        assertEquals(new AtlasAnomalyRecord.Chain(1, stages, null), RelayStages.stageAt(generator, state, start));

        AtlasCell cell = start;
        int settled = 0;
        while (cell != null && settled <= stages) {
            AtlasAnomalyRecord.Chain chain = RelayStages.stageAt(generator, state, cell);
            assertEquals(stages, chain.stages());
            assertEquals(RelayStages.stageEffects(chain.stage()), RelayStages.join(new AtlasRunNode(cell.pack(), "pirates", List.of(),
                    List.of(RelayNetwork.ANOMALY_ID)), generator, state, cell).effects());
            state.add(cell, AtlasState.COMPLETED);
            cell = RelayStages.settle(generator, state, cell, true, true);
            settled++;
        }

        assertNull(cell);
        assertEquals(stages, settled);
        assertEquals(List.of(), state.anomalyRecords());
    }

    @Test
    void theChainStagesLineAddsStagesThroughTheRunContext() {
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_RELAY_CHAIN_STAGES, 2f)));

        assertEquals(2, RelayStages.extraStages(context));
        assertEquals(0, RelayStages.extraStages(ChartGenContext.none()));
        assertEquals(0, RelayStages.extraStages(null));
        assertEquals("2 additional Relay Network stages", ChartModifier.CHART_RELAY_CHAIN_STAGES.description(2f).plain());
    }

    @Test
    void eachStagePastTheFirstAddsTheStageLinesOnceMore() {
        assertEquals(List.of(), RelayStages.stageEffects(1));
        assertEquals(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_FLEET_SIZE_PERCENT, 40f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_QUANTITY_PERCENT, 50f)), RelayStages.stageEffects(3));
    }

    @Test
    void openingAChainSystemAddsItsStageLinesWhetherOrNotItIsTheLastStage() {
        AtlasRunNode node = new AtlasRunNode(start.pack(), "pirates", List.of(), List.of(RelayNetwork.ANOMALY_ID));
        int recordId = insertAtStart(new AtlasAnomalyRecord.Chain(3, 3, AtlasCell.ORIGIN));

        AtlasRunNode last = RelayStages.join(node, generator, state, start);

        assertEquals(RelayStages.stageEffects(3), last.effects());
        state.removeAnomaly(recordId);
        insertAtStart(new AtlasAnomalyRecord.Chain(3, 4, AtlasCell.ORIGIN));
        assertEquals(RelayStages.stageEffects(3), RelayStages.join(node, generator, state, start).effects());
        AtlasRunNode plain = new AtlasRunNode(start.pack(), "pirates", List.of(), List.of());
        assertSame(plain, ChartAnomalies.joinRunNode(plain, generator, state, start, null));
    }

    @Test
    void aClosingRunSettlesItsChainOnlyOnce() {
        ChartRun run = new ChartRun("run1", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);

        assertTrue(RelayStates.markSettled(run));
        assertFalse(RelayStates.markSettled(run));
    }

    @Test
    void aFirstStageAddsNothingAndASystemWithoutTheNetworkIsLeftAlone() {
        AtlasRunNode plain = new AtlasRunNode(start.pack(), "pirates", List.of(), List.of());
        assertSame(plain, RelayStages.join(plain, generator, state, start));

        AtlasRunNode node = new AtlasRunNode(start.pack(), "pirates", List.of(), List.of(RelayNetwork.ANOMALY_ID));
        AtlasRunNode added = RelayStages.join(node, generator, state, start);
        assertEquals(List.of(), added.effects());

        insertAtStart(null);
        AtlasRunNode first = RelayStages.join(node, generator, state, start);
        assertEquals(List.of(), first.effects());
    }
}
