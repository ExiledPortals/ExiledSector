package exiledsector.harness;

import com.fs.starfarer.api.Global;
import exiledsector.charts.anomaly.skirmish.SkirmishContests;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.TerritoryContext;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerRank;
import exiledsector.charts.officer.OfficerRanks;
import exiledsector.charts.officer.OfficerSeats;
import exiledsector.charts.officer.turn.Intent;
import exiledsector.charts.officer.turn.IntentKind;
import exiledsector.charts.officer.turn.OfficerTurns;
import exiledsector.charts.officer.turn.TurnCause;
import exiledsector.charts.officer.turn.TurnResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class OfficerTurnsStep implements HarnessStep {

    static final int DEFAULT_TURNS = 1;
    static final int RENOWN_LEAD = 25;

    private final int turns;
    private final int moves;
    private final boolean skirmish;

    OfficerTurnsStep(int turns, int moves, boolean skirmish) {
        if (turns < 1) {
            throw new IllegalArgumentException("officerTurns needs at least 1 turn, not " + turns);
        }
        if (moves < 0) {
            throw new IllegalArgumentException("officerTurns needs at least 0 moves, not " + moves);
        }
        this.turns = turns;
        this.moves = moves;
        this.skirmish = skirmish;
    }

    @Override
    public String name() {
        return "officerTurns " + turns + (moves > 0 ? ", the last moving at least " + moves + " officers" : "")
                + (skirmish ? ", ending with a declared skirmish" : "");
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("officerTurns");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        AtlasGenerator generator = AtlasRuntime.generator(Global.getSector());
        AtlasState state = AtlasState.get(Global.getSector());
        for (int taken = 1; taken < turns; taken++) {
            OfficerTurns.take(generator, state, TurnCause.forced(), TerritoryContext.current());
        }
        int risen = raiseSubordinates(state, (moves + 1) / 2);
        OfficerTurns.take(generator, state, TurnCause.forced(), TerritoryContext.current());
        String turnsTaken = "took " + turns + " officer turns, the last moving " + movedOfficers(state) + " officers"
                + (risen > 0 ? " after " + risen + " subordinates outgrew their superiors' renown" : "");
        if (movedOfficers(state) < moves) {
            return StepOutcome.failed(turnsTaken + ", fewer than " + moves + "; only " + state.officers().stream().filter(AtlasOfficer::heldCommand).count()
                    + " officers have held a command, so reveal more of the atlas first");
        }
        if (!skirmish || skirmishDeclared(state)) {
            return StepOutcome.passed(turnsTaken + (skirmishDeclared(state) ? " and declaring a skirmish" : ""));
        }
        Intent challenge = possibleChallenge(generator, state);
        if (challenge == null) {
            return StepOutcome.failed(turnsTaken + ", but no free superior and subordinate pair can skirmish");
        }
        List<String> declared = new ArrayList<>(state.declaredIntents());
        declared.add(challenge.encode());
        state.setDeclaredIntents(declared);
        return StepOutcome.passed(turnsTaken + "; declared a skirmish of officer " + challenge.officerRecordId()
                + " against officer " + challenge.targetRecordId());
    }

    private static int raiseSubordinates(AtlasState state, int wanted) {
        List<OfficerRank> ranks = OfficerRanks.all();
        Set<Integer> paired = new HashSet<>();
        int risen = 0;
        for (AtlasOfficer superior : state.officers()) {
            if (risen >= wanted || !superior.heldCommand() || paired.contains(superior.recordId())) {
                continue;
            }
            List<AtlasOfficer> subordinates = OfficerSeats.subordinates(ranks, state.officers(), superior).stream()
                    .filter(AtlasOfficer::heldCommand)
                    .toList();
            AtlasOfficer above = OfficerSeats.superior(ranks, state.officers(), superior);
            boolean chained = above != null && paired.contains(above.recordId())
                    || OfficerSeats.subordinates(ranks, state.officers(), superior).stream().anyMatch(officer -> paired.contains(officer.recordId()));
            AtlasOfficer rising = subordinates.isEmpty() ? null : subordinates.get(0);
            if (chained || rising == null) {
                continue;
            }
            int leadingRenown = subordinates.stream().mapToInt(AtlasOfficer::renown).max().orElse(0);
            state.putOfficer(rising.withRenown(Math.max(leadingRenown, superior.renown()) + RENOWN_LEAD));
            paired.add(superior.recordId());
            paired.add(rising.recordId());
            risen++;
        }
        return risen;
    }

    private static int movedOfficers(AtlasState state) {
        int moved = 0;
        for (TurnResult.StandingChange change : OfficerTurns.lastTurn(state).standingChanges()) {
            if (change.before() != null && change.after() != null
                    && (change.rankChanged() || change.before().seat() != change.after().seat())) {
                moved++;
            }
        }
        return moved;
    }

    private static boolean skirmishDeclared(AtlasState state) {
        return OfficerTurns.declaredIntents(state).stream()
                .anyMatch(intent -> intent.kind() == IntentKind.CHALLENGE && intent.targetRecordId() != Intent.NO_OFFICER);
    }

    private static Intent possibleChallenge(AtlasGenerator generator, AtlasState state) {
        List<OfficerRank> ranks = OfficerRanks.all();
        Set<Integer> busy = new HashSet<>();
        for (Intent intent : OfficerTurns.declaredIntents(state)) {
            busy.add(intent.officerRecordId());
            busy.add(intent.targetRecordId());
        }
        for (AtlasOfficer attacker : state.officers()) {
            if (!free(attacker, busy)) {
                continue;
            }
            List<AtlasOfficer> linked = new ArrayList<>(OfficerSeats.subordinates(ranks, state.officers(), attacker));
            AtlasOfficer superior = OfficerSeats.superior(ranks, state.officers(), attacker);
            if (superior != null) {
                linked.add(superior);
            }
            for (AtlasOfficer defender : linked) {
                Intent challenge = Intent.challenge(attacker.recordId(), defender.recordId());
                if (free(defender, busy) && SkirmishContests.battleCell(generator, state, challenge) != null) {
                    return challenge;
                }
            }
        }
        return null;
    }

    private static boolean free(AtlasOfficer officer, Set<Integer> busy) {
        return officer.isActive() && !busy.contains(officer.recordId());
    }
}
