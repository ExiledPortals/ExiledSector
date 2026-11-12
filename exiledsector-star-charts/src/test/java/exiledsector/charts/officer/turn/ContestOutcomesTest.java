package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.TestBorders;
import exiledsector.charts.atlas.TestBorders.Border;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static exiledsector.charts.officer.turn.TurnBoard.ATTACKER_WINS;
import static exiledsector.charts.officer.turn.TurnBoard.DEFENDER_WINS;
import static exiledsector.charts.officer.turn.TurnBoard.declare;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContestOutcomesTest {

    private AtlasState state;
    private AtlasGenerator generator;
    private Border border;

    private Intent push() {
        return Intent.push(border.attacker().recordId(), border.contested(), border.defender().recordId());
    }

    private Intent challenge() {
        return Intent.challenge(border.attacker().recordId(), border.defender().recordId());
    }

    private int renownOf(int recordId) {
        return state.officer(recordId).renown();
    }

    @BeforeEach
    void setUp() {
        state = new AtlasState(TestBorders.SEED);
        generator = TestBorders.generator(state);
        border = TestBorders.border(generator, state, "tritachyon", "hegemony");
    }

    @AfterEach
    void tearDown() {
        OfficerTurnTables.useForTests(null);
    }

    @Test
    void anAttackerWinMovesTheSystemAndSettlesThePush() {
        Intent defend = Intent.defend(border.defender().recordId());
        declare(state, push(), defend);
        int renownBefore = renownOf(border.attacker().recordId());

        assertTrue(ContestOutcomes.settle(generator, state, push(), border.contested(), true));

        assertEquals(border.attacker().commandCentre(), state.commandCentreOf(border.contested()));
        assertEquals(List.of(defend), OfficerTurns.declaredIntents(state));
        assertEquals(renownBefore + OfficerTurnTables.rules().renownPerClaim(), renownOf(border.attacker().recordId()));
    }

    @Test
    void aDefenderWinKeepsTheSystemAndRewardsTheDeclaredDefence() {
        declare(state, push(), Intent.defend(border.defender().recordId()));
        int renownBefore = renownOf(border.defender().recordId());

        assertFalse(ContestOutcomes.settle(generator, state, push(), border.contested(), false));

        assertEquals(border.defender().commandCentre(), state.commandCentreOf(border.contested()));
        assertEquals(List.of(Intent.defend(border.defender().recordId())), OfficerTurns.declaredIntents(state));
        assertEquals(renownBefore + OfficerTurnTables.rules().renownPerDefendHeld(), renownOf(border.defender().recordId()));
    }

    @Test
    void aChallengeWonInTheSkirmishAlsoTakesTheSystemAndStoresTheVerdict() {
        declare(state, challenge());

        assertTrue(ContestOutcomes.settle(generator, state, challenge(), border.contested(), true));

        assertEquals(border.attacker().commandCentre(), state.commandCentreOf(border.contested()));
        assertEquals(Boolean.TRUE, state.decidedContest(challenge().encode()));
        assertEquals(List.of(challenge()), OfficerTurns.declaredIntents(state));
    }

    @Test
    void anUndeclaredChallengeStoresNoVerdict() {
        declare(state);

        ContestOutcomes.settle(generator, state, challenge(), border.contested(), false);

        assertNull(state.decidedContest(challenge().encode()));
    }

    @Test
    void theAbstractRollFollowsTheTurnRules() {
        declare(state, push());

        OfficerTurnTables.useForTests(ATTACKER_WINS);
        assertTrue(ContestOutcomes.abstractAttackerWins(state, push(), border.contested()));
        OfficerTurnTables.useForTests(DEFENDER_WINS);
        assertFalse(ContestOutcomes.abstractAttackerWins(state, push(), border.contested()));
        assertFalse(ContestOutcomes.abstractAttackerWins(state, Intent.push(border.attacker().recordId() + 50, border.contested(),
                border.defender().recordId()), border.contested()));
    }

    @Test
    void aSystemTheDefenderNoLongerHoldsIsNotTakenAgain() {
        declare(state, push());
        state.claim(border.contested(), border.attacker().commandCentre());
        int renownBefore = renownOf(border.attacker().recordId());

        assertFalse(ContestOutcomes.settle(generator, state, push(), border.contested(), true));
        assertTrue(OfficerTurns.declaredIntents(state).isEmpty());
        assertEquals(renownBefore, renownOf(border.attacker().recordId()));
    }
}
