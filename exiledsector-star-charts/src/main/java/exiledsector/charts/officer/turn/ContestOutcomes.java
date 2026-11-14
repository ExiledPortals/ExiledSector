package exiledsector.charts.officer.turn;

import exiledsector.Seeds;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTerritoryTransfer;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerRanks;

import java.util.List;

public final class ContestOutcomes {

    static final long ABSTRACT_ROLL_SALT = 0x434e5453524f4cL;

    private ContestOutcomes() {
    }

    public static boolean abstractAttackerWins(AtlasState state, Intent intent, AtlasCell contestedCell) {
        TurnSetup setup = setupWithoutOpenRun();
        AtlasOfficer attacker = state.officer(intent.officerRecordId());
        AtlasOfficer defender = state.officer(intent.targetRecordId());
        if (attacker == null || defender == null || !setup.canAct(defender)) {
            return false;
        }
        boolean defended = OfficerTurns.declaredIntents(state).contains(Intent.defend(defender.recordId()));
        float roll = Seeds.derived(state.seed(), ABSTRACT_ROLL_SALT + contestedCell.pack() * Seeds.GOLDEN_GAMMA + state.officerTurn()).nextFloat();
        return attackerWins(setup, state, intent, attacker, defender, defended, roll);
    }

    static boolean attackerWins(TurnSetup setup, AtlasState state, Intent intent, AtlasOfficer attacker, AtlasOfficer defender, boolean defended,
                                float roll) {
        Boolean decided = state.decidedContest(intent.encode());
        if (decided != null) {
            return decided;
        }
        float chance = setup.rules().attackerChancePercent(setup.rankOrder(attacker.rankId()), setup.rankOrder(defender.rankId()), defended);
        return setup.resolvers().forKind(intent.kind()).attackerWins(new Contest(intent, attacker, defender, defended, chance, roll));
    }

    private static TurnSetup setupWithoutOpenRun() {
        return new TurnSetup(OfficerTurnTables.rules(), OfficerRanks.all(), OfficerTurns.resolvers(), null, Intent.NO_OFFICER, FlagshipCatalogue.NONE);
    }

    public static boolean settle(AtlasGenerator generator, AtlasState state, Intent intent, AtlasCell contestedCell, boolean attackerWins) {
        AtlasOfficer attacker = state.officer(intent.officerRecordId());
        AtlasOfficer defender = state.officer(intent.targetRecordId());
        List<Intent> declared = OfficerTurns.declaredIntents(state);
        boolean moved = attackerWins && canTake(generator, state, attacker, defender, contestedCell)
                && AtlasTerritoryTransfer.transferTo(generator::lanes, state, contestedCell, attacker.commandCentre());
        TurnRules rules = OfficerTurnTables.rules();
        if (intent.kind() == IntentKind.PUSH) {
            if (moved || attackerWins && attacker != null && state.isCompleted(contestedCell)) {
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
        if (attacker == null || !attacker.isActive() || state.isCompleted(cell) || state.officerAt(cell) != null) {
            return false;
        }
        AtlasCell holder = state.commandCentreOf(cell);
        boolean stillContested = holder == null || defender != null && defender.isActive() && holder.equals(defender.commandCentre());
        return stillContested && AtlasTerritoryTransfer.territoryBorders(generator::lanes, state, attacker.commandCentre(), cell);
    }

    private static void addRenown(AtlasState state, AtlasOfficer officer, int gained) {
        AtlasOfficer current = state.officer(officer.recordId());
        if (current != null && gained != 0) {
            state.putOfficer(current.withRenown(current.renown() + gained));
        }
    }
}
