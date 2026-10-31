package exiledsector.charts.anomaly.relay;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.ChartAnomalies;
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
import java.util.Set;

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
                false, List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_RELAY_CHAIN_STAGES, 1f)))));
    }

    @Test
    void theRelayNetworkCanBeInsertedSoTheChainCanMoveIt() {
        assertTrue(AtlasAnomalies.canInsert(generator, state, start, RelayNetwork.ANOMALY_ID, null).allows());
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

        AtlasCell target = RelayChain.settle(generator, state, start, true, true, true);

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

        AtlasCell target = RelayChain.settle(generator, state, start, true, true, true);

        assertEquals(target, state.anomalyRecord(recordId).cell());
        assertEquals(new AtlasAnomalyRecord.Chain(3, 4, start), state.anomalyRecord(recordId).chain());
        assertEquals(1, state.anomalyRecords().size());
    }

    @Test
    void aDormantRelayOrAFailedRunLosesTheTrace() {
        int recordId = insertAtStart(new AtlasAnomalyRecord.Chain(2, 4, AtlasCell.ORIGIN));

        assertNull(RelayChain.settle(generator, state, start, true, true, false));
        assertNull(state.anomalyRecord(recordId));

        insertAtStart(new AtlasAnomalyRecord.Chain(2, 4, AtlasCell.ORIGIN));
        assertNull(RelayChain.settle(generator, state, start, false, true, true));
        assertEquals(List.of(), state.anomalyRecords());
    }

    @Test
    void aNetworkThatCouldNotBePlacedCarriesItsChainOnAtTheSameStage() {
        int recordId = insertAtStart(new AtlasAnomalyRecord.Chain(2, 4, AtlasCell.ORIGIN));
        state.add(start, AtlasState.COMPLETED);

        AtlasCell target = RelayChain.settle(generator, state, start, true, false, false);

        assertNotNull(target);
        assertEquals(new AtlasAnomalyRecord.Chain(2, 4, start), state.anomalyRecord(recordId).chain());
        assertEquals(target, state.anomalyRecord(recordId).cell());
    }

    @Test
    void theLastStageEndsTheChain() {
        insertAtStart(new AtlasAnomalyRecord.Chain(3, 3, AtlasCell.ORIGIN));
        state.add(start, AtlasState.COMPLETED);

        assertNull(RelayChain.settle(generator, state, start, true, true, true));
        assertEquals(List.of(), state.anomalyRecords());
    }

    @Test
    void theStageOfASystemComesFromItsRecordOrStartsAtOne() {
        assertNull(RelayChain.stageAt(generator, state, start));

        insertAtStart(null);
        assertEquals(new AtlasAnomalyRecord.Chain(1, RelayChain.rolledStages(state, start), null), RelayChain.stageAt(generator, state, start));
    }

    @Test
    void extraStagesCountOnceWhenTheChainStartsSoAChainWhereEveryRunAddsOneStillEnds() {
        everyNetworkCarriesOneExtraStage();
        insertAtStart(null);
        int stages = RelayChain.rolledStages(state, start) + 1;
        assertEquals(new AtlasAnomalyRecord.Chain(1, stages, null), RelayChain.stageAt(generator, state, start));

        AtlasCell cell = start;
        int settled = 0;
        while (cell != null && settled <= stages) {
            AtlasAnomalyRecord.Chain chain = RelayChain.stageAt(generator, state, cell);
            assertEquals(stages, chain.stages());
            assertEquals(chain.isLast(), RelayChain.join(new AtlasRunNode(cell.pack(), "pirates", List.of(), List.of(RelayNetwork.ANOMALY_ID), false),
                    generator, state, cell).guaranteedBossUnique());
            state.add(cell, AtlasState.COMPLETED);
            cell = RelayChain.settle(generator, state, cell, true, true, true);
            settled++;
        }

        assertNull(cell);
        assertEquals(stages, settled);
        assertEquals(List.of(), state.anomalyRecords());
    }

    @Test
    void theChainStagesLineAddsStagesThroughTheRunContext() {
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_RELAY_CHAIN_STAGES, 2f)));

        assertEquals(2, RelayChain.extraStages(context));
        assertEquals(0, RelayChain.extraStages(ChartGenContext.none()));
        assertEquals(0, RelayChain.extraStages(null));
        assertEquals("2 additional Relay Network stages", ChartEffect.CHART_RELAY_CHAIN_STAGES.description(2f).plain());
    }

    @Test
    void eachStagePastTheFirstAddsTheStageLinesOnceMore() {
        assertEquals(List.of(), RelayChain.stageEffects(1));
        assertEquals(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_FLEET_SIZE_PERCENT, 40f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_QUANTITY_PERCENT, 50f)), RelayChain.stageEffects(3));
    }

    @Test
    void openingAChainSystemAddsItsStageLinesAndTheLastStageGuaranteesAUnique() {
        AtlasRunNode node = new AtlasRunNode(start.pack(), "pirates", List.of(), List.of(RelayNetwork.ANOMALY_ID), false);
        int recordId = insertAtStart(new AtlasAnomalyRecord.Chain(3, 3, AtlasCell.ORIGIN));

        AtlasRunNode last = RelayChain.join(node, generator, state, start);

        assertEquals(RelayChain.stageEffects(3), last.effects());
        assertTrue(last.guaranteedBossUnique());
        state.removeAnomaly(recordId);
        insertAtStart(new AtlasAnomalyRecord.Chain(3, 4, AtlasCell.ORIGIN));
        assertFalse(RelayChain.join(node, generator, state, start).guaranteedBossUnique());
        AtlasRunNode plain = new AtlasRunNode(start.pack(), "pirates", List.of(), List.of(), false);
        assertSame(plain, RelayChain.join(plain, generator, state, start));
    }

    @Test
    void aFirstStageAddsNothingAndASystemWithoutTheNetworkIsLeftAlone() {
        AtlasRunNode node = new AtlasRunNode(start.pack(), "pirates", List.of(), List.of(RelayNetwork.ANOMALY_ID), false);
        assertSame(node, RelayChain.join(node, generator, state, start));

        insertAtStart(null);
        AtlasRunNode first = RelayChain.join(node, generator, state, start);
        assertEquals(List.of(), first.effects());
        assertFalse(first.guaranteedBossUnique());
    }
}
