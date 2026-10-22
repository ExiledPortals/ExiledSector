package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HullSizeSkillEffectTest {

    private static final HullSizeSkillEffect EFFECT =
            new HullSizeSkillEffect(DefenseSkillEffect.HULL_PERCENT, 1f, 2f, 3f, 4f);

    @Test
    void returnsTheTunedValueForEachRealShipSize() {
        assertEquals(1f, EFFECT.valueFor(HullSize.FRIGATE));
        assertEquals(2f, EFFECT.valueFor(HullSize.DESTROYER));
        assertEquals(3f, EFFECT.valueFor(HullSize.CRUISER));
        assertEquals(4f, EFFECT.valueFor(HullSize.CAPITAL_SHIP));
    }

    @Test
    void foldsHullSizesLargerOrUndefinedThanCapitalIntoTheCapitalValue() {
        assertEquals(4f, EFFECT.valueFor(HullSize.DEFAULT));
        assertEquals(4f, EFFECT.valueFor(HullSize.FIGHTER));
        assertEquals(4f, EFFECT.valueFor(null));
    }
}
