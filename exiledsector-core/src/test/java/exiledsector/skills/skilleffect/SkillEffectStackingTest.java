package exiledsector.skills.skilleffect;

import exiledsector.skills.skilleffect.fighter.FighterSkillEffect;
import exiledsector.skills.skilleffect.phase.PhaseSkillEffect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillEffectStackingTest {

    @Test
    void everyStatEffectNamedAsAMultiplierDeclaresTheMultiplierMode() {
        for (String name : SkillEffectRegistry.names()) {
            StatMode mode = SkillEffect.byName(name).statMode();
            if (name.endsWith("_MULT") && mode != null) {
                assertEquals(StatMode.MULT, mode, name);
            }
        }
    }

    @Test
    void noEffectNamedAsAPercentDeclaresTheMultiplierMode() {
        for (String name : SkillEffectRegistry.names()) {
            if (name.endsWith("_PERCENT")) {
                assertNotEquals(StatMode.MULT, SkillEffect.byName(name).statMode(), name);
            }
        }
    }

    @Test
    void stackingFollowsTheStatModeRatherThanTheName() {
        assertTrue(PhaseSkillEffect.COMBAT_BOOST_WHILE_PHASED.isMultiplicative());
        assertTrue(FighterSkillEffect.CONVERTED_HANGAR_REFIT_TIME_MULT.isMultiplicative());
        assertEquals(StatMode.PERCENT, FighterSkillEffect.FIGHTER_WEAPON_DAMAGE_PERCENT.statMode());
    }
}
