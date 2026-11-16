package exiledsector.charts.anomaly.debris;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnomalousDebrisEffectsTest {

    @Test
    void theTuningLinesAddFieldsAndRaiseTheEngramsDropped() {
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_EXTRA_ANOMALOUS_DEBRIS, 2f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_EXTRA_ANOMALOUS_DEBRIS, 1f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_ANOMALOUS_DEBRIS_CRAFTING_ITEM_QUANTITY_PERCENT, 50f)));

        assertEquals(3, Math.round(context.anomalyPercent(AnomalousDebris.EXTRA_KEY)));
        assertEquals(1.5f, context.anomalyMult(AnomalousDebris.CRAFTING_ITEM_KEY), 0.0001f);
        assertEquals(1f, ChartGenContext.none().anomalyMult(AnomalousDebris.CRAFTING_ITEM_KEY), 0.0001f);
    }

    private static DebrisTuning tuning(Object... effectsAndMagnitudes) {
        List<ChartGenContext.AppliedEffect> applied = new ArrayList<>();
        for (int index = 0; index < effectsAndMagnitudes.length; index += 2) {
            applied.add(new ChartGenContext.AppliedEffect((ChartModifier) effectsAndMagnitudes[index], ((Number) effectsAndMagnitudes[index + 1]).floatValue()));
        }
        return DebrisTuning.of(ChartGenContext.of(applied));
    }

    @Test
    void withoutTreeLinesTheFleetCorruptsOnceAndDropsOneEngram() {
        DebrisTuning plain = DebrisTuning.of(null);

        assertEquals(0, plain.forcedCorruptions());
        assertEquals(1, plain.mostCorruptions());
        assertEquals(0, plain.engramsFor(0));
        assertEquals(1, plain.engramsFor(1));
        assertEquals(1, plain.engramsFor(2));
        assertEquals(0f, plain.increasedSizePercent(1), 0.0001f);
        assertFalse(plain.sleeperShares().any());
        assertFalse(plain.singleField() || plain.breachField() || plain.awake() || plain.roaming());
    }

    @Test
    void deepCorruptionGrowsOnlyCorruptedFleetsAndAddsAnEngram() {
        DebrisTuning deep = tuning(ChartModifier.CHART_ANOMALOUS_DEBRIS_CORRUPTED_FLEET_SIZE_PERCENT, 30, ChartModifier.CHART_ANOMALOUS_DEBRIS_CORRUPTED_ENGRAMS, 1);

        assertEquals(0f, deep.increasedSizePercent(0), 0.0001f);
        assertEquals(30f, deep.increasedSizePercent(1), 0.0001f);
        assertEquals(0, deep.engramsFor(0));
        assertEquals(2, deep.engramsFor(1));
    }

    @Test
    void doubleDoseAllowsASecondCorruptionThatDropsTwoMoreEngrams() {
        DebrisTuning doubleDose = tuning(ChartModifier.CHART_ANOMALOUS_DEBRIS_SECOND_CORRUPTION, 1, ChartModifier.CHART_ANOMALOUS_DEBRIS_TWICE_CORRUPTED_ENGRAMS, 2);

        assertEquals(2, doubleDose.mostCorruptions());
        assertEquals(0, doubleDose.forcedCorruptions());
        assertEquals(1, doubleDose.engramsFor(1));
        assertEquals(3, doubleDose.engramsFor(2));
    }

    @Test
    void loneSleeperPlacesOneBigFleetThatDropsThreeMoreEngramsWhenCorrupted() {
        DebrisTuning lone = tuning(ChartModifier.CHART_ANOMALOUS_DEBRIS_SINGLE_FIELD, 1, ChartModifier.CHART_ANOMALOUS_DEBRIS_FLEET_SIZE_PERCENT, 100,
                ChartModifier.CHART_ANOMALOUS_DEBRIS_CORRUPTED_ENGRAMS, 3);

        assertTrue(lone.singleField());
        assertEquals(100f, lone.increasedSizePercent(0), 0.0001f);
        assertEquals(4, lone.engramsFor(1));
    }

    @Test
    void restlessSleepersWakeCorruptedAndSilentChoirCorruptsTwiceEvenWithThem() {
        DebrisTuning restless = tuning(ChartModifier.CHART_ANOMALOUS_DEBRIS_AWAKE, 1, ChartModifier.CHART_ANOMALOUS_DEBRIS_ROAMING, 1);
        DebrisTuning choir = tuning(ChartModifier.CHART_ANOMALOUS_DEBRIS_ALWAYS_TWICE_CORRUPTED, 1, ChartModifier.CHART_ANOMALOUS_DEBRIS_FLEET_SIZE_PERCENT, 50);
        DebrisTuning both = tuning(ChartModifier.CHART_ANOMALOUS_DEBRIS_AWAKE, 1, ChartModifier.CHART_ANOMALOUS_DEBRIS_ROAMING, 1,
                ChartModifier.CHART_ANOMALOUS_DEBRIS_ALWAYS_TWICE_CORRUPTED, 1);

        assertTrue(restless.awake() && restless.roaming());
        assertEquals(1, restless.forcedCorruptions());
        assertEquals(2, choir.forcedCorruptions());
        assertEquals(2, choir.mostCorruptions());
        assertEquals(50f, choir.increasedSizePercent(2), 0.0001f);
        assertEquals(2, both.forcedCorruptions());
        assertTrue(both.awake());
    }

    @Test
    void theSleeperLinesStackIntoOneShareEach() {
        DebrisTuning sleepers = tuning(ChartModifier.CHART_ANOMALOUS_DEBRIS_REMNANT_SHIP_SHARE, 30, ChartModifier.CHART_ANOMALOUS_DEBRIS_THREAT_SHIP_SHARE, 30,
                ChartModifier.CHART_ANOMALOUS_DEBRIS_DWELLER_SHIP_SHARE, 30, ChartModifier.CHART_ANOMALOUS_DEBRIS_BREACH_FIELD, 1);

        assertEquals(new SleeperShips.Shares(30f, 30f, 30f), sleepers.sleeperShares());
        assertTrue(sleepers.breachField());
    }
}
