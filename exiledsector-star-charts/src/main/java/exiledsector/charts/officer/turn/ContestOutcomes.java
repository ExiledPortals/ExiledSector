package exiledsector.charts.officer.turn;

import exiledsector.Seeds;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTerritoryTransfer;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerRanks;

import java.util.List;

public final class ContestOutcomes {

    static final long ABSTRACT_ROLL_SALT = 0x434e5453524f4cL;

    private ContestOutcomes() {
    }

    public static boolean abstractAttackerWins(AtlasState state, Intent intent, AtlasCell contestedCell) {
        AtlasOfficer attacker = state.officer(intent.officerRecordId());
        AtlasOfficer defender = state.officer(intent.targetRecordId());
        if (attacker == null || defender == null) {
            return false;
        }
        boolean defended = OfficerTurns.declaredIntents(state).contains(Intent.defend(defender.recordId()));
        float chance = OfficerTurnTables.rules().attackerChancePercent(OfficerRanks.order(attacker.rankId()), OfficerRanks.order(defender.rankId()),
                defended);
        float roll = Seeds.derived(state.seed(), ABSTRACT_ROLL_SALT + contestedCell.pack() * Seeds.GOLDEN_GAMMA + state.officerTurn()).nextFloat();
        return OfficerTurns.resolvers().forKind(intent.kind()).attackerWins(new Contest(intent, attacker, defender, defended, chance, roll));
    }

    public static boolean settle(AtlasGenerator generator, AtlasState state, Intent intent, AtlasCell contestedCell, boolean attackerWins) {
        AtlasOfficer attacker = state.officer(intent.officerRecordId());
        AtlasOfficer defender = state.officer(intent.targetRecordId());
        List<Intent> declared = OfficerTurns.declaredIntents(state);
        boolean moved = attackerWins && canTake(generator, state, attacker, defender, contestedCell)
                && AtlasTerritoryTransfer.transferTo(generator::lanes, state, contestedCell, attacker.commandCentre());
        TurnRules rules = OfficerTurnTables.rules();
        if (intent.kind() == IntentKind.PUSH) {
            if (moved) {
                addRenown(state, attacker, rules.renownPerClaim());
            } else if (!attackerWins && defender != null && declared.contains(Intent.defend(defender.recordId()))) {
                addRenown(state, defender, rules.renownPerDefendHeld());
            }
            if (declared.contains(intent)) {
                state.setDeclaredIntents(declared.stream().filter(other -> !other.equals(intent)).map(Intent::encode).toList());
            }
        } else if (intent.kind() == IntentKind.CHALLENGE && declared.contains(intent)) {
            state.decideContest(intent.encode(), attackerWins);
        }
        return moved;
    }

    private static boolean canTake(AtlasGenerator generator, AtlasState state, AtlasOfficer attacker, AtlasOfficer defender, AtlasCell cell) {
        if (attacker == null || !attacker.isActive() || state.officerAt(cell) != null) {
            return false;
        }
        AtlasCell holder = state.commandCentreOf(cell);
        boolean stillContested = holder == null || defender != null && defender.isActive() && holder.equals(defender.commandCentre());
        return stillContested && borders(generator, state, attacker.commandCentre(), cell);
    }

    private static boolean borders(AtlasGenerator generator, AtlasState state, AtlasCell commandCentre, AtlasCell cell) {
        for (AtlasCell lane : generator.lanes(cell)) {
            if (commandCentre.equals(state.commandCentreOf(lane))) {
                return true;
            }
        }
        return false;
    }

    private static void addRenown(AtlasState state, AtlasOfficer officer, int gained) {
        AtlasOfficer current = state.officer(officer.recordId());
        if (current != null && gained != 0) {
            state.putOfficer(current.withRenown(current.renown() + gained));
        }
    }
}
