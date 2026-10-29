package exiledsector.ui.chart;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.MockSector;
import exiledsector.i18n.I18n;
import exiledsector.i18n.LocaleChain;
import exiledsector.i18n.RealCatalogue;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import exiledsector.items.StarChartFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarChartCargoTest {

    private MockSector mockSector;
    private CargoAPI cargo;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        mockSector = new MockSector();
        cargo = mockSector.cargo();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        StarChartFixtures.clear();
    }

    private CargoStackAPI chartStack(int tier, float size) {
        ItemData chart = ItemData.rolledAtTier(ItemDefinitions.get("star_chart"), 40L + tier, tier);
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(new SpecialItemData(ItemData.STAR_CHART_ITEM_ID, chart.encode()));
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    @Test
    void preparingMovesChartsFromCargoIntoStorageAndShowsThemByTier() {
        CargoStackAPI threes = chartStack(3, 2f);
        CargoStackAPI eight = chartStack(8, 1f);
        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>(List.of(threes, eight)));
        StarChartBrowser browser = new StarChartBrowser();

        int moved = StarChartBrowser.prepare(mockSector.sector, browser);

        assertEquals(3, moved);
        verify(cargo).removeStack(threes);
        verify(cargo).removeStack(eight);
        assertEquals(3, ItemStore.starChartsIfCreated().owned().size());
        assertEquals(2, browser.count(3));
        assertEquals(1, browser.count(8));
        assertEquals(3, browser.selectedTier());
        assertEquals("3 Star Charts moved from your cargo into storage.", StarChartBrowser.movedNotice(moved));
        assertEquals("1 Star Chart moved from your cargo into storage.", StarChartBrowser.movedNotice(1));
        assertNull(StarChartBrowser.movedNotice(0));
    }

    @Test
    void preparingWithNoChartsAnywhereCreatesNoStorage() {
        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>());
        StarChartBrowser browser = new StarChartBrowser();

        assertEquals(0, StarChartBrowser.prepare(mockSector.sector, browser));

        assertNull(ItemStore.starChartsIfCreated());
        assertEquals(0, browser.storedCount());
        verify(cargo, never()).removeStack(Mockito.any());
    }

    @Test
    void theMovedNoticeUsesTheGameTextLanguageBecauseVanillaDrawsIt() {
        I18n.install(RealCatalogue.of(LocaleChain.SIMPLIFIED_CHINESE), RealCatalogue.english());

        assertEquals("2 Star Charts moved from your cargo into storage.", StarChartBrowser.movedNotice(2));
    }
}
