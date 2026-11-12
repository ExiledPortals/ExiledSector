package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.scar.ScarTable;
import exiledsector.charts.officer.turn.TurnResult.LossKind;
import exiledsector.charts.officer.turn.TurnResult.OfficerLoss;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static exiledsector.charts.officer.turn.TurnBoard.place;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeLossScarsTest {

    private static final AtlasCell WINNER_CENTRE = new AtlasCell(0, 0);
    private static final AtlasCell LOSER_CENTRE = new AtlasCell(2, 0);
    private static final AtlasCell BEATEN_CENTRE = new AtlasCell(-2, 0);

    @Test
    void anOfficerWhoSurvivesALostChallengeGainsAScar() {
        AtlasState state = new AtlasState(3L);
        place(state, 1, "hegemony", "captain", WINNER_CENTRE);
        place(state, 2, "pirates", "lieutenant", LOSER_CENTRE);
        place(state, 3, "tritachyon", "lieutenant", BEATEN_CENTRE);

        ChallengeLossScars.INSTANCE.turnTaken(null, state, result(
                new OfficerLoss(2, LossKind.LOST_CHALLENGE, 1, LOSER_CENTRE, false, false),
                new OfficerLoss(3, LossKind.BEATEN_BY_PLAYER, 0, BEATEN_CENTRE, false, false)), null);

        assertEquals(Map.of(ScarTable.FALLBACK_SCAR_ID, 1), state.officer(2).scars());
        assertTrue(state.officer(1).scars().isEmpty());
        assertTrue(state.officer(3).scars().isEmpty());
    }

    @Test
    void anOfficerWhoDiedInTheChallengeGainsNoScar() {
        AtlasState state = new AtlasState(3L);
        place(state, 1, "hegemony", "captain", WINNER_CENTRE);
        place(state, 2, "pirates", "lieutenant", LOSER_CENTRE);

        ChallengeLossScars.INSTANCE.turnTaken(null, state, result(new OfficerLoss(2, LossKind.LOST_CHALLENGE, 1, LOSER_CENTRE, false, true)), null);

        assertTrue(state.officer(2).scars().isEmpty());
    }

    private static TurnResult result(OfficerLoss... losses) {
        return new TurnResult(1, TurnCause.forced(), List.of(), List.of(), List.of(losses), List.of(), Map.of());
    }
}
