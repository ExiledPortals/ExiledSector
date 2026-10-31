package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.List;

public final class RelayChain {

    public static final String STAGE_EFFECTS_ID = "relay_stage";

    private RelayChain() {
    }

    public static AtlasAnomalyRecord recordAt(AtlasState state, AtlasCell cell) {
        for (AtlasAnomalyRecord record : state.anomalyRecords(cell)) {
            if (RelayNetwork.ANOMALY_ID.equals(record.anomalyId())) {
                return record;
            }
        }
        return null;
    }

    public static int rolledStages(AtlasState state, AtlasCell cell) {
        int spread = RelayRules.MAX_STAGES - RelayRules.MIN_STAGES + 1;
        return RelayRules.MIN_STAGES + Math.min(spread - 1, (int) (AtlasAnomalies.roll(state, cell, RelayNetwork.ANOMALY_ID) * spread));
    }

    public static AtlasAnomalyRecord.Chain stageAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        if (!AtlasAnomalies.effective(generator, state, cell).contains(RelayNetwork.ANOMALY_ID)) {
            return null;
        }
        return chainAt(generator, state, cell, recordAt(state, cell));
    }

    private static AtlasAnomalyRecord.Chain chainAt(AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasAnomalyRecord record) {
        if (record != null && record.chain() != null) {
            return record.chain();
        }
        return new AtlasAnomalyRecord.Chain(1, rolledStages(state, cell) + atlasExtraStages(generator, state, cell), null);
    }

    static int atlasExtraStages(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        List<ChartGenContext.AppliedEffect> effects = new ArrayList<>(AtlasOpening.runNode(generator, state, cell).effects());
        effects.addAll(ChartAnomalies.effectsOf(AtlasAnomalies.effective(generator, state, cell)));
        return extraStages(ChartGenContext.of(effects));
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

    public static AtlasRunNode join(AtlasRunNode node, AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        AtlasAnomalyRecord.Chain stage = stageAt(generator, state, cell);
        if (stage == null) {
            return node;
        }
        return node.withExtraEffects(stageEffects(stage.stage()), stage.isLast());
    }

    public static List<AtlasLine> lines(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        AtlasAnomalyRecord.Chain stage = stageAt(generator, state, cell);
        if (stage == null) {
            return List.of();
        }
        List<AtlasLine> lines = new ArrayList<>();
        lines.add(new AtlasLine(Translation.msg("chart.atlas.node.relayStage").arg("stage", stage.stage()).arg("stages", stage.stages()).text(),
                AtlasLine.Tone.PLAIN));
        for (ChartGenContext.AppliedEffect applied : stageEffects(stage.stage())) {
            lines.add(new AtlasLine(applied.effect().description(applied.value()).toMarkup(), AtlasLine.Tone.STYLED));
        }
        if (stage.isLast()) {
            lines.add(new AtlasLine(Translation.text("chart.atlas.node.relayLast"), AtlasLine.Tone.GOOD));
        }
        return lines;
    }

    public static void closed(SectorAPI sector, ChartRun run) {
        AtlasRunNode node = run.atlasNode();
        if (!RelayStates.markSettled(run)) {
            return;
        }
        settle(AtlasRuntime.generator(sector), AtlasState.get(sector), AtlasCell.unpack(node.cell()), run.isObjectiveComplete(),
                run.anomalyIds().contains(RelayNetwork.ANOMALY_ID), RelayStates.allWoken(run));
    }

    static AtlasCell settle(AtlasGenerator generator, AtlasState state, AtlasCell cell, boolean completed, boolean placed, boolean allWoken) {
        AtlasAnomalyRecord record = recordAt(state, cell);
        AtlasAnomalyRecord.Chain current = chainAt(generator, state, cell, record);
        AtlasAnomalyRecord.Chain next = null;
        if (completed && !placed) {
            next = new AtlasAnomalyRecord.Chain(current.stage(), current.stages(), cell);
        } else if (completed && allWoken && !current.isLast()) {
            next = new AtlasAnomalyRecord.Chain(current.stage() + 1, current.stages(), cell);
        }
        AtlasCell target = next == null ? null
                : AtlasAnomalies.onward(generator, state, cell, RelayNetwork.ANOMALY_ID, RelayRules.MIN_ONWARD_LANES, RelayRules.MAX_ONWARD_LANES);
        boolean moved = target != null && (record != null
                ? AtlasAnomalies.move(generator, state, record.recordId(), target, null, next).allows()
                : AtlasAnomalies.insert(generator, state, target, RelayNetwork.ANOMALY_ID, AtlasAnomalyRecord.Source.WORLD, null, next).check().allows());
        if (!moved && record != null) {
            state.removeAnomaly(record.recordId());
        }
        return moved ? target : null;
    }
}
