package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;

import java.util.ArrayList;
import java.util.List;

public final class RelayChain {

    public static final String STAGE_EFFECTS_ID = "relay_stage";

    private RelayChain() {
    }

    public static AtlasAnomalyRecord recordAt(AtlasState state, AtlasCell cell) {
        for (AtlasAnomalyRecord record : state.anomalyRecords(cell)) {
            if (RelayNetwork.ID.equals(record.anomalyId())) {
                return record;
            }
        }
        return null;
    }

    public static int rolledStages(AtlasState state, AtlasCell cell) {
        int spread = RelayRules.MAX_STAGES - RelayRules.MIN_STAGES + 1;
        return RelayRules.MIN_STAGES + Math.min(spread - 1, (int) (AtlasAnomalies.roll(state, cell, RelayNetwork.ID) * spread));
    }

    public static AtlasAnomalyRecord.Chain stageAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        if (!AtlasAnomalies.effective(generator, state, cell).contains(RelayNetwork.ID)) {
            return null;
        }
        AtlasAnomalyRecord record = recordAt(state, cell);
        return record != null && record.chain() != null ? record.chain() : new AtlasAnomalyRecord.Chain(1, rolledStages(state, cell), null);
    }

    public static List<ChartGenContext.AppliedEffect> stageEffects(int stage) {
        int steps = stage - 1;
        if (steps <= 0) {
            return List.of();
        }
        List<ChartGenContext.AppliedEffect> lines = new ArrayList<>();
        for (ChartGenContext.AppliedEffect line : AtlasData.nodeEffects(STAGE_EFFECTS_ID)) {
            lines.add(new ChartGenContext.AppliedEffect(line.effect(), line.value() * steps));
        }
        return lines;
    }

    public static int extraStages(ChartGenContext context) {
        return context == null ? 0 : Math.max(0, Math.round(context.anomalyPercent(RelayNetwork.STAGES_KEY)));
    }

    public static AtlasRunNode join(AtlasRunNode node, AtlasGenerator generator, AtlasState state, AtlasCell cell, int extraStages) {
        if (node == null || !node.anomalyIds().contains(RelayNetwork.ID)) {
            return node;
        }
        AtlasAnomalyRecord.Chain stage = stageAt(generator, state, cell);
        if (stage == null) {
            return node;
        }
        return node.withExtraEffects(stageEffects(stage.stage()), stage.stage() >= stage.stages() + Math.max(0, extraStages));
    }

    public static void closed(SectorAPI sector, ChartRun run) {
        AtlasRunNode node = run == null ? null : run.atlasNode();
        if (sector == null || node == null || !node.anomalyIds().contains(RelayNetwork.ID)) {
            return;
        }
        int extraStages = extraStages(ChartRunEffects.contextOf(run));
        settle(AtlasRuntime.generator(sector), AtlasState.get(sector), AtlasCell.unpack(node.cell()), run.isObjectiveComplete(),
                RelayStates.allWoken(run), extraStages);
    }

    static AtlasCell settle(AtlasGenerator generator, AtlasState state, AtlasCell cell, boolean completed, boolean allWoken, int extraStages) {
        AtlasAnomalyRecord record = recordAt(state, cell);
        AtlasAnomalyRecord.Chain current = record != null && record.chain() != null ? record.chain()
                : new AtlasAnomalyRecord.Chain(1, rolledStages(state, cell), null);
        int stages = current.stages() + Math.max(0, extraStages);
        AtlasCell target = completed && allWoken && current.stage() < stages
                ? AtlasAnomalies.onward(generator, state, cell, RelayNetwork.ID, RelayRules.MIN_ONWARD_LANES, RelayRules.MAX_ONWARD_LANES) : null;
        if (target == null) {
            if (record != null) {
                state.removeAnomaly(record.recordId());
            }
            return null;
        }
        AtlasAnomalyRecord.Chain next = new AtlasAnomalyRecord.Chain(current.stage() + 1, stages, cell);
        boolean moved = record != null
                ? AtlasAnomalies.move(generator, state, record.recordId(), target, null, next).allows()
                : AtlasAnomalies.insert(generator, state, target, RelayNetwork.ID, AtlasAnomalyRecord.Source.WORLD, null, next).check().allows();
        if (!moved && record != null) {
            state.removeAnomaly(record.recordId());
        }
        return moved ? target : null;
    }
}
