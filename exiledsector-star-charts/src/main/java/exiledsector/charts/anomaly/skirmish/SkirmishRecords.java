package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.turn.OfficerTurnListener;
import exiledsector.charts.officer.turn.OfficerTurns;
import exiledsector.charts.officer.turn.TurnResult;

import java.util.Set;

public final class SkirmishRecords implements OfficerTurnListener {

    public static final SkirmishRecords INSTANCE = new SkirmishRecords();

    private SkirmishRecords() {
    }

    public static void register() {
        OfficerTurns.removeListener(INSTANCE);
        OfficerTurns.addListener(INSTANCE);
    }

    @Override
    public void turnTaken(AtlasGenerator generator, AtlasState state, TurnResult result, AtlasCell openRunCell) {
        sync(generator, state, openRunCell);
    }

    static void sync(AtlasGenerator generator, AtlasState state, AtlasCell openRunCell) {
        Set<AtlasCell> contested = SkirmishContests.declaredCells(generator, state);
        for (AtlasAnomalyRecord record : state.anomalyRecords()) {
            if (record.source() == AtlasAnomalyRecord.Source.WORLD && FactionSkirmish.ANOMALY_ID.equals(record.anomalyId())
                    && !record.cell().equals(openRunCell) && !contested.contains(record.cell())) {
                state.removeAnomaly(record.recordId());
            }
        }
        for (AtlasCell cell : contested) {
            AtlasAnomalies.insert(generator, state, cell, FactionSkirmish.ANOMALY_ID, AtlasAnomalyRecord.Source.WORLD, openRunCell);
        }
    }
}
