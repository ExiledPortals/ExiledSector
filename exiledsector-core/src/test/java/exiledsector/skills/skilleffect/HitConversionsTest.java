package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.DamageType;
import org.junit.jupiter.api.Test;

import static exiledsector.skills.skilleffect.HitConversions.EMP;
import static exiledsector.skills.skilleffect.HitConversions.PORTIONS;
import static exiledsector.skills.skilleffect.HitConversions.portionOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitConversionsTest {

    private static final float TOLERANCE = 1e-4f;
    private static final int KINETIC = portionOf(DamageType.KINETIC);
    private static final int HIGH_EXPLOSIVE = portionOf(DamageType.HIGH_EXPLOSIVE);
    private static final int ENERGY = portionOf(DamageType.ENERGY);

    private static float[] hit(float kinetic, float emp) {
        float[] portions = new float[PORTIONS];
        portions[KINETIC] = kinetic;
        portions[EMP] = emp;
        return portions;
    }

    @Test
    void theEmpSlotFollowsEveryDamageType() {
        assertEquals(DamageType.values().length, EMP);
        assertEquals(EMP + 1, PORTIONS);
    }

    @Test
    void noRequestsLeaveTheHitAsItWas() {
        HitConversions conversions = new HitConversions();
        float[] original = hit(100f, 20f);

        assertTrue(conversions.isEmpty());
        assertArrayEquals(original, conversions.applyTo(original), TOLERANCE);
    }

    @Test
    void aConversionIntoItsOwnPortionOrOfNothingIsIgnored() {
        HitConversions conversions = new HitConversions();

        conversions.convert(KINETIC, KINETIC, 0.5f);
        conversions.convert(KINETIC, ENERGY, 0f);
        conversions.convert(KINETIC, ENERGY, -0.5f);
        conversions.gain(KINETIC, EMP, 0f);

        assertTrue(conversions.isEmpty());
    }

    @Test
    void aPartialConversionMovesThatShareOfThePortion() {
        HitConversions conversions = new HitConversions();
        conversions.convert(KINETIC, HIGH_EXPLOSIVE, 0.4f);

        float[] converted = conversions.applyTo(hit(100f, 0f));

        assertFalse(conversions.isEmpty());
        assertEquals(60f, converted[KINETIC], TOLERANCE);
        assertEquals(40f, converted[HIGH_EXPLOSIVE], TOLERANCE);
    }

    @Test
    void oneConversionMovesAtMostTheWholePortion() {
        HitConversions conversions = new HitConversions();
        conversions.convert(KINETIC, HIGH_EXPLOSIVE, 1.5f);

        float[] converted = conversions.applyTo(hit(100f, 0f));

        assertEquals(0f, converted[KINETIC], TOLERANCE);
        assertEquals(100f, converted[HIGH_EXPLOSIVE], TOLERANCE);
    }

    @Test
    void theOriginalAmountsAreLeftUntouched() {
        HitConversions conversions = new HitConversions();
        conversions.convert(KINETIC, ENERGY, 0.5f);
        float[] original = hit(100f, 10f);

        conversions.applyTo(original);

        assertArrayEquals(hit(100f, 10f), original, TOLERANCE);
    }

    @Test
    void conversionsOutOfOnePortionBelowTheWholeKeepTheRemainder() {
        HitConversions conversions = new HitConversions();
        conversions.convert(KINETIC, HIGH_EXPLOSIVE, 0.3f);
        conversions.convert(KINETIC, ENERGY, 0.2f);

        float[] converted = conversions.applyTo(hit(100f, 0f));

        assertEquals(50f, converted[KINETIC], TOLERANCE);
        assertEquals(30f, converted[HIGH_EXPLOSIVE], TOLERANCE);
        assertEquals(20f, converted[ENERGY], TOLERANCE);
    }

    @Test
    void conversionsOutOfOnePortionPastTheWholeAreScaledDownInProportion() {
        HitConversions conversions = new HitConversions();
        conversions.convert(KINETIC, HIGH_EXPLOSIVE, 0.8f);
        conversions.convert(KINETIC, ENERGY, 0.6f);

        float[] converted = conversions.applyTo(hit(100f, 0f));

        assertEquals(0f, converted[KINETIC], TOLERANCE);
        assertEquals(100f * 0.8f / 1.4f, converted[HIGH_EXPLOSIVE], TOLERANCE);
        assertEquals(100f * 0.6f / 1.4f, converted[ENERGY], TOLERANCE);
    }

    @Test
    void conversionsPastTheWholeIntoSeveralTargetsIncludingEmpShareThePortion() {
        HitConversions conversions = new HitConversions();
        conversions.convert(KINETIC, HIGH_EXPLOSIVE, 0.5f);
        conversions.convert(KINETIC, ENERGY, 0.5f);
        conversions.convert(KINETIC, EMP, 1f);

        float[] converted = conversions.applyTo(hit(100f, 0f));

        assertEquals(0f, converted[KINETIC], TOLERANCE);
        assertEquals(25f, converted[HIGH_EXPLOSIVE], TOLERANCE);
        assertEquals(25f, converted[ENERGY], TOLERANCE);
        assertEquals(50f, converted[EMP], TOLERANCE);
    }

    @Test
    void everyConversionIsComputedFromTheOriginalSplitAtOnce() {
        HitConversions conversions = new HitConversions();
        conversions.convert(KINETIC, HIGH_EXPLOSIVE, 0.5f);
        conversions.convert(HIGH_EXPLOSIVE, ENERGY, 0.5f);

        float[] converted = conversions.applyTo(hit(100f, 0f));

        assertEquals(50f, converted[KINETIC], TOLERANCE);
        assertEquals(50f, converted[HIGH_EXPLOSIVE], TOLERANCE);
        assertEquals(0f, converted[ENERGY], TOLERANCE);
    }

    @Test
    void theOrderOfRequestsDoesNotChangeTheResult() {
        HitConversions forwards = new HitConversions();
        forwards.convert(KINETIC, HIGH_EXPLOSIVE, 0.7f);
        forwards.convert(HIGH_EXPLOSIVE, ENERGY, 0.5f);
        forwards.convert(KINETIC, EMP, 0.6f);
        forwards.convert(EMP, ENERGY, 0.25f);
        forwards.gain(KINETIC, EMP, 0.1f);
        HitConversions backwards = new HitConversions();
        backwards.gain(KINETIC, EMP, 0.1f);
        backwards.convert(EMP, ENERGY, 0.25f);
        backwards.convert(KINETIC, EMP, 0.6f);
        backwards.convert(HIGH_EXPLOSIVE, ENERGY, 0.5f);
        backwards.convert(KINETIC, HIGH_EXPLOSIVE, 0.7f);
        float[] original = hit(100f, 40f);
        original[HIGH_EXPLOSIVE] = 20f;

        assertArrayEquals(forwards.applyTo(original), backwards.applyTo(original), TOLERANCE);
    }

    @Test
    void damageMovedIntoEmpIsNotMovedOnByAnEmpConversion() {
        HitConversions conversions = new HitConversions();
        conversions.convert(KINETIC, EMP, 0.5f);
        conversions.convert(EMP, ENERGY, 0.5f);

        float[] converted = conversions.applyTo(hit(100f, 20f));

        assertEquals(50f, converted[KINETIC], TOLERANCE);
        assertEquals(10f, converted[ENERGY], TOLERANCE);
        assertEquals(60f, converted[EMP], TOLERANCE);
    }

    @Test
    void anExtraGainIsAddedOnTopWithoutTakingFromItsSource() {
        HitConversions conversions = new HitConversions();
        conversions.gain(KINETIC, EMP, 0.3f);

        float[] converted = conversions.applyTo(hit(100f, 5f));

        assertEquals(100f, converted[KINETIC], TOLERANCE);
        assertEquals(35f, converted[EMP], TOLERANCE);
    }

    @Test
    void anExtraGainIsComputedFromTheOriginalAmountAndIgnoresTheCap() {
        HitConversions conversions = new HitConversions();
        conversions.convert(KINETIC, HIGH_EXPLOSIVE, 0.8f);
        conversions.convert(KINETIC, ENERGY, 0.6f);
        conversions.gain(KINETIC, EMP, 1.5f);

        float[] converted = conversions.applyTo(hit(100f, 0f));

        assertEquals(150f, converted[EMP], TOLERANCE);
        assertEquals(100f * 0.8f / 1.4f, converted[HIGH_EXPLOSIVE], TOLERANCE);
        assertEquals(100f * 0.6f / 1.4f, converted[ENERGY], TOLERANCE);
    }
}
