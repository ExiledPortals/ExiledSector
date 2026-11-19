package exiledsector.charts.anomaly.magneticstorm;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.MagneticStorm;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.loot.DropType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagneticStormLinesTest {

    private static final float TOLERANCE = 0.001f;

    @BeforeEach
    void setUp() throws Exception {
        assertEquals(List.of(), ChartAnomalies.register(ChartTables.csvRows(ChartAnomalies.PATH)));
    }

    @AfterEach
    void tearDown() {
        ChartAnomalies.resetForTests();
    }

    private static ChartGenContext tree(AppliedEffect... lines) {
        return ChartGenContext.of(List.of(lines));
    }

    @Test
    void theAnomalyItselfAddsTwentyPercentCraftingItems() {
        assertEquals(List.of(new AppliedEffect(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, 20f)),
                ChartAnomalies.definition(MagneticStorm.ANOMALY_ID).effects());
    }

    @Test
    void chargedFieldsBringsAMagneticStormBackToFiftyPercentCraftingItems() {
        ChartGenContext context = tree(new AppliedEffect(ChartModifier.CHART_MAGNETIC_STORM_CRAFTING_ITEM_PERCENT, 30f))
                .withAnomalyEffects(List.of(MagneticStorm.ANOMALY_ID));

        assertEquals(50f, context.dropPercent(DropType.CRAFTING_ITEM), TOLERANCE);
    }

    @Test
    void theStormDropLinesCountOnlyInChartsWithAMagneticStorm() {
        ChartGenContext lines = tree(new AppliedEffect(ChartModifier.CHART_MAGNETIC_STORM_CRAFTING_ITEM_PERCENT, 10f),
                new AppliedEffect(ChartModifier.CHART_MAGNETIC_STORM_FEEDSTOCK_PERCENT, 30f));

        assertEquals(0f, lines.dropPercent(DropType.CRAFTING_ITEM), TOLERANCE);
        assertEquals(0f, lines.dropPercent(DropType.FEEDSTOCK), TOLERANCE);
        ChartGenContext withoutStorm = lines.withAnomalyEffects(List.of(SpatialBreach.ANOMALY_ID));
        assertEquals(0f, withoutStorm.dropPercent(DropType.CRAFTING_ITEM), TOLERANCE);
        assertEquals(0f, withoutStorm.dropPercent(DropType.FEEDSTOCK), TOLERANCE);
        ChartGenContext withStorm = lines.withAnomalyEffects(List.of(SpatialBreach.ANOMALY_ID, MagneticStorm.ANOMALY_ID));
        assertEquals(30f, withStorm.dropPercent(DropType.CRAFTING_ITEM), TOLERANCE);
        assertEquals(30f, withStorm.dropPercent(DropType.FEEDSTOCK), TOLERANCE);
    }

    @Test
    void theStormDropLinesAreNeverTakenForTheirDropTypesOwnLine() {
        assertEquals(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, ChartModifier.forDropType(DropType.CRAFTING_ITEM));
        assertEquals(ChartModifier.CHART_FEEDSTOCK_PERCENT, ChartModifier.forDropType(DropType.FEEDSTOCK));
        assertNull(ChartModifier.CHART_MAGNETIC_STORM_CRAFTING_ITEM_PERCENT.dropType());
        assertNull(ChartModifier.CHART_MAGNETIC_STORM_CRAFTING_ITEM_PERCENT.dropLineWith(List.of(SpatialBreach.ANOMALY_ID), 10f));
        assertNull(ChartModifier.CHART_CRAFTING_ITEM_PERCENT.dropLineWith(List.of(MagneticStorm.ANOMALY_ID), 10f));
    }

    @Test
    void theTuningReadsTheIonFrontAndGeomagneticSurgeLines() {
        assertEquals(MagneticStormTuning.NONE, MagneticStormTuning.of(ChartGenContext.none()));
        assertEquals(MagneticStormTuning.NONE, MagneticStormTuning.of(null));
        assertFalse(MagneticStormTuning.NONE.setsOffAuroras());

        MagneticStormTuning ionFront = MagneticStormTuning.of(tree(new AppliedEffect(ChartModifier.CHART_MAGNETIC_STORM_ION_FRONT, 1f),
                new AppliedEffect(ChartModifier.CHART_MAGNETIC_STORM_ION_FRONT_CRAFTING_ITEM_PERCENT, 50f)));
        assertEquals(new MagneticStormTuning(true, 50f, false, 0), ionFront);
        assertTrue(ionFront.setsOffAuroras());

        MagneticStormTuning surge = MagneticStormTuning.of(tree(new AppliedEffect(ChartModifier.CHART_MAGNETIC_STORM_FLARE_AURORAS, 1f),
                new AppliedEffect(ChartModifier.CHART_MAGNETIC_STORM_LUCKY_MODIFIERS, 1f)));
        assertEquals(new MagneticStormTuning(false, 0f, true, 1), surge);
        assertTrue(surge.setsOffAuroras());
    }

    @Test
    void ionFrontAddsCraftingItemsOnlyToBattlesTheMagneticStormHolds() {
        ChartGenContext context = tree(new AppliedEffect(ChartModifier.CHART_MAGNETIC_STORM_ION_FRONT_CRAFTING_ITEM_PERCENT, 50f));

        assertEquals(50f, MagneticStorm.INSTANCE.battleCraftingItemPercent(context), TOLERANCE);
        assertEquals(0f, MagneticStorm.INSTANCE.battleCraftingItemPercent(ChartGenContext.none()), TOLERANCE);
        assertEquals(0f, context.dropPercent(DropType.CRAFTING_ITEM), TOLERANCE);
    }
}
