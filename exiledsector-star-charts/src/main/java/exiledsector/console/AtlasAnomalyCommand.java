package exiledsector.console;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AtlasAnomalyCommand extends AtlasTargetCommand {

    static final String MOVE = "move";
    static final String REMOVE = "remove";
    static final String WORLD = "world";

    @Override
    protected String usage() {
        return "ExiledSectorAtlasAnomaly <x> <y> | <site id> [<anomaly id> [world] | move <record> | remove <record>].";
    }

    @Override
    protected CommandResult runAt(SectorAPI sector, AtlasGenerator generator, AtlasState state, AtlasCell cell, List<String> rest) {
        if (rest.isEmpty()) {
            Console.showMessage(describe(generator, state, cell));
            return CommandResult.SUCCESS;
        }
        String action = rest.get(0).toLowerCase(Locale.ROOT);
        if (action.equals(MOVE) || action.equals(REMOVE)) {
            Integer recordId = rest.size() == 2 ? ConsoleArgs.wholeNumber(rest.get(1), 1, Integer.MAX_VALUE) : null;
            if (recordId == null) {
                return badSyntax("Give one record number after " + action + ".");
            }
            return action.equals(MOVE) ? move(generator, state, cell, recordId) : remove(state, cell, recordId);
        }
        if (rest.size() > 2 || rest.size() == 2 && !rest.get(1).equalsIgnoreCase(WORLD)) {
            return badSyntax(rest);
        }
        AtlasAnomalyRecord.Source source = rest.size() == 2 ? AtlasAnomalyRecord.Source.WORLD : AtlasAnomalyRecord.Source.PLAYER;
        AtlasAnomalies.Insertion insertion = AtlasAnomalies.insert(generator, state, cell, rest.get(0), source, AtlasAnomalies.openRunCell());
        if (!insertion.check().allows()) {
            Console.showMessage("Can't insert " + rest.get(0) + " into " + AtlasCommandText.cell(cell) + ": " + reason(insertion.check())
                    + ". Insertable anomalies: " + String.join(", ", insertable()) + ".");
            return CommandResult.ERROR;
        }
        Console.showMessage("Inserted " + rest.get(0) + " as record " + insertion.recordId() + ".\n" + describe(generator, state, cell));
        return CommandResult.SUCCESS;
    }

    private static CommandResult move(AtlasGenerator generator, AtlasState state, AtlasCell cell, int recordId) {
        AtlasAnomalies.InsertCheck check = AtlasAnomalies.move(generator, state, recordId, cell, AtlasAnomalies.openRunCell());
        if (!check.allows()) {
            Console.showMessage("Can't move record " + recordId + " to " + AtlasCommandText.cell(cell) + ": " + reason(check) + ".");
            return CommandResult.ERROR;
        }
        Console.showMessage("Moved record " + recordId + ".\n" + describe(generator, state, cell));
        return CommandResult.SUCCESS;
    }

    private static CommandResult remove(AtlasState state, AtlasCell cell, int recordId) {
        AtlasAnomalyRecord record = state.anomalyRecord(recordId);
        if (record == null || !record.cell().equals(cell)) {
            Console.showMessage(AtlasCommandText.cell(cell) + " has no record " + recordId + ".");
            return CommandResult.ERROR;
        }
        state.removeAnomaly(recordId);
        Console.showMessage("Removed record " + recordId + " (" + record.anomalyId() + ").");
        return CommandResult.SUCCESS;
    }

    static String describe(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        StringBuilder text = new StringBuilder(AtlasCommandText.node(generator, cell, state));
        for (AtlasAnomalyRecord record : state.anomalyRecords(cell)) {
            text.append("\n   record ").append(record.recordId()).append(": ").append(record.anomalyId()).append(" (")
                    .append(record.source().name().toLowerCase(Locale.ROOT)).append(")");
            if (record.chain() != null) {
                text.append(", stage ").append(record.chain().stage()).append(" of ").append(record.chain().stages());
            }
        }
        return text.toString();
    }

    static String reason(AtlasAnomalies.InsertCheck check) {
        return switch (check) {
            case ALLOWED -> "allowed";
            case UNKNOWN -> "no such anomaly or record";
            case NOT_INSERTABLE -> "that anomaly can't be inserted";
            case MOD_MISSING -> "the mod it needs isn't loaded";
            case NO_SYSTEM -> "there is no system there";
            case COMPLETED -> "the system is completed";
            case OPEN_RUN -> "a chart run is open there";
            case ALREADY_PRESENT -> "the system already has it";
            case FULL -> "the system already has " + AtlasAnomalies.MAX_PER_SYSTEM + " anomalies";
            case CONFLICT -> "it conflicts with an anomaly already there";
            case DOES_NOT_FIT -> "it can't exist at that system";
        };
    }

    private static List<String> insertable() {
        List<String> ids = new ArrayList<>();
        for (ChartAnomalies.Definition definition : ChartAnomalies.definitions()) {
            if (definition.insertable()) {
                ids.add(definition.id());
            }
        }
        return ids;
    }
}
