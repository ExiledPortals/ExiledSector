package exiledsector.modifiers;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TierBandLineTest {

    @Test
    void rangedTierBandsRollAPositionThatEachBandScales() {
        ModifierLine.Parsed parsed = ModifierLine.parse("FLEET_POWER:t1=10-15/t6=20-30/t11=35-45");
        ModifierLine line = parsed.line();

        assertNull(parsed.trailingField());
        assertTrue(line.scalesWithTier());
        assertFalse(line.scalesWithHullSize());
        assertTrue(line.canVary());
        assertEquals(0f, line.min());
        assertEquals(ModifierLine.TIER_ROLL_POSITIONS, line.max());
        assertEquals(10f, line.valueFor(0f, ScaleContext.ofTier(1)));
        assertEquals(15f, line.valueFor(100f, ScaleContext.ofTier(5)));
        assertEquals(25f, line.valueFor(50f, ScaleContext.ofTier(6)));
        assertEquals(45f, line.valueFor(100f, ScaleContext.ofTier(15)));
        assertEquals(50f, line.valueFor(50f, ScaleContext.NONE), "without a tier the roll is not scaled");
        assertTrue(line.listsEveryValueFor(ScaleContext.NONE));
        assertEquals(List.of(10f, 20f, 35f), line.scaledValues());
    }

    @Test
    void rollsStayInsideTheirPositionRange() {
        ModifierLine line = ModifierLine.parse("FLEET_POWER:t1=10-15/t6=20-30").line();
        Random random = new Random(4);
        for (int roll = 0; roll < 200; roll++) {
            float position = line.roll(random);
            assertTrue(position >= 0f && position <= ModifierLine.TIER_ROLL_POSITIONS, "position " + position);
            float value = line.valueFor(position, ScaleContext.ofTier(7));
            assertTrue(value >= 20f && value <= 30f, "value " + value);
        }
        assertEquals(50f, line.average());
    }

    @Test
    void fixedTierBandsGiveOneValuePerBand() {
        ModifierLine line = ModifierLine.parse("EXTRA_FLEETS:t1=1/t8=2/t13=3:5").line();

        assertFalse(line.canVary());
        assertEquals(1f, line.valueFor(1f, ScaleContext.ofTier(7)));
        assertEquals(2f, line.valueFor(1f, ScaleContext.ofTier(8)));
        assertEquals(3f, line.valueFor(1f, ScaleContext.ofTier(15)));
        assertEquals("5", ModifierLine.parse("EXTRA_FLEETS:t1=1/t8=2/t13=3:5").trailingField());
    }

    @Test
    void negativeRangesAndTheirBandRangeForShowingRolls() {
        ModifierLine line = ModifierLine.parse("SENSOR:t1=-25--15/t6=-35--25").line();

        assertEquals(-25f, line.valueFor(0f, ScaleContext.ofTier(2)));
        assertEquals(-15f, line.valueFor(100f, ScaleContext.ofTier(2)));
        assertEquals(ModifierLine.range("SENSOR", -35f, -25f), line.rangeFor(ScaleContext.ofTier(9)));
        assertSame(line, line.rangeFor(ScaleContext.NONE));
        ModifierLine plain = ModifierLine.range("HULL", 1f, 2f);
        assertSame(plain, plain.rangeFor(ScaleContext.ofTier(3)));
    }

    @Test
    void malformedTierBandsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ModifierLine.parse("X:t3=1-2"), "the first band must start at t1");
        assertThrows(IllegalArgumentException.class, () -> ModifierLine.parse("X:t1=1-2/t1=3-4"), "bands must rise");
        assertThrows(IllegalArgumentException.class, () -> ModifierLine.parse("X:t1=one"));
        assertThrows(IllegalArgumentException.class, () -> ModifierLine.parse("X:t1=1-2:5:6"));
        assertThrows(IllegalArgumentException.class, () -> ModifierLine.parse("X:t1=1-2/6=3"));
    }

    @Test
    void theExistingFormatsParseAsBefore() {
        assertEquals(ModifierLine.range("HULL_MULT", 4f, 6f), ModifierLine.parse("HULL_MULT:4:6").line());
        assertEquals("10", ModifierLine.parse("HULL_MULT:4:6:10").trailingField());
        ModifierLine hullValues = ModifierLine.parse("ARMOR_FLAT:10/15/20/30").line();
        assertTrue(hullValues.scalesWithHullSize());
        assertEquals(20f, hullValues.valueFor(0f, HullSize.CRUISER));
        assertFalse(ModifierLine.isTierBandText("10/15/20/30"));
        assertTrue(ModifierLine.isTierBandText("t1=10"));
    }

    @Test
    void tierScaleContextsAreCached() {
        assertSame(ScaleContext.ofTier(7), ScaleContext.ofTier(7));
        assertSame(ScaleContext.ofTier(7), ScaleContext.of(null, 7));
        assertSame(ScaleContext.NONE, ScaleContext.ofTier(TierRange.NO_TIER));
        assertEquals(new ScaleContext(HullSize.FRIGATE, 3), ScaleContext.of(HullSize.FRIGATE, 3));
    }
}
