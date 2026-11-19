package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.MockSector;
import exiledsector.charts.anomaly.stormfront.StormFront;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlingDustTest {

    private static final float TOLERANCE = 0.01f;

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        ChartAnomalies.resetForTests();
    }

    private static void winBattles(StarSystemAPI system, int battles) {
        for (int i = 0; i < battles; i++) {
            ChartAnomalies.battleWon(system);
        }
    }

    private static ChartRun withTreeLines(AppliedEffect... lines) {
        ChartRun run = ChartRuns.currentOrNull();
        run.recordAtlasTreeLines(List.of(lines));
        return run;
    }

    private static AppliedEffect line(ChartModifier effect, float value) {
        return new AppliedEffect(effect, value);
    }

    @Test
    void stormChartsGatherNoDustWithoutIonizedDust() {
        StarSystemAPI system = LiveAnomalyRuns.systemWith(MagneticStorm.ANOMALY_ID);
        ChartRun run = ChartRuns.currentOrNull();

        winBattles(system, 3);

        assertEquals(0, SettlingDust.storedStacks(run));
        assertEquals(0f, ChartAnomalies.bossStarChartPercent(run));
        assertNull(SettlingDust.intelText(run));
    }

    @Test
    void ionizedDustMakesMagneticStormsAndStormFrontsGatherDust() {
        for (String stormId : List.of(MagneticStorm.ANOMALY_ID, StormFront.ANOMALY_ID)) {
            StarSystemAPI system = LiveAnomalyRuns.systemWith(stormId);
            ChartRun run = withTreeLines(line(ChartModifier.CHART_SETTLING_DUST_IONIZED, 1f));

            winBattles(system, 2);

            assertEquals(2, SettlingDust.stacks(run), stormId);
            assertEquals(2 * SettlingDust.STAR_CHART_PERCENT_PER_STACK, ChartAnomalies.bossStarChartPercent(run), TOLERANCE, stormId);
            String intel = SettlingDust.intelText(run);
            assertNotNull(intel, stormId);
            assertTrue(intel.contains("2 of " + SettlingDust.BASE_MAX_STACKS), intel);
        }
    }

    @Test
    void ionizedDustDoesNothingInChartsWithoutAStormOrNebula() {
        StarSystemAPI system = LiveAnomalyRuns.systemWith(StellarFlares.ANOMALY_ID);
        ChartRun run = withTreeLines(line(ChartModifier.CHART_SETTLING_DUST_IONIZED, 1f));

        winBattles(system, 2);

        assertFalse(SettlingDust.gathersIn(run, SettlingDustTuning.ofRun(run)));
        assertEquals(0, SettlingDust.stacks(run));
    }

    @Test
    void aNebulaWithAStormStillAddsOneStackForEachBattle() {
        StarSystemAPI system = LiveAnomalyRuns.systemWith(DenseNebula.ANOMALY_ID, MagneticStorm.ANOMALY_ID);
        ChartRun run = withTreeLines(line(ChartModifier.CHART_SETTLING_DUST_IONIZED, 1f));

        winBattles(system, 3);

        assertEquals(3, SettlingDust.stacks(run));
        assertEquals(1, ChartAnomalies.intelLines(new AnomalyRun(run, system)).stream().filter(text -> text.contains("Settling Dust")).count());
    }

    @Test
    void extraMaximumStacksRaiseTheCap() {
        StarSystemAPI system = LiveAnomalyRuns.systemWith(DenseNebula.ANOMALY_ID);
        ChartRun run = withTreeLines(line(ChartModifier.CHART_SETTLING_DUST_MAX_STACKS, 1f), line(ChartModifier.CHART_SETTLING_DUST_MAX_STACKS, 5f));

        winBattles(system, SettlingDust.BASE_MAX_STACKS + 10);

        assertEquals(SettlingDust.BASE_MAX_STACKS + 6, SettlingDust.stacks(run));
        assertTrue(SettlingDust.intelText(run).contains("of " + (SettlingDust.BASE_MAX_STACKS + 6)));
    }

    @Test
    void increasedEffectScalesTheBosssStarChartsAndDustTrails() {
        StarSystemAPI system = LiveAnomalyRuns.systemWith(DenseNebula.ANOMALY_ID);
        ChartRun run = withTreeLines(line(ChartModifier.CHART_SETTLING_DUST_EFFECT_PERCENT, 20f),
                line(ChartModifier.CHART_SETTLING_DUST_QUANTITY_PERCENT, 5f));
        float quantityBeforeDust = ChartRunEffects.contextOf(run).quantityPercent();

        winBattles(system, 3);

        assertEquals(3 * SettlingDust.STAR_CHART_PERCENT_PER_STACK * 1.2f, ChartAnomalies.bossStarChartPercent(run), TOLERANCE);
        assertEquals(3 * 5f * 1.2f, SettlingDust.quantityPercent(run, SettlingDustTuning.ofRun(run)), TOLERANCE);
        assertEquals(3 * 5f * 1.2f, ChartRunEffects.contextOf(run).quantityPercent() - quantityBeforeDust, TOLERANCE);
        assertTrue(SettlingDust.intelText(run).contains("18% increased quantity of items found"), SettlingDust.intelText(run));
    }

    @Test
    void withoutDustTrailsTheDustAddsNoItemQuantity() {
        StarSystemAPI system = LiveAnomalyRuns.systemWith(DenseNebula.ANOMALY_ID);
        ChartRun run = ChartRuns.currentOrNull();
        float quantityBeforeDust = ChartRunEffects.contextOf(run).quantityPercent();

        winBattles(system, 3);

        assertEquals(quantityBeforeDust, ChartRunEffects.contextOf(run).quantityPercent(), TOLERANCE);
        assertFalse(SettlingDust.intelText(run).contains("items found"));
    }

    @Test
    void theTuningReadsEveryLine() {
        LiveAnomalyRuns.systemWith(DenseNebula.ANOMALY_ID);
        ChartRun run = withTreeLines(line(ChartModifier.CHART_SETTLING_DUST_EFFECT_PERCENT, 20f), line(ChartModifier.CHART_SETTLING_DUST_MAX_STACKS, 1f),
                line(ChartModifier.CHART_SETTLING_DUST_QUANTITY_PERCENT, 5f), line(ChartModifier.CHART_SETTLING_DUST_FLEET_SIZE_PERCENT, 3f),
                line(ChartModifier.CHART_SETTLING_DUST_IONIZED, 1f));

        SettlingDustTuning tuning = SettlingDustTuning.ofRun(run);

        assertEquals(new SettlingDustTuning(20f, 1, 5f, 3f, true), tuning);
        assertEquals(SettlingDust.BASE_MAX_STACKS + 1, tuning.maxStacks());
        assertEquals(1.2f, tuning.effectMult(), TOLERANCE);
        assertEquals(new SettlingDustTuning(0f, 0, 0f, 0f, false), SettlingDustTuning.NONE);
    }
}
