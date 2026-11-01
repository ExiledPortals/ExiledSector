package exiledsector.ui.chart;

import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.atlas.AtlasNames;
import exiledsector.charts.content.ChartBossType;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.loot.ChartLootTotals;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemData;
import exiledsector.items.StarChart;
import exiledsector.items.StarChartFixtures;
import exiledsector.ui.socket.AtlasAnomalyPanel.Entry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartInfoTextTest {

    private static final String RUN_ID = "infoRun";

    private MockSector mockSector;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        mockSector = new MockSector();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        StarChartFixtures.clear();
    }

    private static ChartRun run(List<String> anomalies) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(7, "hegemony", "boss", ChartBossType.FLEET, 4, 6, anomalies));
        return run;
    }

    @Test
    void anomaliesAreListedInRunOrderUnderTheirAtlasNames() {
        ChartRun run = run(List.of("magnetic_storm", "black_hole"));

        List<String> titles = ChartInfoText.anomalies(run).stream().map(Entry::title).toList();

        assertEquals(List.of(AtlasNames.anomaly("magnetic_storm"), AtlasNames.anomaly("black_hole")), titles);
    }

    @Test
    void theKeyChangesForOtherAnomaliesOrOnceTheObjectiveIsDecided() {
        ChartRun run = run(List.of("magnetic_storm"));
        ChartInfoText.Key key = ChartInfoText.Key.of(run);

        assertTrue(key.matches(run));
        assertFalse(key.matches(run(List.of("black_hole"))));
        run.markObjectiveComplete();
        assertFalse(key.matches(run));
    }

    @Test
    void theObjectiveNamesTheBossAndShowsTheRewardUntilTheRunEnds() {
        ChartRun run = run(List.of());

        ChartInfoText.Objective objective = ChartInfoText.objective(run);

        assertEquals(Translation.gameText("chart.objective.boss.name"), objective.name());
        assertEquals(Translation.gameText("chart.objective.boss.fleet.description") + "\n" + Translation.gameText("chart.intel.reward"),
                objective.body().plain());
        run.markObjectiveComplete();
        assertTrue(ChartInfoText.objective(run).body().plain().endsWith(Translation.gameText("chart.intel.complete")));
    }

    @Test
    void aRunWithoutABossHasNoObjective() {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(7, "hegemony", null, 4, 6));

        assertNull(ChartInfoText.objective(run));
    }

    @Test
    void theChartsNameRewardsAndModifiersComeFromTheRunsChart() {
        StarChart chart = StarChartFixtures.fullChart(StarChartFixtures.TEST_FIXED, 9L, 6);
        ChartRun run = run(List.of());
        run.recordChart(ItemData.of(chart).encode());

        ChartInfoText.Content content = ChartInfoText.of(run);

        List<StyledText> modifiers = new ArrayList<>(chart.implicitLines(false));
        modifiers.addAll(chart.effectLines());
        assertEquals(chart.displayName().title(), content.chartName().title());
        assertEquals(modifiers, content.modifierLines());
        assertFalse(content.modifierLines().isEmpty());
        assertEquals(ChartLootTotals.lines(ChartRunEffects.contextOf(run)), content.rewardLines());
    }

    @Test
    void aRunWithoutAChartHasNoNameRewardsOrModifiers() {
        ChartInfoText.Content content = ChartInfoText.of(run(List.of()));

        assertNull(content.chartName());
        assertTrue(content.rewardLines().isEmpty());
        assertTrue(content.modifierLines().isEmpty());
    }
}
