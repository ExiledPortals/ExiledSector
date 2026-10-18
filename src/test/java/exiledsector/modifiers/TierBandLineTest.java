package exiledsector.modifiers;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.json.CDL;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

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

    private static Map<Float, Integer> valueCounts(ModifierLine line, int tier) {
        Map<Float, Integer> counts = new TreeMap<>();
        for (int position = 0; position <= (int) ModifierLine.TIER_ROLL_POSITIONS; position++) {
            counts.merge(line.valueFor(position, ScaleContext.ofTier(tier)), 1, Integer::sum);
        }
        return counts;
    }

    @Test
    void everyStepOfABandIsAsLikelyAsAnyOtherWithinOnePosition() {
        ModifierLine line = ModifierLine.parse("QUANTITY:t1=4-6/t6=10-15/t11=-25--15").line();

        assertEquals(Map.of(4f, 34, 5f, 34, 6f, 33), valueCounts(line, 1));
        assertEquals(Map.of(10f, 17, 11f, 17, 12f, 17, 13f, 17, 14f, 17, 15f, 16), valueCounts(line, 6));
        Map<Float, Integer> negativeCounts = valueCounts(line, 11);
        assertEquals(11, negativeCounts.size());
        assertEquals(Map.of(9, 9, 10, 2), negativeCounts.values().stream().collect(Collectors.groupingBy(count -> count, Collectors.summingInt(count -> 1))));
        assertEquals(-25f, line.valueFor(0f, ScaleContext.ofTier(11)));
        assertEquals(-15f, line.valueFor(ModifierLine.TIER_ROLL_POSITIONS, ScaleContext.ofTier(11)));
    }

    @Test
    void everyShippedChartBandRollsEachOfItsValuesEvenly() throws Exception {
        String csv = Files.readString(Path.of("data/config/exiledSector/star_chart_modifiers.csv"), StandardCharsets.UTF_8);
        JSONArray rows = CDL.toJSONArray(csv.replace("\r\n", "\n"));
        int checkedBands = 0;
        for (int row = 0; row < rows.length(); row++) {
            for (String entry : rows.getJSONObject(row).getString("lines").split(";")) {
                ModifierLine line = ModifierLine.parse(entry.trim()).line();
                if (!(line.scaling() instanceof ValueScaling.PerTierBand tierBands) || !tierBands.variesWithinAPoint()) {
                    continue;
                }
                for (int band = 0; band < tierBands.bandStartTiers().size(); band++) {
                    Map<Float, Integer> counts = valueCounts(line, tierBands.bandStartTiers().get(band));
                    int stepCount = Math.round(tierBands.highAt(band) - tierBands.lowAt(band)) + 1;
                    assertEquals(stepCount, counts.size(), entry);
                    int fewest = counts.values().stream().mapToInt(Integer::intValue).min().orElseThrow();
                    int most = counts.values().stream().mapToInt(Integer::intValue).max().orElseThrow();
                    assertTrue(most - fewest <= 1, entry + " band " + band + ": " + counts);
                    checkedBands++;
                }
            }
        }
        assertTrue(checkedBands > 20, "checked " + checkedBands);
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
