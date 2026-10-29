package exiledsector.skills.skilleffect.fighter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvertedHangarPenaltyTest {

    @Test
    void convertedHangarPenaltiesColourLikeTheStatsTheyPenalise() {
        assertTrue(FighterSkillEffect.CONVERTED_HANGAR_REFIT_TIME_MULT.lowerIsBetter());
        assertTrue(FighterSkillEffect.CONVERTED_HANGAR_RELAUNCH_TIME_FLAT.lowerIsBetter());
        assertTrue(FighterSkillEffect.CONVERTED_HANGAR_MIN_CREW_FLAT.lowerIsBetter());
        assertFalse(FighterSkillEffect.CONVERTED_HANGAR_REPLACEMENT_RATE_MULT.lowerIsBetter());
    }
}
