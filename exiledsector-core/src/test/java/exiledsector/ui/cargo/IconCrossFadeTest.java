package exiledsector.ui.cargo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IconCrossFadeTest {

    private static final float EPSILON = 1e-4f;

    @Test
    void eachIconTakesOneHoldAndOneFade() {
        assertEquals(IconCrossFade.HOLD_SECONDS + IconCrossFade.FADE_SECONDS, IconCrossFade.STEP_SECONDS, EPSILON);
        assertEquals(3f * IconCrossFade.STEP_SECONDS, IconCrossFade.periodSeconds(3), EPSILON);
        assertEquals(IconCrossFade.STEP_SECONDS, IconCrossFade.periodSeconds(0), EPSILON);
    }

    @Test
    void anIconHoldsFullyShownBeforeItFades() {
        IconCrossFade.Frame frame = IconCrossFade.frameAt(IconCrossFade.HOLD_SECONDS * 0.5f, 3);

        assertEquals(new IconCrossFade.Frame(0, 1, 0f), frame);
        assertEquals(1f, frame.shownAlpha(), EPSILON);
    }

    @Test
    void halfwayThroughTheFadeBothIconsShowEqually() {
        IconCrossFade.Frame frame = IconCrossFade.frameAt(IconCrossFade.STEP_SECONDS + IconCrossFade.HOLD_SECONDS + IconCrossFade.FADE_SECONDS / 2f, 3);

        assertEquals(1, frame.shownIndex());
        assertEquals(2, frame.nextIndex());
        assertEquals(0.5f, frame.nextAlpha(), EPSILON);
        assertEquals(0.5f, frame.shownAlpha(), EPSILON);
    }

    @Test
    void theLastIconFadesBackIntoTheFirstAndTheCycleRepeats() {
        IconCrossFade.Frame lastFade = IconCrossFade.frameAt(3f * IconCrossFade.STEP_SECONDS - IconCrossFade.FADE_SECONDS * 0.25f, 3);

        assertEquals(2, lastFade.shownIndex());
        assertEquals(0, lastFade.nextIndex());
        assertEquals(IconCrossFade.frameAt(0.3f, 3), IconCrossFade.frameAt(0.3f + IconCrossFade.periodSeconds(3) * 4f, 3));
        assertEquals(IconCrossFade.frameAt(0.3f, 3).shownIndex(), IconCrossFade.frameAt(0.3f - IconCrossFade.periodSeconds(3), 3).shownIndex());
    }

    @Test
    void aSingleIconOrNoneNeverFades() {
        for (float seconds : new float[]{0f, IconCrossFade.HOLD_SECONDS + 0.1f, 100f}) {
            assertEquals(new IconCrossFade.Frame(0, 0, 0f), IconCrossFade.frameAt(seconds, 1));
            assertEquals(new IconCrossFade.Frame(0, 0, 0f), IconCrossFade.frameAt(seconds, 0));
        }
    }
}
