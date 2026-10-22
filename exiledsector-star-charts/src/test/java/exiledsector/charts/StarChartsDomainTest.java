package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StarChartsDomainTest {

    private MockedStatic<Global> globalMock;
    private Map<String, Object> persistentData;

    @BeforeEach
    void setUp() {
        persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
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
        globalMock.when(Global::getSector).thenReturn(null);

        assertFalse(new StarChartsDomain().enabled());
    }
}
