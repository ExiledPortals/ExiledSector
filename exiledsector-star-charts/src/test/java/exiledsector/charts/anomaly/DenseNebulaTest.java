package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.MockSector;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.atlas.AtlasNodeText;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DenseNebulaTest {

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

    @Test
    void theNebulaReplacesTheStarWithANebulaSystem() {
        assertSame(DenseNebula.INSTANCE, ChartAnomalies.role(DenseNebula.ANOMALY_ID, StarRule.class));
        assertEquals(StarSystemType.NEBULA, DenseNebula.INSTANCE.systemType());
    }

    @Test
    void eachBattleWonInTheNebulaAddsADustStackUpToTheCap() {
        StarSystemAPI system = LiveAnomalyRuns.systemWith(DenseNebula.ANOMALY_ID);
        ChartRun run = ChartRuns.currentOrNull();

        winBattles(system, 3);
        assertEquals(3, DenseNebula.dust(run));
        assertEquals(3 * DenseNebula.STAR_CHART_PERCENT_PER_DUST, ChartAnomalies.bossStarChartPercent(run));

        winBattles(system, DenseNebula.MAX_DUST);
        assertEquals(DenseNebula.MAX_DUST, DenseNebula.dust(run));
        assertEquals(DenseNebula.MAX_DUST * DenseNebula.STAR_CHART_PERCENT_PER_DUST, ChartAnomalies.bossStarChartPercent(run));
    }

    @Test
    void battlesOutsideTheNebulaOrInAnotherAnomalysSystemAddNoDust() {
        StarSystemAPI system = LiveAnomalyRuns.systemWith(StellarFlares.ANOMALY_ID);
        ChartRun run = ChartRuns.currentOrNull();

        winBattles(system, 2);
        winBattles(mock(StarSystemAPI.class), 2);

        assertEquals(0, DenseNebula.dust(run));
        assertEquals(0f, ChartAnomalies.bossStarChartPercent(run));
    }

    @Test
    void theDustLivesOnlyInItsRun() {
        StarSystemAPI system = LiveAnomalyRuns.systemWith(DenseNebula.ANOMALY_ID);
        winBattles(system, 2);

        LiveAnomalyRuns.systemWith(DenseNebula.ANOMALY_ID);

        assertEquals(0, DenseNebula.dust(ChartRuns.currentOrNull()));
    }

    @Test
    void theIntelAndTheAtlasShowTheDust() {
        StarSystemAPI system = LiveAnomalyRuns.systemWith(DenseNebula.ANOMALY_ID);
        winBattles(system, 2);

        String intel = DenseNebula.INSTANCE.intelText(new AnomalyRun(ChartRuns.currentOrNull(), system));
        String body = AtlasNodeText.anomalyBody(DenseNebula.ANOMALY_ID).plain();

        assertTrue(intel.contains("2 of " + DenseNebula.MAX_DUST) && intel.contains("20%"), intel);
        assertTrue(body.contains("Settling Dust") && body.contains("up to " + DenseNebula.MAX_DUST), body);
    }
}
