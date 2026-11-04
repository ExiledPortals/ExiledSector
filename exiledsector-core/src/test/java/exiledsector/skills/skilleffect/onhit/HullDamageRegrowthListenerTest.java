package exiledsector.skills.skilleffect.onhit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HullDamageRegrowthListenerTest {

    @Test
    void hullDamageEarnsFragmentsInProportionToTheStep() {
        float step = HullDamageRegrowthListener.HULL_DAMAGE_PER_STEP;

        assertEquals(1f, HullDamageRegrowthListener.fragmentsEarned(step, 1f), 0.001f);
        assertEquals(0.5f, HullDamageRegrowthListener.fragmentsEarned(step / 2f, 1f), 0.001f);
        assertEquals(3f, HullDamageRegrowthListener.fragmentsEarned(step * 2f, 1.5f), 0.001f);
    }

    @Test
    void noDamageOrNoMagnitudeEarnsNothing() {
        assertEquals(0f, HullDamageRegrowthListener.fragmentsEarned(0f, 1f));
        assertEquals(0f, HullDamageRegrowthListener.fragmentsEarned(500f, 0f));
    }
}
