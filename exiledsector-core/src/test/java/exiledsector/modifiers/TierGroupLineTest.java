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

class TierGroupLineTest {

    @Test
    void rangedTierGroupsRollAPositionThatEachGroupScales() {
        ModifierLine.Parsed parsed = ModifierLine.parse("FLEET_POWER:low=10-15/mid=20-30/high=35-45");
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
        assertEquals(30f, line.valueFor(100f, ScaleContext.ofTier(10)));
        assertEquals(35f, line.valueFor(0f, ScaleContext.ofTier(11)));
        assertEquals(45f, line.valueFor(100f, ScaleContext.ofTier(16)));
        assertEquals(50f, line.valueFor(50f, ScaleContext.NONE), "without a tier the roll is not scaled");
        assertTrue(line.listsEveryValueFor(ScaleContext.NONE));
        assertEquals(List.of(10f, 20f, 35f), line.scaledValues());
    }

    @Test
    void aLineLeavingOutTheLowGroupOnlyRollsFromTheMidGroup() {
        ModifierLine line = ModifierLine.parse("COLONY_ITEMS:mid=40-60/high=90-120").line();
        Modifier modifier = Modifier.single(ModifierSlot.SUFFIX, line);

        assertFalse(line.rollsAtTier(5));
        assertFalse(modifier.rollsAtTier(1));
        assertTrue(modifier.rollsAtTier(6));
        assertTrue(modifier.rollsAtTier(16));
        assertTrue(modifier.rollsAtTier(TierRange.NO_TIER));
        assertTrue(line.listsEveryValueFor(ScaleContext.ofTier(3)));
        assertEquals(40f, line.valueFor(0f, ScaleContext.ofTier(6)));
        assertEquals(120f, line.valueFor(100f, ScaleContext.ofTier(11)));
    }

    @Test
    void aModifierRollsOnlyWhereEveryTierGroupedLineHasAValue() {
        Modifier hybrid = new Modifier("HYBRID", ModifierSlot.PREFIX, List.of(ModifierLine.parse("A:low=1-2/mid=3-4/high=5-6").line(),
                ModifierLine.parse("B:high=7-8").line(), ModifierLine.range("C", 1f, 2f)));

        assertFalse(hybrid.rollsAtTier(10));
        assertTrue(hybrid.rollsAtTier(11));
    }

    @Test
    void rollsStayInsideTheirPositionRange() {
        ModifierLine line = ModifierLine.parse("FLEET_POWER:mid=20-30/high=35-45").line();
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
    void everyStepOfAGroupIsAsLikelyAsAnyOtherWithinOnePosition() {
        ModifierLine line = ModifierLine.parse("QUANTITY:low=4-6/mid=10-15/high=-25--15").line();

        assertEquals(Map.of(4f, 34, 5f, 34, 6f, 33), valueCounts(line, 1));
        assertEquals(Map.of(10f, 17, 11f, 17, 12f, 17, 13f, 17, 14f, 17, 15f, 16), valueCounts(line, 6));
        Map<Float, Integer> negativeCounts = valueCounts(line, 11);
        assertEquals(11, negativeCounts.size());
        assertEquals(Map.of(9, 9, 10, 2), negativeCounts.values().stream().collect(Collectors.groupingBy(count -> count, Collectors.summingInt(count -> 1))));
        assertEquals(-25f, line.valueFor(0f, ScaleContext.ofTier(11)));
        assertEquals(-15f, line.valueFor(ModifierLine.TIER_ROLL_POSITIONS, ScaleContext.ofTier(11)));
    }

    @Test
    void everyShippedChartGroupRollsEachOfItsValuesEvenly() throws Exception {
        String csv = Files.readString(Path.of("data/config/exiledSector/star_chart_modifiers.csv"), StandardCharsets.UTF_8);
        JSONArray rows = CDL.toJSONArray(csv.replace("\r\n", "\n"));
        int checkedGroups = 0;
        for (int row = 0; row < rows.length(); row++) {
            for (String entry : rows.getJSONObject(row).getString("lines").split(";")) {
                ModifierLine line = ModifierLine.parse(entry.trim()).line();
                if (!(line.scaling() instanceof ValueScaling.PerTierGroup tierGroups) || !tierGroups.variesWithinAPoint()) {
                    continue;
                }
                for (int group = 0; group < tierGroups.groups().size(); group++) {
                    Map<Float, Integer> counts = valueCounts(line, tierGroups.groups().get(group).tiers().minTier());
                    int stepCount = Math.round(tierGroups.highAt(group) - tierGroups.lowAt(group)) + 1;
                    assertEquals(stepCount, counts.size(), entry);
                    int fewest = counts.values().stream().mapToInt(Integer::intValue).min().orElseThrow();
                    int most = counts.values().stream().mapToInt(Integer::intValue).max().orElseThrow();
                    assertTrue(most - fewest <= 1, entry + " group " + group + ": " + counts);
                    checkedGroups++;
                }
            }
        }
        assertTrue(checkedGroups > 20, "checked " + checkedGroups);
    }

    @Test
    void fixedTierGroupsGiveOneValuePerGroup() {
        ModifierLine line = ModifierLine.parse("EXTRA_FLEETS:low=1/mid=2/high=3:5").line();

        assertFalse(line.canVary());
        assertEquals(1f, line.valueFor(1f, ScaleContext.ofTier(5)));
        assertEquals(2f, line.valueFor(1f, ScaleContext.ofTier(6)));
        assertEquals(3f, line.valueFor(1f, ScaleContext.ofTier(15)));
        assertEquals("5", ModifierLine.parse("EXTRA_FLEETS:low=1/mid=2/high=3:5").trailingField());
    }

    @Test
    void negativeRangesAndTheirGroupRangeForShowingRolls() {
        ModifierLine line = ModifierLine.parse("SENSOR:low=-25--15/mid=-35--25/high=-45--35").line();

        assertEquals(-25f, line.valueFor(0f, ScaleContext.ofTier(2)));
        assertEquals(-15f, line.valueFor(100f, ScaleContext.ofTier(2)));
        assertEquals(ModifierLine.range("SENSOR", -35f, -25f), line.rangeFor(ScaleContext.ofTier(9)));
        assertSame(line, line.rangeFor(ScaleContext.NONE));
        ModifierLine plain = ModifierLine.range("HULL", 1f, 2f);
        assertSame(plain, plain.rangeFor(ScaleContext.ofTier(3)));
    }

    @Test
    void malformedTierGroupsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ModifierLine.parse("X:low=1-2/mid=3-4"), "groups must reach high");
        assertThrows(IllegalArgumentException.class, () -> ModifierLine.parse("X:low=1-2/high=3-4"), "groups must not skip one");
        assertThrows(IllegalArgumentException.class, () -> ModifierLine.parse("X:mid=1-2/low=3-4/high=5-6"), "groups must rise");
        assertThrows(IllegalArgumentException.class, () -> ModifierLine.parse("X:high=1-2/high=3-4"), "groups must not repeat");
        assertThrows(IllegalArgumentException.class, () -> ModifierLine.parse("X:t1=1-2"), "tier numbers are not groups");
        assertThrows(IllegalArgumentException.class, () -> ModifierLine.parse("X:high=one"));
        assertThrows(IllegalArgumentException.class, () -> ModifierLine.parse("X:high=1-2:5:6"));
        assertThrows(IllegalArgumentException.class, () -> ModifierLine.parse("X:mid=1-2/3"));
    }

    @Test
    void theExistingFormatsParseAsBefore() {
        assertEquals(ModifierLine.range("HULL_MULT", 4f, 6f), ModifierLine.parse("HULL_MULT:4:6").line());
        assertEquals("10", ModifierLine.parse("HULL_MULT:4:6:10").trailingField());
        ModifierLine hullValues = ModifierLine.parse("ARMOR_FLAT:10/15/20/30").line();
        assertTrue(hullValues.scalesWithHullSize());
        assertEquals(20f, hullValues.valueFor(0f, HullSize.CRUISER));
        assertFalse(ModifierLine.isTierGroupText("10/15/20/30"));
        assertTrue(ModifierLine.isTierGroupText("low=10"));
    }

    @Test
    void tierScaleContextsAreCached() {
        assertSame(ScaleContext.ofTier(7), ScaleContext.ofTier(7));
        assertSame(ScaleContext.ofTier(7), ScaleContext.of(null, 7));
        assertSame(ScaleContext.NONE, ScaleContext.ofTier(TierRange.NO_TIER));
        assertEquals(new ScaleContext(HullSize.FRIGATE, 3), ScaleContext.of(HullSize.FRIGATE, 3));
    }
}
