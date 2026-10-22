package exiledsector.skills.skilleffect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatSkillEffectTest {

    @Test
    void convertedHangarPenaltiesColourLikeTheStatsTheyPenalise() {
        assertTrue(CompatSkillEffect.CONVERTED_HANGAR_REFIT_TIME_MULT.lowerIsBetter());
        assertTrue(CompatSkillEffect.CONVERTED_HANGAR_RELAUNCH_TIME_FLAT.lowerIsBetter());
        assertTrue(CompatSkillEffect.CONVERTED_HANGAR_MIN_CREW_FLAT.lowerIsBetter());
        assertFalse(CompatSkillEffect.CONVERTED_HANGAR_REPLACEMENT_RATE_MULT.lowerIsBetter());
    }
}
