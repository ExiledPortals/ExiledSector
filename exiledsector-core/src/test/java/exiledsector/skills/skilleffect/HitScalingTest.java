package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableStat;
import org.junit.jupiter.api.Test;

import static exiledsector.skills.skilleffect.HitConversions.EMP;
import static exiledsector.skills.skilleffect.HitConversions.portionOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitScalingTest {

    private static final float TOLERANCE = 1e-4f;
    private static final int KINETIC = portionOf(DamageType.KINETIC);
    private static final int ENERGY = portionOf(DamageType.ENERGY);

    @Test
    void anUnscaledPortionKeepsItsDamage() {
        HitScaling scaling = new HitScaling();

        assertEquals(1f, scaling.factor(KINETIC, 1f), TOLERANCE);
        assertEquals(1f, scaling.factor(EMP, 1.5f), TOLERANCE);
    }

    @Test
    void increasesToOnePortionAddUp() {
        HitScaling scaling = new HitScaling();
        scaling.increase(KINETIC, 30f);
        scaling.increase(KINETIC, 20f);

        assertEquals(1.5f, scaling.factor(KINETIC, 1f), TOLERANCE);
        assertEquals(1f, scaling.factor(ENERGY, 1f), TOLERANCE);
    }

    @Test
    void anIncreasePoolsWithTheGenericIncreasesAlreadyOnTheHit() {
        HitScaling scaling = new HitScaling();
        scaling.increase(KINETIC, 50f);

        assertEquals(2f / 1.5f, scaling.factor(KINETIC, 1.5f), TOLERANCE);
    }

    @Test
    void aLargerGenericPoolLeavesTheSameIncreaseLessToAdd() {
        HitScaling scaling = new HitScaling();
        scaling.increase(ENERGY, 50f);

        float alone = scaling.factor(ENERGY, 1f);
        float pooled = scaling.factor(ENERGY, 2f);

        assertEquals(1.5f, alone, TOLERANCE);
        assertEquals(1.25f, pooled, TOLERANCE);
        assertTrue(pooled < alone);
    }

    @Test
    void moreMultipliersCompoundWithEachOtherAndWithTheIncreases() {
        HitScaling scaling = new HitScaling();
        scaling.more(KINETIC, 1.2f);
        scaling.more(KINETIC, 1.5f);

        assertEquals(1.8f, scaling.factor(KINETIC, 1f), TOLERANCE);

        scaling.increase(KINETIC, 50f);

        assertEquals(1.5f * 1.8f, scaling.factor(KINETIC, 1f), TOLERANCE);
    }

    @Test
    void aNegativeMoreMultiplierStopsAtNoDamage() {
        HitScaling scaling = new HitScaling();
        scaling.more(EMP, -0.5f);

        assertEquals(0f, scaling.factor(EMP, 1f), TOLERANCE);
    }

    @Test
    void decreasesPastTheWholePoolStopAtNoDamage() {
        HitScaling scaling = new HitScaling();
        scaling.increase(KINETIC, -200f);

        assertEquals(0f, scaling.factor(KINETIC, 1f), TOLERANCE);
    }

    @Test
    void anEmptyGenericPoolAppliesOnlyTheMoreMultipliers() {
        HitScaling scaling = new HitScaling();
        scaling.increase(KINETIC, 50f);
        scaling.more(KINETIC, 2f);

        assertEquals(2f, scaling.factor(KINETIC, 0f), TOLERANCE);
        assertEquals(2f, scaling.factor(KINETIC, -1f), TOLERANCE);
    }

    @Test
    void theGenericPoolIsTheModifiersBaseWithItsPercentAndFlatModsButNotItsMultipliers() {
        MutableStat modifier = new MutableStat(1f);
        modifier.modifyPercent("first", 30f);
        modifier.modifyPercent("second", 20f);
        modifier.modifyFlat("flat", 0.25f);
        modifier.modifyMult("mult", 2f);

        assertEquals(1.75f, HitScaling.genericPool(modifier), TOLERANCE);
    }

    @Test
    void aHitWithoutAModifierHasAGenericPoolOfOne() {
        assertEquals(1f, HitScaling.genericPool(null), TOLERANCE);
        assertEquals(1f, HitScaling.genericPool(new MutableStat(1f)), TOLERANCE);
    }
}
