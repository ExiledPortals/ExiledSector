package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.DamageType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeSharesTest {

    private static final float TOLERANCE = 1e-5f;

    private static float[] amounts(Map<DamageType, Float> amountByType, float emp) {
        float[] amounts = new float[HitConversions.PORTIONS];
        amountByType.forEach((type, amount) -> amounts[HitConversions.portionOf(type)] = amount);
        amounts[HitConversions.EMP] = emp;
        return amounts;
    }

    @Test
    void aHitWithNoConversionIsAllItsOwnType() {
        TypeShares kinetic = TypeShares.single(DamageType.KINETIC);

        assertEquals(1f, kinetic.share(DamageType.KINETIC));
        assertEquals(0f, kinetic.share(DamageType.HIGH_EXPLOSIVE));
        assertFalse(kinetic.mixed());
        assertEquals(Set.of(DamageType.KINETIC), kinetic.types());
        assertSame(kinetic, TypeShares.single(DamageType.KINETIC));
    }

    @Test
    void aHitWithoutATypeCountsAsOther() {
        assertSame(TypeShares.single(DamageType.OTHER), TypeShares.single(null));
        assertEquals(0f, TypeShares.single(DamageType.OTHER).share(null));
    }

    @Test
    void amountsOfTwoTypesSplitTheHitBetweenThem() {
        TypeShares mix = TypeShares.ofAmounts(amounts(Map.of(DamageType.KINETIC, 60f, DamageType.HIGH_EXPLOSIVE, 40f), 0f));

        assertEquals(0.6f, mix.share(DamageType.KINETIC), TOLERANCE);
        assertEquals(0.4f, mix.share(DamageType.HIGH_EXPLOSIVE), TOLERANCE);
        assertTrue(mix.mixed());
        assertEquals(Set.of(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE), mix.types());
        assertEquals(DamageType.KINETIC, mix.largest());
    }

    @Test
    void theEmpAmountTakesNoShareOfTheDamageTypes() {
        TypeShares mix = TypeShares.ofAmounts(amounts(Map.of(DamageType.KINETIC, 50f, DamageType.ENERGY, 150f), 1000f));

        assertEquals(0.25f, mix.share(DamageType.KINETIC), TOLERANCE);
        assertEquals(0.75f, mix.share(DamageType.ENERGY), TOLERANCE);
    }

    @Test
    void amountsOfOneTypeAreThatSingleType() {
        TypeShares converted = TypeShares.ofAmounts(amounts(Map.of(DamageType.HIGH_EXPLOSIVE, 80f), 30f));

        assertFalse(converted.mixed());
        assertEquals(TypeShares.single(DamageType.HIGH_EXPLOSIVE), converted);
    }

    @Test
    void negativeAmountsCountAsNone() {
        TypeShares shares = TypeShares.ofAmounts(amounts(Map.of(DamageType.KINETIC, 100f, DamageType.FRAGMENTATION, -50f), 0f));

        assertEquals(TypeShares.single(DamageType.KINETIC), shares);
    }

    @Test
    void aHitWithNoDamageLeftHasNoShares() {
        assertNull(TypeShares.ofAmounts(amounts(Map.of(), 200f)));
        assertNull(TypeShares.ofAmounts(amounts(Map.of(DamageType.KINETIC, 0f), 0f)));
    }

    @Test
    void perTypeValuesAreWeightedByShare() {
        TypeShares mix = TypeShares.ofAmounts(amounts(Map.of(DamageType.KINETIC, 60f, DamageType.HIGH_EXPLOSIVE, 40f), 0f));
        Map<DamageType, Float> bonusByType = Map.of(DamageType.KINETIC, 20f, DamageType.HIGH_EXPLOSIVE, 50f);

        assertEquals(32f, mix.weighted(type -> bonusByType.getOrDefault(type, 0f)), TOLERANCE);
    }
}
