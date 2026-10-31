package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.anomaly.AtlasRule;
import exiledsector.charts.anomaly.ChartAnomaly;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AtlasAnomalies {

    static final long ONWARD_SALT = 0x4f4e5741524453L;
    static final long ROLL_SALT = 0x414e4f4d524f4cL;
    public static final int MAX_PER_SYSTEM = 9;

    public enum InsertCheck {
        ALLOWED,
        UNKNOWN,
        NOT_INSERTABLE,
        MOD_MISSING,
        NO_SYSTEM,
        COMPLETED,
        OPEN_RUN,
        ALREADY_PRESENT,
        FULL,
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
            boolean keptWhenSpent = definition != null && definition.keptWhenSpent();
            if (accepted.size() < MAX_PER_SYSTEM && (!spent || keptWhenSpent) && accepts(generator, state, cell, accepted, anomalyId)) {
                accepted.add(anomalyId);
            }
        }
        for (AtlasAnomalyRecord record : state.anomalyRecords(cell)) {
            if (accepted.size() < MAX_PER_SYSTEM && accepts(generator, state, cell, accepted, record.anomalyId())) {
                accepted.add(record.anomalyId());
            }
        }
        return List.copyOf(accepted);
    }

    private static boolean accepts(AtlasGenerator generator, AtlasState state, AtlasCell cell, List<String> accepted, String anomalyId) {
        return ChartAnomalies.accepts(accepted, anomalyId) && AtlasRule.of(ChartAnomalies.byId(anomalyId)).holdsAt(generator, state, cell);
    }

    public static InsertCheck canInsert(AtlasGenerator generator, AtlasState state, AtlasCell cell, String anomalyId, AtlasCell openRunCell) {
        return canInsert(generator, state, cell, anomalyId, openRunCell, false);
    }

    public static InsertCheck canInsertStar(AtlasGenerator generator, AtlasState state, AtlasCell cell, String anomalyId, AtlasCell openRunCell) {
        return canInsert(generator, state, cell, anomalyId, openRunCell, true);
    }

    private static InsertCheck canInsert(AtlasGenerator generator, AtlasState state, AtlasCell cell, String anomalyId, AtlasCell openRunCell,
                                         boolean forcingStar) {
        ChartAnomalies.Definition definition = ChartAnomalies.definition(anomalyId);
        ChartAnomaly anomaly = ChartAnomalies.byId(anomalyId);
        if (definition == null || anomaly == null) {
            return InsertCheck.UNKNOWN;
        }
        boolean forcedStar = forcingStar && definition.replacesStar() && !definition.perBiome();
        if (!definition.insertable() && !forcedStar) {
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
        if (present.size() >= MAX_PER_SYSTEM) {
            return InsertCheck.FULL;
        }
        if (present.stream().anyMatch(existing -> ChartAnomalies.conflicts(existing, anomalyId))) {
            return InsertCheck.CONFLICT;
        }
        return AtlasRule.of(anomaly).insertVerdict(generator, state, cell).allowed() ? InsertCheck.ALLOWED : InsertCheck.DOES_NOT_FIT;
    }

    public static Insertion insert(AtlasGenerator generator, AtlasState state, AtlasCell cell, String anomalyId, AtlasAnomalyRecord.Source source,
                                   AtlasCell openRunCell) {
        return insert(generator, state, cell, anomalyId, source, openRunCell, null);
    }

    public static Insertion insert(AtlasGenerator generator, AtlasState state, AtlasCell cell, String anomalyId, AtlasAnomalyRecord.Source source,
                                   AtlasCell openRunCell, AtlasAnomalyRecord.Chain chain) {
        InsertCheck check = canInsert(generator, state, cell, anomalyId, openRunCell);
        return new Insertion(check, check.allows() ? state.insertAnomaly(cell, anomalyId, source, chain) : 0);
    }

    public static Insertion insertStar(AtlasGenerator generator, AtlasState state, AtlasCell cell, String anomalyId, AtlasAnomalyRecord.Source source,
                                       AtlasCell openRunCell) {
        InsertCheck check = canInsertStar(generator, state, cell, anomalyId, openRunCell);
        return new Insertion(check, check.allows() ? state.insertAnomaly(cell, anomalyId, source) : 0);
    }

    public static InsertCheck move(AtlasGenerator generator, AtlasState state, int recordId, AtlasCell target, AtlasCell openRunCell) {
        AtlasAnomalyRecord record = state.anomalyRecord(recordId);
        return move(generator, state, recordId, target, openRunCell, record == null ? null : record.chain());
    }

    public static InsertCheck move(AtlasGenerator generator, AtlasState state, int recordId, AtlasCell target, AtlasCell openRunCell,
                                   AtlasAnomalyRecord.Chain chain) {
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
            state.moveAnomaly(recordId, target, chain);
        }
        return check;
    }

    public static AtlasCell onward(AtlasGenerator generator, AtlasState state, AtlasCell from, String anomalyId, int minLanes, int maxLanes) {
        Map<AtlasCell, Integer> lanesAway = new HashMap<>();
        ArrayDeque<AtlasCell> frontier = new ArrayDeque<>();
        lanesAway.put(from, 0);
        frontier.add(from);
        AtlasCell best = null;
        double bestRoll = Double.POSITIVE_INFINITY;
        while (!frontier.isEmpty()) {
            AtlasCell cell = frontier.poll();
            int lanes = lanesAway.get(cell) + 1;
            if (lanes > maxLanes) {
                continue;
            }
            for (AtlasCell neighbour : generator.lanes(cell)) {
                if (lanesAway.containsKey(neighbour) || generator.isVoid(neighbour) || state.isCompleted(neighbour)) {
                    continue;
                }
                lanesAway.put(neighbour, lanes);
                frontier.add(neighbour);
                if (lanes < minLanes || neighbour.centreDistance() <= from.centreDistance()
                        || !canInsert(generator, state, neighbour, anomalyId, null).allows()) {
                    continue;
                }
                double roll = AtlasHash.unit(AtlasHash.of(state.seed(), ONWARD_SALT ^ from.pack(), anomalyId, neighbour.gridX(), neighbour.gridY()));
                if (roll < bestRoll) {
                    bestRoll = roll;
                    best = neighbour;
                }
            }
        }
        return best;
    }

    public static double roll(AtlasState state, AtlasCell cell, String anomalyId) {
        return AtlasHash.unit(AtlasHash.of(state.seed(), ROLL_SALT, anomalyId, cell.gridX(), cell.gridY()));
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
