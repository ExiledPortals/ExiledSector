package exiledlib.skills.skilleffect.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FadeTest {

    @Test
    void stepsTowardTheTargetAndStopsAtTheEnds() {
        assertEquals(0.75f, Fade.step(0.5f, true, 0.25f));
        assertEquals(0.25f, Fade.step(0.5f, false, 0.25f));
        assertEquals(1f, Fade.step(0.9f, true, 0.25f));
        assertEquals(0f, Fade.step(0.1f, false, 0.25f));
    }

    @Test
    void smoothEasesInAndOut() {
        assertEquals(0f, Fade.smooth(0f), 0.0001f);
        assertEquals(0.5f, Fade.smooth(0.5f), 0.0001f);
        assertEquals(1f, Fade.smooth(1f), 0.0001f);
        assertEquals(0.104f, Fade.smooth(0.2f), 0.0001f);
    }
}
