package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.officer.turn.Intent;
import exiledsector.charts.officer.turn.IntentKind;
import exiledsector.charts.officer.turn.TurnResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerBoardArrowsTest {

    private static final AtlasCell CELL = new AtlasCell(1, 2);

    @Test
    void aSolidArrowRunsFromTheWinnerToTheLoser() {
        List<TurnResult.ResolvedIntent> lastTurn = List.of(new TurnResult.ResolvedIntent(IntentKind.PUSH, 1, 2, CELL, true, false),
                new TurnResult.ResolvedIntent(IntentKind.CHALLENGE, 3, 4, null, false, true));

        List<OfficerBoardArrows.Arrow> arrows = OfficerBoardArrows.arrows(lastTurn, List.of(), recordId -> true);

        assertEquals(List.of(new OfficerBoardArrows.Arrow(1, 2, OfficerBoardArrows.Style.SOLID),
                new OfficerBoardArrows.Arrow(4, 3, OfficerBoardArrows.Style.SOLID)), arrows);
    }

    @Test
    void expandsAndPushesIntoUnclaimedSpaceDrawNoArrow() {
        List<TurnResult.ResolvedIntent> lastTurn = List.of(new TurnResult.ResolvedIntent(IntentKind.EXPAND, 1, Intent.NO_OFFICER, CELL, true, false),
                new TurnResult.ResolvedIntent(IntentKind.PUSH, 1, Intent.NO_OFFICER, CELL, true, false));
        List<Intent> declared = List.of(Intent.expand(1, CELL), Intent.hold(2));

        assertTrue(OfficerBoardArrows.arrows(lastTurn, declared, recordId -> true).isEmpty());
    }

    @Test
    void aDeclaredConflictDrawsADashedArrowToItsTarget() {
        List<Intent> declared = List.of(Intent.challenge(5, 6), Intent.push(7, CELL, 8));

        assertEquals(List.of(new OfficerBoardArrows.Arrow(5, 6, OfficerBoardArrows.Style.DASHED),
                new OfficerBoardArrows.Arrow(7, 8, OfficerBoardArrows.Style.DASHED)), OfficerBoardArrows.arrows(List.of(), declared, recordId -> true));
    }

    @Test
    void anArrowNeedsBothEndsShown() {
        List<Intent> declared = List.of(Intent.challenge(5, 6), Intent.challenge(7, 8));

        assertEquals(List.of(new OfficerBoardArrows.Arrow(7, 8, OfficerBoardArrows.Style.DASHED)),
                OfficerBoardArrows.arrows(List.of(), declared, recordId -> recordId != 6));
    }

    @Test
    void shieldsMarkShownDefenders() {
        List<Intent> declared = List.of(Intent.defend(1), Intent.defend(2), Intent.hold(3));

        assertEquals(Set.of(1), OfficerBoardArrows.shields(declared, recordId -> recordId != 2));
    }

    @Test
    void hoveringBrightensItsOwnArrowsAndFadesTheRest() {
        OfficerBoardArrows.Arrow arrow = new OfficerBoardArrows.Arrow(1, 2, OfficerBoardArrows.Style.SOLID);

        assertEquals(OfficerBoardArrows.RESTING_ALPHA, OfficerBoardArrows.alpha(arrow, null));
        assertEquals(OfficerBoardArrows.HOVERED_ALPHA, OfficerBoardArrows.alpha(arrow, 2));
        assertEquals(OfficerBoardArrows.FADED_ALPHA, OfficerBoardArrows.alpha(arrow, 3));
    }

    @Test
    void aCurveStartsAndEndsTrimmedAndBulgesToOneSide() {
        float[] points = OfficerBoardArrows.curve(0f, 0f, 100f, 0f, 10f, 0.2f, 4);

        assertEquals(10, points.length);
        assertEquals(10f, points[0], 1e-4f);
        assertEquals(0f, points[1], 1e-4f);
        assertEquals(90f, points[8], 1e-4f);
        assertEquals(0f, points[9], 1e-4f);
        assertTrue(points[5] < 0f);
    }

    @Test
    void portraitsTooCloseForTheTrimDrawNoCurve() {
        assertEquals(0, OfficerBoardArrows.curve(0f, 0f, 10f, 0f, 6f, 0.2f, 4).length);
    }
}
