package exiledsector.skills.skilleffect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillEffectStackingTest {

    @Test
    void everyEffectNamedAsAMultiplierDeclaresTheMultiplierMode() {
        for (String name : SkillEffectRegistry.names()) {
            if (name.endsWith("_MULT")) {
                assertEquals(StatMode.MULT, SkillEffect.byName(name).statMode(), name);
            }
        }
    }

    @Test
    void stackingFollowsTheStatModeRatherThanTheName() {
        assertTrue(PhaseSkillEffect.COMBAT_BOOST_WHILE_PHASED.isMultiplicative());
        assertTrue(CompatSkillEffect.CONVERTED_HANGAR_REFIT_TIME_MULT.isMultiplicative());
        assertEquals(StatMode.PERCENT, FighterSkillEffect.FIGHTER_WEAPON_DAMAGE_PERCENT.statMode());
    }
}
