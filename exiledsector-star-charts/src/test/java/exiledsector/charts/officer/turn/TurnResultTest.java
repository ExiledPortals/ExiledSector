package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasTerritoryPlacer;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import exiledsector.charts.officer.turn.TurnResult.LossKind;
import exiledsector.charts.officer.turn.TurnResult.OfficerLoss;
import exiledsector.charts.officer.turn.TurnResult.Replacement;
import exiledsector.charts.officer.turn.TurnResult.ResolvedIntent;
import exiledsector.charts.officer.turn.TurnResult.Standing;
import exiledsector.charts.officer.turn.TurnResult.StandingChange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnResultTest {

    private static TurnResult fullResult() {
        AtlasCell centre = new AtlasCell(2, -3);
        AtlasOfficer fallen = AtlasOfficer.waiting(4, "pirates", "lieutenant", "reckless", new AtlasOfficer.OfficerSeeds(4L, 5L, 6L, 7L), null)
                .takingCommand(centre, AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.ARRIVED_HISTORY, centre))
                .withRenown(3)
                .withLastChance(true)
                .withCarried(List.of(new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 2)));
        return new TurnResult(7, TurnCause.officerDefeated(4),
                List.of(new StandingChange(1, new Standing("commander", 4, 2), new Standing("captain", 6, 1)),
                        new StandingChange(9, null, new Standing("lieutenant", 0, 7)),
                        new StandingChange(4, new Standing("lieutenant", 3, 7), null)),
                List.of(new ResolvedIntent(IntentKind.PUSH, 1, 2, new AtlasCell(-1, 5), true, false),
                        new ResolvedIntent(IntentKind.CHALLENGE, 3, 1, null, false, true)),
                List.of(new OfficerLoss(4, LossKind.BEATEN_BY_PLAYER, Intent.NO_OFFICER, centre, false, true),
                        new OfficerLoss(3, LossKind.LOST_CHALLENGE, 1, new AtlasCell(6, 0), false, false)),
                List.of(new Replacement(4, 9)),
                Map.of(4, fallen));
    }

    @Test
    void aTurnResultRoundTrips() {
        TurnResult result = fullResult();
        assertEquals(result, TurnResult.decode(result.encode()));
    }

    @Test
    void aMissingOrMalformedResultDecodesToNone() {
        assertEquals(TurnResult.NONE, TurnResult.decode(null));
        assertEquals(TurnResult.NONE, TurnResult.decode(List.of()));
        assertEquals(TurnResult.NONE, TurnResult.decode(List.of("standing,1,commander,4,captain,6")));
        assertEquals(TurnResult.NONE, TurnResult.decode(List.of("turn,1,UNKNOWN_CAUSE,0")));
        assertEquals(TurnResult.NONE, TurnResult.decode(List.of("turn,1,FORCED,0", "intent,raid,1,2,,0,0")));
        assertEquals(TurnResult.NONE, TurnResult.decode(List.of("turn,1,FORCED,0", "loss,1")));
    }

    @Test
    void aStandingWrittenBeforeSeatsDecodesUnseated() {
        TurnResult result = TurnResult.decode(List.of("turn,1,FORCED,0", "standing,1,commander,4,captain,6"));

        assertEquals(new Standing("commander", 4, AtlasOfficer.UNSEATED), result.standingChange(1).before());
        assertEquals(new Standing("captain", 6, AtlasOfficer.UNSEATED), result.standingChange(1).after());
    }

    @Test
    void theLookupsFindTheirOfficer() {
        TurnResult result = fullResult();

        assertEquals(1, result.rankChanges().size());
        assertTrue(result.standingChange(1).rankChanged());
        assertFalse(result.standingChange(9).rankChanged());
        assertTrue(result.lossOf(4).died());
        assertEquals(1, result.lossOf(3).winnerRecordId());
        assertNull(result.lossOf(1));
    }
}
