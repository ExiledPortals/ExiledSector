package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledlib.MockSector;
import exiledsector.ui.chart.StarChartBrowser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class ModificationBayModeTest {

    private static final SocketPanelPlacement SIDE_BY_SIDE_AREA = new SocketPanelPlacement(40f, 40f, 1840f, 1000f, true);

    private final AtomicReference<BenchMode> openedMode = new AtomicReference<>();
    private final AtomicReference<SocketWorkbenchPanel.Listener> benchListener = new AtomicReference<>();
    private MockSector mockSector;
    private MockedStatic<SocketWorkbenchPanel> workbenchMock;
    private MockedStatic<ItemStoragePanel> itemStorageMock;
    private MockedStatic<StarChartStoragePanel> chartStorageMock;
    private MockedStatic<StarChartBrowser> browserMock;
    private final ModificationBay.Listener bayListener = mock(ModificationBay.Listener.class);

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        SocketWorkbenchPanel workbench = mock(SocketWorkbenchPanel.class);
        workbenchMock = Mockito.mockStatic(SocketWorkbenchPanel.class, invocation -> {
            if (!"open".equals(invocation.getMethod().getName())) {
                return invocation.callRealMethod();
            }
            openedMode.set(invocation.getArgument(3));
            benchListener.set(invocation.getArgument(4));
            return workbench;
        });
        itemStorageMock = Mockito.mockStatic(ItemStoragePanel.class);
        chartStorageMock = Mockito.mockStatic(StarChartStoragePanel.class);
        browserMock = Mockito.mockStatic(StarChartBrowser.class);
    }

    @AfterEach
    void tearDown() {
        browserMock.close();
        chartStorageMock.close();
        itemStorageMock.close();
        workbenchMock.close();
        mockSector.close();
    }

    @Test
    void theStarChartsOnlyBayOpensOnCharts() {
        ModificationBay bay = ModificationBay.openMode(mock(CustomPanelAPI.class), SIDE_BY_SIDE_AREA, BenchMode.STAR_CHARTS, item -> null, bayListener);

        assertEquals(BenchMode.STAR_CHARTS, openedMode.get());
        assertEquals(BenchMode.STAR_CHARTS, bay.mode());
        assertTrue(benchListener.get().offersWormholeOpening());
        chartStorageMock.verify(() -> StarChartStoragePanel.open(any(), any(), any(), any(), any()), times(1));
        itemStorageMock.verify(() -> ItemStoragePanel.openPicker(any(), any(), any(), any(), anyBoolean()), never());
    }

    @Test
    void theCraftingBayOpensOnHullItemsWithoutCharts() {
        ModificationBay bay = ModificationBay.open(mock(CustomPanelAPI.class), SIDE_BY_SIDE_AREA, item -> null, bayListener);

        assertEquals(BenchMode.HULL_ITEMS, openedMode.get());
        assertEquals(BenchMode.HULL_ITEMS, bay.mode());
        chartStorageMock.verify(() -> StarChartStoragePanel.open(any(), any(), any(), any(), any()), never());
    }

    @Test
    void theHullItemsOnlyBayHidesWormholeOpening() {
        ModificationBay.openHullItemsOnly(mock(CustomPanelAPI.class), SIDE_BY_SIDE_AREA, item -> null, bayListener);

        assertEquals(BenchMode.HULL_ITEMS, openedMode.get());
        assertFalse(benchListener.get().offersWormholeOpening());
    }
}
