package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasHash;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.turn.Intent;
import exiledsector.charts.officer.turn.IntentKind;
import exiledsector.charts.officer.turn.OfficerTurns;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SkirmishContests {

    static final long ORDERED_ATTACKER_SALT = 0x4f5244415454L;

    private SkirmishContests() {
    }

    public static SkirmishContest at(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        SkirmishContest declared = declaredAt(generator, state, cell);
        return declared != null ? declared : orderedAt(generator, state, cell);
    }

    public static SkirmishContest declaredAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
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
        candidates.removeIf(cell -> declaredAt(generator, state, cell) == null);
        return candidates;
    }

    static SkirmishContest orderedAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        AtlasOfficer defender = defenderAt(generator, state, cell);
        AtlasAnomalyRecord ordered = defender == null ? null : orderedRecord(state, cell);
        if (ordered == null) {
            return null;
        }
        String raced = AtlasHash.race(state.seed(), ORDERED_ATTACKER_SALT + ordered.recordId(), cell.gridX(), cell.gridY(),
                lanesFromAttackers(generator, state, cell, defender));
        AtlasOfficer attacker = raced == null ? null : state.officer(Integer.parseInt(raced));
        return attacker == null ? null : new SkirmishContest(Intent.push(attacker.recordId(), cell, defender.recordId()), attacker, defender);
    }

    public static boolean canBeAttacked(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        AtlasOfficer defender = defenderAt(generator, state, cell);
        return defender != null && (declaredAt(generator, state, cell) != null || !lanesFromAttackers(generator, state, cell, defender).isEmpty());
    }

    public static AtlasOfficer defenderAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        AtlasCell centre = state.commandCentreOf(cell);
        if (centre == null || centre.equals(cell)) {
            return null;
        }
        AtlasOfficer defender = state.officerAt(centre);
        if (defender == null || !defender.isActive() || !SkirmishFactions.takesSides(defender.factionId())
                || !defender.factionId().equals(AtlasOpening.ownerOf(generator, state, cell))) {
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
        boolean possible = intent.kind() == IntentKind.CHALLENGE ? canChallenge(attacker, defender)
                : canAttack(generator, state, attacker, defender, cell);
        return possible ? new SkirmishContest(intent, attacker, defender) : null;
    }

    private static boolean canAttack(AtlasGenerator generator, AtlasState state, AtlasOfficer attacker, AtlasOfficer defender, AtlasCell cell) {
        return canChallenge(attacker, defender) && borders(generator, state, attacker.commandCentre(), cell);
    }

    private static boolean canChallenge(AtlasOfficer attacker, AtlasOfficer defender) {
        return attacker != null && attacker.isActive() && SkirmishFactions.takesSides(attacker.factionId())
                && !attacker.factionId().equals(defender.factionId());
    }

    private static Map<String, Float> lanesFromAttackers(AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasOfficer defender) {
        Map<String, Float> lanesInto = new LinkedHashMap<>();
        for (AtlasCell neighbour : generator.lanes(cell)) {
            AtlasCell centre = state.commandCentreOf(neighbour);
            AtlasOfficer neighbouring = centre == null ? null : state.officerAt(centre);
            if (neighbouring != null && canAttack(generator, state, neighbouring, defender, cell)) {
                lanesInto.merge(Integer.toString(neighbouring.recordId()), 1f, Float::sum);
            }
        }
        return lanesInto;
    }

    private static AtlasAnomalyRecord orderedRecord(AtlasState state, AtlasCell cell) {
        for (AtlasAnomalyRecord record : state.anomalyRecords(cell)) {
            if (record.source() == AtlasAnomalyRecord.Source.PLAYER && FactionSkirmish.ANOMALY_ID.equals(record.anomalyId())) {
                return record;
            }
        }
        return null;
    }

    private static boolean pushedAt(List<Intent> declared, AtlasCell cell) {
        return declared.stream().anyMatch(intent -> intent.kind() == IntentKind.PUSH && cell.equals(intent.target()));
    }

    private static boolean borders(AtlasGenerator generator, AtlasState state, AtlasCell commandCentre, AtlasCell cell) {
        for (AtlasCell lane : generator.lanes(cell)) {
            if (commandCentre.equals(state.commandCentreOf(lane))) {
                return true;
            }
        }
        return false;
    }
}
