package exiledlib.modifiers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StretchedTierBandTest {

    private static final ModifierLine FLEET_POWER = ModifierLine.parse("FLEET_POWER:t1=10-15/t6=20-30/t11=-45--35").line();

    @Test
    void positionsInsideTheRollRangeKeepTheirBandValues() {
        for (int tier = 1; tier <= 15; tier++) {
            ScaleContext context = ScaleContext.ofTier(tier);
            float low = FLEET_POWER.valueFor(0f, context);
            float high = FLEET_POWER.valueFor(ModifierLine.TIER_ROLL_POSITIONS, context);
            for (int position = 0; position <= (int) ModifierLine.TIER_ROLL_POSITIONS; position++) {
                float value = FLEET_POWER.valueFor(position, context);
                assertTrue(value >= Math.min(low, high) && value <= Math.max(low, high), "tier " + tier + " position " + position);
            }
        }
        assertEquals(10f, FLEET_POWER.valueFor(0f, ScaleContext.ofTier(1)));
        assertEquals(15f, FLEET_POWER.valueFor(100f, ScaleContext.ofTier(5)));
    }

    @Test
    void positionsPastTheRollRangeStretchTheBandOutwardsByTheirMagnitude() {
        assertEquals(7f, FLEET_POWER.valueFor(-35f, ScaleContext.ofTier(1)));
        assertEquals(20f, FLEET_POWER.valueFor(135f, ScaleContext.ofTier(1)));
        assertEquals(13f, FLEET_POWER.valueFor(-35f, ScaleContext.ofTier(6)));
        assertEquals(41f, FLEET_POWER.valueFor(135f, ScaleContext.ofTier(6)));
        assertEquals(-61f, FLEET_POWER.valueFor(-35f, ScaleContext.ofTier(11)));
        assertEquals(-23f, FLEET_POWER.valueFor(135f, ScaleContext.ofTier(11)));
    }

    @Test
    void aStretchedValueNeverCrossesZero() {
        assertEquals(1f, FLEET_POWER.valueFor(-1000f, ScaleContext.ofTier(1)));
        assertEquals(-1f, FLEET_POWER.valueFor(1000f, ScaleContext.ofTier(11)));
    }

    @Test
    void fixedBandsAndHullSizeValuesIgnoreStretchedPositions() {
        ModifierLine fixed = ModifierLine.parse("EXTRA_FLEETS:t1=1/t8=2").line();
        assertFalse(fixed.canVary());
        assertEquals(1f, fixed.valueFor(-35f, ScaleContext.ofTier(3)));
        assertEquals(2f, fixed.valueFor(135f, ScaleContext.ofTier(9)));
        ValueScaling hullValues = ValueScaling.perHullSize(List.of(5f, 10f, 15f, 20f));
        assertEquals(15f, hullValues.valueAt(2, -0.35f));
        assertEquals(15f, hullValues.valueAt(2, 1.35f));
        ModifierLine stretchedFixed = fixed.stretchedRollPositions(0.35f);
        assertEquals(fixed.min(), stretchedFixed.min());
        assertEquals(fixed.max(), stretchedFixed.max());
    }

    @Test
    void stretchedRollPositionsWidenBothEndsOfTheRollRange() {
        ModifierLine stretched = FLEET_POWER.stretchedRollPositions(0.35f);

        assertEquals(-35f, stretched.min());
        assertEquals(135f, stretched.max());
        assertEquals(FLEET_POWER.scaling(), stretched.scaling());
        assertEquals(FLEET_POWER.effectName(), stretched.effectName());
    }
}
