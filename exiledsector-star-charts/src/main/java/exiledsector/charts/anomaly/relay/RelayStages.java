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

public final class RelayStages {

    public static final String STAGE_EFFECTS_ID = "relay_stage";

    private RelayStages() {
    }

    public static AtlasAnomalyRecord recordAt(AtlasState state, AtlasCell cell) {
        for (AtlasAnomalyRecord record : state.anomalyRecords(cell)) {
            if (RelayNetwork.ANOMALY_ID.equals(record.anomalyId())) {
                return record;
            }
        }
        return null;
    }

    public static AtlasAnomalyRecord.Chain stageAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return stageAt(generator, state, cell, AtlasAnomalies.effective(generator, state, cell));
    }

    private static AtlasAnomalyRecord.Chain stageAt(AtlasGenerator generator, AtlasState state, AtlasCell cell, List<String> anomalyIds) {
        if (!anomalyIds.contains(RelayNetwork.ANOMALY_ID)) {
            return null;
        }
        return chainAt(generator, state, cell, recordAt(state, cell), anomalyIds);
    }

    public static AtlasAnomalyRecord.Chain liveStage(SectorAPI sector, ChartRun run) {
        AtlasRunNode node = run == null ? null : run.atlasNode();
        if (node == null || sector == null || !run.anomalyIds().contains(RelayNetwork.ANOMALY_ID)) {
            return null;
        }
        AtlasGenerator generator = AtlasRuntime.generator(sector);
        AtlasState state = AtlasState.get(sector);
        AtlasCell cell = AtlasCell.unpack(node.cell());
        return chainAt(generator, state, cell, recordAt(state, cell), node.anomalyIds());
    }

    private static AtlasAnomalyRecord.Chain chainAt(AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasAnomalyRecord record,
                                                    List<String> anomalyIds) {
        if (record != null && record.chain() != null) {
            return record.chain();
        }
        return new AtlasAnomalyRecord.Chain(1, RelayRules.STAGES + atlasExtraStages(generator, state, cell, anomalyIds), null);
    }

    static int atlasExtraStages(AtlasGenerator generator, AtlasState state, AtlasCell cell, List<String> anomalyIds) {
        List<ChartGenContext.AppliedEffect> effects = new ArrayList<>(AtlasOpening.runNode(generator, state, cell).effects());
        effects.addAll(ChartAnomalies.effectsOf(anomalyIds));
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
        AtlasAnomalyRecord.Chain stage = stageAt(generator, state, cell, node.anomalyIds());
        if (stage == null) {
            return node;
        }
        return node.withExtraEffects(stageEffects(stage.stage()));
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
        return lines;
    }

    public static void closed(SectorAPI sector, ChartRun run) {
        AtlasRunNode node = run.atlasNode();
        if (node == null || !RelayStates.markSettled(run)) {
            return;
        }
        settle(AtlasRuntime.generator(sector), AtlasState.get(sector), AtlasCell.unpack(node.cell()), node.anomalyIds(),
                run.anomalyIds().contains(RelayNetwork.ANOMALY_ID), RelayStates.allWoken(run), RelayTuning.onwardSystems(RelayTuning.of(run)));
    }

    static AtlasCell settle(AtlasGenerator generator, AtlasState state, AtlasCell cell, boolean placed, boolean allWoken) {
        List<AtlasCell> targets = settle(generator, state, cell, AtlasAnomalies.effective(generator, state, cell), placed, allWoken, 1);
        return targets.isEmpty() ? null : targets.get(0);
    }

    static List<AtlasCell> settle(AtlasGenerator generator, AtlasState state, AtlasCell cell, List<String> runAnomalyIds, boolean placed,
                                  boolean allWoken, int onwardSystems) {
        AtlasAnomalyRecord record = recordAt(state, cell);
        AtlasAnomalyRecord.Chain current = chainAt(generator, state, cell, record, runAnomalyIds);
        AtlasAnomalyRecord.Chain next = null;
        if (!placed) {
            next = new AtlasAnomalyRecord.Chain(current.stage(), current.stages(), cell);
        } else if (allWoken && !current.isLast()) {
            next = new AtlasAnomalyRecord.Chain(current.stage() + 1, current.stages(), cell);
        }
        AtlasCell target = moveOn(generator, state, cell, record, next);
        if (target == null) {
            return List.of();
        }
        List<AtlasCell> targets = new ArrayList<>(List.of(target));
        int branches = placed ? onwardSystems : 1;
        for (int branch = 1; branch < branches; branch++) {
            AtlasCell fork = fork(generator, state, cell, next);
            if (fork == null) {
                break;
            }
            targets.add(fork);
        }
        return List.copyOf(targets);
    }

    static AtlasCell fork(AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasAnomalyRecord.Chain next) {
        AtlasCell target = onward(generator, state, cell);
        boolean inserted = target != null
                && AtlasAnomalies.insert(generator, state, target, RelayNetwork.ANOMALY_ID, AtlasAnomalyRecord.Source.WORLD, null, next).check().allows();
        return inserted ? target : null;
    }

    private static AtlasCell onward(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return AtlasAnomalies.onward(generator, state, cell, RelayNetwork.ANOMALY_ID, RelayRules.MIN_ONWARD_LANES, RelayRules.MAX_ONWARD_LANES);
    }

    private static AtlasCell moveOn(AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasAnomalyRecord record, AtlasAnomalyRecord.Chain next) {
        AtlasCell target = next == null ? null : onward(generator, state, cell);
        boolean moved = target != null && (record != null
                ? AtlasAnomalies.move(generator, state, record.recordId(), target, null, next).allows()
                : AtlasAnomalies.insert(generator, state, target, RelayNetwork.ANOMALY_ID, AtlasAnomalyRecord.Source.WORLD, null, next).check().allows());
        if (!moved && record != null) {
            state.removeAnomaly(record.recordId());
        }
        return moved ? target : null;
    }
}
