package exiledsector.charts;

import com.fs.starfarer.api.Global;
import exiledlib.MockSector;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class StarChartsDomainTest {

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
    }

    @Test
    void switchedOffStarChartsStopOnceNoRunIsOpen() {
        StarChartSettings.setEnabledForTests(false);

        assertFalse(new StarChartsDomain().enabled());
    }

    @Test
    void switchedOffStarChartsStayOnWhileARunIsOpenSoItPlaysOut() {
        StarChartSettings.setEnabledForTests(false);
        ChartRuns.store(new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 100f, 200f, 1));

        assertTrue(new StarChartsDomain().enabled());
    }

    @Test
    void switchedOffStarChartsStopInTheMainMenu() {
        StarChartSettings.setEnabledForTests(false);
        mockSector.global.when(Global::getSector).thenReturn(null);

        assertFalse(new StarChartsDomain().enabled());
    }
}
