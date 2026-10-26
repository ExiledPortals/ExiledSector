package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.anomaly.ChartAnomaly;

import java.util.ArrayList;
import java.util.List;

public final class AtlasAnomalies {

    public enum InsertCheck {
        ALLOWED,
        UNKNOWN,
        NOT_INSERTABLE,
        MOD_MISSING,
        NO_SYSTEM,
        COMPLETED,
        OPEN_RUN,
        ALREADY_PRESENT,
        CONFLICT,
        DOES_NOT_FIT;

        public boolean allows() {
            return this == ALLOWED;
        }
    }

    public record Insertion(InsertCheck check, int recordId) {
    }

    private AtlasAnomalies() {
    }

    public static List<String> effective(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        if (cell.equals(AtlasCell.ORIGIN) || generator.isVoid(cell)) {
            return List.of();
        }
        boolean spent = state.has(cell, AtlasState.MARKERS_SPENT);
        List<String> accepted = new ArrayList<>();
        for (String anomalyId : generator.node(cell).anomalyIds()) {
            ChartAnomalies.Definition definition = ChartAnomalies.definition(anomalyId);
            boolean keptWhenSpent = definition != null && definition.replacesStar();
            if ((!spent || keptWhenSpent) && accepts(generator, state, cell, accepted, anomalyId)) {
                accepted.add(anomalyId);
            }
        }
        for (AtlasAnomalyRecord record : state.anomalyRecords(cell)) {
            if (accepts(generator, state, cell, accepted, record.anomalyId())) {
                accepted.add(record.anomalyId());
            }
        }
        return List.copyOf(accepted);
    }

    private static boolean accepts(AtlasGenerator generator, AtlasState state, AtlasCell cell, List<String> accepted, String anomalyId) {
        return ChartAnomalies.accepts(accepted, anomalyId) && ChartAnomalies.byId(anomalyId).holdsAt(generator, state, cell);
    }

    public static InsertCheck canInsert(AtlasGenerator generator, AtlasState state, AtlasCell cell, String anomalyId, AtlasCell openRunCell) {
        ChartAnomalies.Definition definition = ChartAnomalies.definition(anomalyId);
        ChartAnomaly anomaly = ChartAnomalies.byId(anomalyId);
        if (definition == null || anomaly == null) {
            return InsertCheck.UNKNOWN;
        }
        if (!definition.insertable()) {
            return InsertCheck.NOT_INSERTABLE;
        }
        if (!ChartAnomalies.usable(anomalyId)) {
            return InsertCheck.MOD_MISSING;
        }
        if (cell.equals(AtlasCell.ORIGIN) || generator.isVoid(cell)) {
            return InsertCheck.NO_SYSTEM;
        }
        if (state.isCompleted(cell)) {
            return InsertCheck.COMPLETED;
        }
        if (cell.equals(openRunCell)) {
            return InsertCheck.OPEN_RUN;
        }
        List<String> present = effective(generator, state, cell);
        if (present.contains(anomalyId)) {
            return InsertCheck.ALREADY_PRESENT;
        }
        if (present.stream().anyMatch(existing -> ChartAnomalies.conflicts(existing, anomalyId))) {
            return InsertCheck.CONFLICT;
        }
        return anomaly.fitsAt(generator.traits(cell)) ? InsertCheck.ALLOWED : InsertCheck.DOES_NOT_FIT;
    }

    public static Insertion insert(AtlasGenerator generator, AtlasState state, AtlasCell cell, String anomalyId, AtlasAnomalyRecord.Source source,
                                   AtlasCell openRunCell) {
        InsertCheck check = canInsert(generator, state, cell, anomalyId, openRunCell);
        return new Insertion(check, check.allows() ? state.insertAnomaly(cell, anomalyId, source) : 0);
    }

    public static InsertCheck move(AtlasGenerator generator, AtlasState state, int recordId, AtlasCell target, AtlasCell openRunCell) {
        AtlasAnomalyRecord record = state.anomalyRecord(recordId);
        if (record == null) {
            return InsertCheck.UNKNOWN;
        }
        if (record.cell().equals(openRunCell)) {
            return InsertCheck.OPEN_RUN;
        }
        if (record.cell().equals(target)) {
            return InsertCheck.ALREADY_PRESENT;
        }
        InsertCheck check = canInsert(generator, state, target, record.anomalyId(), openRunCell);
        if (check.allows()) {
            state.moveAnomaly(recordId, target);
        }
        return check;
    }

    public static AtlasCell openRunCell() {
        ChartRun run = ChartRuns.currentOrNull();
        AtlasRunNode node = run == null ? null : run.atlasNode();
        return node == null ? null : AtlasCell.unpack(node.cell());
    }

    public static Insertion insert(SectorAPI sector, AtlasCell cell, String anomalyId, AtlasAnomalyRecord.Source source) {
        return insert(AtlasRuntime.generator(sector), AtlasState.get(sector), cell, anomalyId, source, openRunCell());
    }

    public static InsertCheck move(SectorAPI sector, int recordId, AtlasCell target) {
        return move(AtlasRuntime.generator(sector), AtlasState.get(sector), recordId, target, openRunCell());
    }
}
