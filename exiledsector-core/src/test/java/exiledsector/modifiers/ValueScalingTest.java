package exiledsector.modifiers;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.ShipHullSizes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueScalingTest {

    private static final List<Float> HULL_VALUES = List.of(5f, 10f, 15f, 20f);

    private static float legacyHullValue(List<Float> hullValues, float rolledMagnitude, HullSize hullSize) {
        boolean hasHullValue = !hullValues.isEmpty() && ShipHullSizes.isShipSize(hullSize);
        return hasHullValue ? hullValues.get(ShipHullSizes.ORDER.indexOf(hullSize)) : rolledMagnitude;
    }

    @Test
    void aHullSizeLineReadsExactlyAsTheOldHullValueListDid() {
        ModifierLine parsed = ModifierLine.parse("ARMOR_FLAT:5/10/15/20").line();
        List<HullSize> contexts = new java.util.ArrayList<>(List.of(HullSize.values()));
        contexts.add(null);

        assertTrue(parsed.scalesWithHullSize());
        assertTrue(parsed.isScaled());
        assertFalse(parsed.scalesWithTier());
        assertEquals(HULL_VALUES, parsed.hullValues());
        assertEquals(HULL_VALUES, parsed.scaledValues());
        assertEquals(5f, parsed.min());
        assertEquals(5f, parsed.max());
        for (HullSize hullSize : contexts) {
            boolean hasHullValue = ShipHullSizes.isShipSize(hullSize);
            assertEquals(legacyHullValue(HULL_VALUES, 3f, hullSize), parsed.valueFor(3f, hullSize), String.valueOf(hullSize));
            assertEquals(hasHullValue, parsed.hasHullValueFor(hullSize), String.valueOf(hullSize));
            assertEquals(!hasHullValue, parsed.listsEveryHullValueFor(hullSize), String.valueOf(hullSize));
            assertEquals(parsed.valueFor(3f, hullSize), parsed.valueFor(3f, ScaleContext.ofHullSize(hullSize)), String.valueOf(hullSize));
        }
    }

    @Test
    void aRangeLineHasNoScalingOnAnyAxis() {
        ModifierLine range = ModifierLine.range("HULL_MULT", 4f, 6f);

        assertSame(ValueScaling.NONE, range.scaling());
        assertFalse(range.isScaled());
        assertFalse(range.scalesWithHullSize());
        assertEquals(List.of(), range.hullValues());
        assertEquals(5f, range.valueFor(5f, HullSize.CRUISER));
        assertEquals(5f, range.valueFor(5f, ScaleContext.ofTier(9)));
        assertFalse(range.listsEveryHullValueFor(null));
        assertFalse(range.listsEveryValueFor(ScaleContext.NONE));
        assertEquals(range, new ModifierLine("HULL_MULT", 4f, 6f, null));
        assertEquals(range, new ModifierLine("HULL_MULT", 4f, 6f, ValueScaling.perHullSize(List.of())));
    }

    @Test
    void aTierGroupLinePicksTheValueOfTheGroupTheTierFallsIn() {
        ModifierLine grouped = ModifierLine.perTierGroup("ARMOR_FLAT", List.of(TierGroup.LOW, TierGroup.MID, TierGroup.HIGH), List.of(5f, 10f, 15f));

        assertTrue(grouped.scalesWithTier());
        assertTrue(grouped.isScaled());
        assertFalse(grouped.scalesWithHullSize());
        assertEquals(List.of(), grouped.hullValues());
        assertEquals(List.of(5f, 10f, 15f), grouped.scaledValues());
        assertEquals(5f, grouped.valueFor(0f, ScaleContext.ofTier(1)));
        assertEquals(5f, grouped.valueFor(0f, ScaleContext.ofTier(5)));
        assertEquals(10f, grouped.valueFor(0f, ScaleContext.ofTier(6)));
        assertEquals(15f, grouped.valueFor(0f, ScaleContext.ofTier(15)));
        assertEquals(15f, grouped.valueFor(0f, ScaleContext.of(HullSize.FRIGATE, 40)));
        assertEquals(7f, grouped.valueFor(7f, ScaleContext.NONE));
        assertEquals(7f, grouped.valueFor(7f, HullSize.FRIGATE));
        assertTrue(grouped.listsEveryHullValueFor(HullSize.FRIGATE));
        assertFalse(grouped.listsEveryValueFor(ScaleContext.ofTier(3)));
    }

    @Test
    void aTierBelowTheFirstGroupHasNoGroupValue() {
        ModifierLine grouped = ModifierLine.perTierGroup("ARMOR_FLAT", List.of(TierGroup.MID, TierGroup.HIGH), List.of(5f, 10f));

        assertEquals(2f, grouped.valueFor(2f, ScaleContext.ofTier(5)));
        assertTrue(grouped.listsEveryValueFor(ScaleContext.ofTier(5)));
        assertFalse(grouped.rollsAtTier(5));
        assertEquals(5f, grouped.valueFor(2f, ScaleContext.ofTier(6)));
    }

    @Test
    void tierGroupsNeedOneValueEachAndRunUnbrokenUpToHigh() {
        assertThrows(IllegalArgumentException.class, () -> ValueScaling.perTierGroup(List.of(TierGroup.MID, TierGroup.HIGH), List.of(1f)));
        assertThrows(IllegalArgumentException.class, () -> ValueScaling.perTierGroup(List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> ValueScaling.perTierGroup(List.of(TierGroup.HIGH, TierGroup.HIGH), List.of(1f, 2f)));
        assertThrows(IllegalArgumentException.class, () -> ValueScaling.perTierGroup(List.of(TierGroup.LOW, TierGroup.HIGH), List.of(1f, 2f)));
        assertThrows(IllegalArgumentException.class, () -> ValueScaling.perTierGroup(List.of(TierGroup.LOW, TierGroup.MID), List.of(1f, 2f)));
    }

    @Test
    void renamingAndWideningKeepTheScaling() {
        ModifierLine scaled = ModifierLine.perHullSize("ARMOR_FLAT", HULL_VALUES);
        ModifierLine grouped = ModifierLine.perTierGroup("ARMOR_FLAT", List.of(TierGroup.MID, TierGroup.HIGH), List.of(5f, 10f));

        assertEquals(scaled.scaling(), scaled.named("ARMOR_FLAT_NEW").scaling());
        assertEquals(HULL_VALUES, scaled.widened(0.35f).hullValues());
        assertEquals(grouped.scaling(), grouped.widened(0.35f).scaling());
    }

    @Test
    void hullSizeContextsAreSharedAndCarryNoTier() {
        assertSame(ScaleContext.ofHullSize(HullSize.CRUISER), ScaleContext.ofHullSize(HullSize.CRUISER));
        assertSame(ScaleContext.NONE, ScaleContext.ofHullSize(null));
        assertSame(ScaleContext.NONE, ScaleContext.ofTier(TierRange.NO_TIER));
        assertSame(ScaleContext.ofHullSize(HullSize.FRIGATE), ScaleContext.of(HullSize.FRIGATE, TierRange.NO_TIER));
        assertEquals(TierRange.NO_TIER, ScaleContext.ofHullSize(HullSize.FRIGATE).tier());
    }

    @Test
    void tierRangesAllowEverythingWithoutATier() {
        TierRange fromFive = TierRange.atLeast(5);
        TierRange threeToSix = TierRange.between(3, 6);

        assertTrue(TierRange.ANY.isAny());
        assertTrue(TierRange.ANY.allows(1));
        assertTrue(TierRange.ANY.allows(TierRange.NO_TIER));
        assertFalse(fromFive.allows(4));
        assertTrue(fromFive.allows(5));
        assertTrue(fromFive.allows(TierRange.NO_TIER));
        assertFalse(threeToSix.allows(2));
        assertTrue(threeToSix.allows(6));
        assertFalse(threeToSix.allows(7));
        assertThrows(IllegalArgumentException.class, () -> TierRange.between(0, 3));
        assertThrows(IllegalArgumentException.class, () -> TierRange.between(4, 3));
    }
}
