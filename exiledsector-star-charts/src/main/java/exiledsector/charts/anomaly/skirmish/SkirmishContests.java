package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTerritoryTransfer;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.turn.Intent;
import exiledsector.charts.officer.turn.IntentKind;
import exiledsector.charts.officer.turn.OfficerTurns;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SkirmishContests {

    private SkirmishContests() {
    }

    public static SkirmishContest at(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        AtlasOfficer defender = defenderAt(generator, state, cell);
        if (defender == null) {
            return null;
        }
        List<Intent> declared = OfficerTurns.declaredIntents(state);
        for (Intent intent : declared) {
            if (intent.kind() == IntentKind.PUSH && cell.equals(intent.target()) && intent.targetRecordId() == defender.recordId()) {
                SkirmishContest contest = contest(generator, state, intent, defender, cell);
                if (contest != null) {
                    return contest;
                }
            }
        }
        for (Intent intent : declared) {
            if (intent.kind() == IntentKind.CHALLENGE && intent.targetRecordId() == defender.recordId()) {
                AtlasOfficer attacker = state.officer(intent.officerRecordId());
                if (attacker != null && cell.equals(challengeCell(generator, state, attacker, defender, declared))) {
                    SkirmishContest contest = contest(generator, state, intent, defender, cell);
                    if (contest != null) {
                        return contest;
                    }
                }
            }
        }
        return null;
    }

    public static Set<AtlasCell> declaredCells(AtlasGenerator generator, AtlasState state) {
        List<Intent> declared = OfficerTurns.declaredIntents(state);
        Set<AtlasCell> candidates = new LinkedHashSet<>();
        for (Intent intent : declared) {
            AtlasOfficer attacker = state.officer(intent.officerRecordId());
            AtlasOfficer defender = state.officer(intent.targetRecordId());
            if (intent.kind() == IntentKind.PUSH && intent.target() != null) {
                candidates.add(intent.target());
            } else if (intent.kind() == IntentKind.CHALLENGE && attacker != null && defender != null) {
                AtlasCell challenged = challengeCell(generator, state, attacker, defender, declared);
                if (challenged != null) {
                    candidates.add(challenged);
                }
            }
        }
        candidates.removeIf(cell -> at(generator, state, cell) == null);
        return candidates;
    }

    public static AtlasCell battleCell(AtlasGenerator generator, AtlasState state, Intent challenge) {
        AtlasOfficer attacker = state.officer(challenge.officerRecordId());
        AtlasOfficer defender = state.officer(challenge.targetRecordId());
        if (challenge.kind() != IntentKind.CHALLENGE || attacker == null || defender == null) {
            return null;
        }
        AtlasCell cell = challengeCell(generator, state, attacker, defender, OfficerTurns.declaredIntents(state));
        return cell != null && at(generator, state, cell) != null ? cell : null;
    }

    public static AtlasOfficer defenderAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        AtlasCell centre = state.commandCentreOf(cell);
        if (centre == null || centre.equals(cell)) {
            return null;
        }
        AtlasOfficer defender = state.officerAt(centre);
        if (defender == null || !defender.isActive() || !defender.factionId().equals(AtlasOpening.ownerOf(generator, state, cell))) {
            return null;
        }
        return defender;
    }

    static AtlasCell challengeCell(AtlasGenerator generator, AtlasState state, AtlasOfficer attacker, AtlasOfficer defender, List<Intent> declared) {
        AtlasCell nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        for (AtlasCell claim : state.claimsOf(defender.commandCentre())) {
            long distance = squaredDistance(attacker.commandCentre(), claim);
            if (distance < nearestDistance && state.officerAt(claim) == null && generator.node(claim).specialId() == null
                    && !pushedAt(declared, claim)) {
                nearest = claim;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static long squaredDistance(AtlasCell from, AtlasCell to) {
        if (from == null) {
            return 0;
        }
        long dx = (long) from.gridX() - to.gridX();
        long dy = (long) from.gridY() - to.gridY();
        return dx * dx + dy * dy;
    }

    private static SkirmishContest contest(AtlasGenerator generator, AtlasState state, Intent intent, AtlasOfficer defender, AtlasCell cell) {
        AtlasOfficer attacker = state.officer(intent.officerRecordId());
        boolean possible = intent.kind() == IntentKind.CHALLENGE ? canChallenge(attacker)
                : canAttack(generator, state, attacker, defender, cell);
        return possible ? new SkirmishContest(intent, attacker, defender) : null;
    }

    private static boolean canAttack(AtlasGenerator generator, AtlasState state, AtlasOfficer attacker, AtlasOfficer defender, AtlasCell cell) {
        return canChallenge(attacker) && !attacker.factionId().equals(defender.factionId())
                && AtlasTerritoryTransfer.territoryBorders(generator::lanes, state, attacker.commandCentre(), cell);
    }

    private static boolean canChallenge(AtlasOfficer attacker) {
        return attacker != null && attacker.isActive();
    }

    private static boolean pushedAt(List<Intent> declared, AtlasCell cell) {
        return declared.stream().anyMatch(intent -> intent.kind() == IntentKind.PUSH && cell.equals(intent.target()));
    }
}
