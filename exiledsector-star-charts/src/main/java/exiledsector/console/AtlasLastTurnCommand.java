package exiledsector.console;

import com.fs.starfarer.api.Global;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.turn.OfficerTurns;
import exiledsector.charts.officer.turn.TurnResult;
import exiledsector.charts.officer.turn.TurnResult.OfficerLoss;
import exiledsector.charts.officer.turn.TurnResult.Replacement;
import exiledsector.charts.officer.turn.TurnResult.ResolvedIntent;
import exiledsector.charts.officer.turn.TurnResult.StandingChange;
import org.lazywizard.console.Console;

import java.util.Locale;

public class AtlasLastTurnCommand extends CampaignCommand {

    @Override
    protected CommandResult runInCampaign(String args) {
        AtlasState state = AtlasState.get(Global.getSector());
        Console.showMessage(describe(state, OfficerTurns.lastTurn(state)));
        return CommandResult.SUCCESS;
    }

    static String describe(AtlasState state, TurnResult result) {
        if (result.turn() == 0) {
            return "No officer turn has been taken yet.";
        }
        StringBuilder text = new StringBuilder("Officer turn " + result.turn() + ", caused by " + words(result.cause().kind()));
        if (result.cause().officerRecordId() != 0) {
            text.append(" (").append(AtlasIntentsCommand.officer(state, result.cause().officerRecordId())).append(")");
        }
        for (ResolvedIntent intent : result.resolvedIntents()) {
            text.append("\n").append(AtlasIntentsCommand.officer(state, intent.attackerRecordId())).append(" ").append(intent.kind().id());
            if (intent.target() != null) {
                text.append(" ").append(AtlasCommandText.cell(intent.target()));
            }
            if (intent.defenderRecordId() != 0) {
                text.append(" against ").append(AtlasIntentsCommand.officer(state, intent.defenderRecordId()))
                        .append(intent.defended() ? " (defended)" : "");
            }
            text.append(intent.succeeded() ? ": succeeded" : ": failed");
        }
        for (OfficerLoss loss : result.losses()) {
            text.append("\nLoss: #").append(loss.recordId()).append(" ").append(words(loss.kind()))
                    .append(loss.fled() ? ", fled" : "").append(loss.died() ? ", died" : "");
        }
        for (Replacement replacement : result.replacements()) {
            text.append("\nReplaced #").append(replacement.fallenRecordId()).append(" with ")
                    .append(AtlasIntentsCommand.officer(state, replacement.replacementRecordId()));
        }
        for (StandingChange change : result.rankChanges()) {
            text.append("\nRank: #").append(change.recordId()).append(" ").append(change.before().rankId()).append(" -> ")
                    .append(change.after().rankId());
        }
        return text.toString();
    }

    private static String words(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
