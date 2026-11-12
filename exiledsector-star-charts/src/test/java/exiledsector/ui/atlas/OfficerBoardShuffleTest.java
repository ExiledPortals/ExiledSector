package exiledsector.ui.atlas;

import exiledsector.charts.officer.turn.TurnResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerBoardShuffleTest {

    private static final List<String> RANKS = List.of("lieutenant", "commander", "admiral");

    private static Map<Integer, OfficerBoardShuffle.Mover> movers(OfficerBoardShuffle shuffle) {
        return shuffle.movers().stream().collect(Collectors.toMap(OfficerBoardShuffle.Mover::recordId, Function.identity()));
    }

    @Test
    void aPromotedOfficerSlidesUpARowWithARisingMark() {
        OfficerBoardShuffle shuffle = OfficerBoardShuffle.of(RANKS, List.of(new OfficerBoardLayout.Entry(1, "commander", 3)),
                List.of(new TurnResult.StandingChange(1, new TurnResult.Standing("lieutenant", 2), new TurnResult.Standing("commander", 3))));

        OfficerBoardShuffle.Mover mover = movers(shuffle).get(1);
        assertEquals(2, mover.from().row());
        assertEquals(1, mover.to().row());
        assertEquals(OfficerBoardShuffle.RankMark.ROSE, mover.mark());
    }

    @Test
    void aDemotedOfficerGetsAFallingMark() {
        OfficerBoardShuffle shuffle = OfficerBoardShuffle.of(RANKS, List.of(new OfficerBoardLayout.Entry(1, "commander", 0)),
                List.of(new TurnResult.StandingChange(1, new TurnResult.Standing("admiral", 9), new TurnResult.Standing("commander", 0))));

        assertEquals(OfficerBoardShuffle.RankMark.FELL, movers(shuffle).get(1).mark());
    }

    @Test
    void aRenownSwapWithinARowMovesColumnsWithoutAMark() {
        OfficerBoardShuffle shuffle = OfficerBoardShuffle.of(RANKS,
                List.of(new OfficerBoardLayout.Entry(1, "commander", 5), new OfficerBoardLayout.Entry(2, "commander", 3)),
                List.of(new TurnResult.StandingChange(1, new TurnResult.Standing("commander", 1), new TurnResult.Standing("commander", 5))));

        OfficerBoardShuffle.Mover mover = movers(shuffle).get(1);
        assertEquals(1, mover.from().column());
        assertEquals(0, mover.to().column());
        assertEquals(OfficerBoardShuffle.RankMark.NONE, mover.mark());
    }

    @Test
    void theFallenDepartAndTheirReplacementsArrive() {
        OfficerBoardShuffle shuffle = OfficerBoardShuffle.of(RANKS, List.of(new OfficerBoardLayout.Entry(2, "lieutenant", 0)),
                List.of(new TurnResult.StandingChange(1, new TurnResult.Standing("lieutenant", 4), null),
                        new TurnResult.StandingChange(2, null, new TurnResult.Standing("lieutenant", 0))));

        Map<Integer, OfficerBoardShuffle.Mover> movers = movers(shuffle);
        assertTrue(movers.get(1).departs());
        assertTrue(movers.get(2).arrives());
        assertNull(shuffle.after().seat(1));
        assertNull(shuffle.before().seat(2));
    }

    @Test
    void anUnchangedOfficerStaysPut() {
        OfficerBoardShuffle shuffle = OfficerBoardShuffle.of(RANKS, List.of(new OfficerBoardLayout.Entry(3, "admiral", 1)), List.of());

        OfficerBoardShuffle.Mover mover = movers(shuffle).get(3);
        assertEquals(mover.from(), mover.to());
    }

    @Test
    void theShuffleRunsDepartSlideArriveThenArrows() {
        assertEquals(1f, OfficerBoardShuffle.departAlpha(0f));
        assertEquals(0f, OfficerBoardShuffle.departAlpha(OfficerBoardShuffle.DEPART_START + OfficerBoardShuffle.DEPART_SECONDS));
        assertEquals(0f, OfficerBoardShuffle.slideProgress(OfficerBoardShuffle.SLIDE_START));
        assertEquals(0.5f, OfficerBoardShuffle.slideProgress(OfficerBoardShuffle.SLIDE_START + OfficerBoardShuffle.SLIDE_SECONDS / 2f), 1e-6f);
        assertEquals(1f, OfficerBoardShuffle.slideProgress(OfficerBoardShuffle.DURATION_SECONDS));
        assertEquals(0f, OfficerBoardShuffle.arriveAlpha(OfficerBoardShuffle.SLIDE_START));
        assertEquals(1f, OfficerBoardShuffle.arriveAlpha(OfficerBoardShuffle.DURATION_SECONDS));
        assertEquals(0f, OfficerBoardShuffle.arrowAlpha(OfficerBoardShuffle.SETTLE_START));
        assertEquals(1f, OfficerBoardShuffle.arrowAlpha(OfficerBoardShuffle.DURATION_SECONDS));
    }

    @Test
    void rankMarksShowOnlyMidShuffle() {
        assertEquals(0f, OfficerBoardShuffle.markAlpha(0f));
        assertEquals(1f, OfficerBoardShuffle.markAlpha(2f));
        assertEquals(0f, OfficerBoardShuffle.markAlpha(OfficerBoardShuffle.DURATION_SECONDS));
    }
}
