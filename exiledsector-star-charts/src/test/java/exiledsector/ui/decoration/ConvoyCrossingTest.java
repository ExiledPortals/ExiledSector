package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvoyCrossingTest {

    private static final float STAR_RADIUS = 90f;
    private static final int SHIPS = 5;
    private static final float STEP_SECONDS = 0.05f;

    @Test
    void aShipFadesOutBeforeItLoopsBackSoItNeverPopsInOrOut() {
        ConvoyCrossing crossing = new ConvoyCrossing(STAR_RADIUS, SHIPS);
        for (int ship = 0; ship < SHIPS; ship++) {
            float previousAlong = crossing.alongAt(ship, 0f);
            float previousVisibility = crossing.visibilityAt(ship, 0f);
            for (float seconds = STEP_SECONDS; seconds < 120f; seconds += STEP_SECONDS) {
                float along = crossing.alongAt(ship, seconds);
                float visibility = crossing.visibilityAt(ship, seconds);
                if (along < previousAlong) {
                    assertTrue(previousVisibility < 0.05f && visibility < 0.05f, "ship " + ship + " looped while visible at " + seconds);
                }
                assertTrue(Math.abs(along) <= crossing.trackHalfLength() + 0.01f);
                assertTrue(visibility >= 0f && visibility <= 1f);
                previousAlong = along;
                previousVisibility = visibility;
            }
        }
    }

    @Test
    void theConvoyMovesNoseFirstAlongItsHeadingBesideTheStar() {
        ConvoyCrossing crossing = new ConvoyCrossing(STAR_RADIUS, SHIPS);
        float headingDeg = 30f;
        float step = 0.01f;
        for (float seconds = 1f; seconds < 60f; seconds += 3.1f) {
            if (crossing.visibilityAt(0, seconds) < 1f) {
                continue;
            }
            double moveX = crossing.xAt(0, seconds + step, headingDeg) - crossing.xAt(0, seconds - step, headingDeg);
            double moveY = crossing.yAt(0, seconds + step, headingDeg) - crossing.yAt(0, seconds - step, headingDeg);
            float travel = (float) Math.toDegrees(Math.atan2(moveY, moveX));
            float gap = Math.abs(((headingDeg - travel) % 360f + 540f) % 360f - 180f);
            assertTrue(gap < 10f, "at " + seconds + " off by " + gap);
        }
    }

    @Test
    void theLaneRunsThroughTheCoronaOnTheLeftOfTheHeadingClearOfTheStar() {
        ConvoyCrossing crossing = new ConvoyCrossing(STAR_RADIUS, SHIPS);
        float headingDeg = -120f;
        double laneSide = Math.toRadians(headingDeg + ConvoyCrossing.LANE_SIDE_DEGREES);
        for (float seconds = 0f; seconds < 60f; seconds += 0.7f) {
            for (int ship = 0; ship < SHIPS; ship++) {
                float sideways = crossing.xAt(ship, seconds, headingDeg) * (float) Math.cos(laneSide)
                        + crossing.yAt(ship, seconds, headingDeg) * (float) Math.sin(laneSide);
                assertEquals(crossing.laneOffsetAt(ship, seconds), sideways, 0.05f);
                assertTrue(sideways > STAR_RADIUS * 1.2f && sideways < STAR_RADIUS * 2f, "ship " + ship + " at " + sideways);
            }
        }
    }

    @Test
    void theShipsStayEvenlySpacedAlongTheLoop() {
        ConvoyCrossing crossing = new ConvoyCrossing(STAR_RADIUS, SHIPS);
        float spacing = crossing.trackHalfLength() * 2f / SHIPS;
        for (float seconds = 0f; seconds < 40f; seconds += 1.3f) {
            for (int ship = 1; ship < SHIPS; ship++) {
                float gap = crossing.alongAt(ship - 1, seconds) - crossing.alongAt(ship, seconds);
                float wrapped = ((gap % (spacing * SHIPS)) + spacing * SHIPS) % (spacing * SHIPS);
                assertEquals(spacing, wrapped, 0.05f);
            }
        }
    }

    @Test
    void theSameMomentAlwaysPutsAShipInTheSamePlace() {
        ConvoyCrossing first = new ConvoyCrossing(STAR_RADIUS, SHIPS);
        ConvoyCrossing again = new ConvoyCrossing(STAR_RADIUS, SHIPS);
        for (float seconds = 0f; seconds < 30f; seconds += 0.9f) {
            assertEquals(first.xAt(2, seconds, 45f), again.xAt(2, seconds, 45f));
            assertEquals(first.yAt(2, seconds, 45f), again.yAt(2, seconds, 45f));
        }
    }
}
