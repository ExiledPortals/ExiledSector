package exiledsector.ui.cargo;

import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.skills.skilleffect.render.Fade;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalystIconFxTest {

    private static final float EPSILON = 1e-4f;

    @Test
    void theBracketsCloseInHoldAndFadeBeforeTheCycleRepeats() {
        assertTrue(CatalystIconFx.bracketDistance(0f) > CatalystIconFx.bracketDistance(0.5f));
        assertEquals(CatalystIconFx.bracketDistance(0.2f), CatalystIconFx.bracketDistance(0.8f), EPSILON);
        assertEquals(0f, CatalystIconFx.bracketAlpha(0f), EPSILON);
        assertEquals(1f, CatalystIconFx.bracketAlpha(0.5f), EPSILON);
        assertEquals(0f, CatalystIconFx.bracketAlpha(1f), EPSILON);
    }

    @Test
    void aTerminusSwellsThenSnapsInwardAndOnlyScarsAfterTheCollapse() {
        assertTrue(CatalystIconFx.strainRadius(0.45f) > CatalystIconFx.strainRadius(0.1f));
        assertTrue(CatalystIconFx.strainRadius(0.52f) < CatalystIconFx.strainRadius(0.1f));
        assertEquals(0f, CatalystIconFx.strainAlpha(0.6f, true), EPSILON);
        assertEquals(0f, CatalystIconFx.scarAlpha(0.4f), EPSILON);
        assertTrue(CatalystIconFx.scarAlpha(0.55f) > CatalystIconFx.scarAlpha(0.75f));
        assertEquals(0f, CatalystIconFx.scarAlpha(0.9f), EPSILON);
    }

    @Test
    void aSurveyRevealsTheIconFromTheTopDownAndLetsItFadeBack() {
        assertTrue(CatalystIconFx.revealLevel(0.1f, 0.3f) > CatalystIconFx.revealLevel(0.9f, 0.3f));
        assertEquals(1f, CatalystIconFx.revealLevel(0.9f, 0.7f), EPSILON);
        assertEquals(0f, CatalystIconFx.revealLevel(0.1f, 0.99f), EPSILON);
    }

    @Test
    void everyFamilyLoopsOnItsOwnCycle() {
        for (AtlasCatalysts.Family family : AtlasCatalysts.Family.values()) {
            float seconds = CatalystIconFx.cycleSeconds(family);
            assertEquals(0.25f, Fade.cycle(seconds * 3.25f, seconds), EPSILON, family.key());
        }
        assertTrue(CatalystIconFx.streakProgress(0.2f, 0) > 0f && CatalystIconFx.streakProgress(0.2f, 2) < 0f);
    }
}
