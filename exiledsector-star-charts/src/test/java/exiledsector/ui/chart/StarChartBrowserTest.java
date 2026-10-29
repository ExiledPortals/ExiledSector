package exiledsector.ui.chart;

import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import exiledsector.items.StarChartFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarChartBrowserTest {

    private final ItemStore store = ItemStore.forStarCharts();

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
    }

    @AfterEach
    void tearDown() {
        StarChartFixtures.clear();
    }

    private Item stored(String definitionId, long seed, int tier) {
        return store.add(ItemData.rolledAtTier(ItemDefinitions.get(definitionId), seed, tier));
    }

    @Test
    void groupsChartsByTierAndCountsEachTier() {
        stored("star_chart", 1L, 3);
        stored("star_chart", 2L, 3);
        stored("star_chart", 3L, 7);
        StarChartBrowser browser = new StarChartBrowser();

        browser.reload(store.owned());

        assertEquals(3, browser.storedCount());
        assertEquals(2, browser.count(3));
        assertEquals(1, browser.count(7));
        for (int tier = StarChartBrowser.LOWEST_TIER; tier <= StarChartBrowser.HIGHEST_TIER; tier++) {
            if (tier != 3 && tier != 7) {
                assertEquals(0, browser.count(tier), "tier " + tier);
            }
        }
        assertEquals(0, browser.count(0));
        assertEquals(0, browser.count(StarChartBrowser.HIGHEST_TIER + 1));
        assertEquals(3, browser.selectedTier(), "opens on the lowest tier that holds charts");
        assertEquals(2, browser.shownCharts().size());
        assertTrue(browser.shownCharts().stream().allMatch(chart -> chart.tier() == 3));
    }

    @Test
    void anEmptyStorageOpensOnTierOneWithNothingShown() {
        StarChartBrowser browser = new StarChartBrowser();

        browser.reload(List.of());

        assertEquals(StarChartBrowser.LOWEST_TIER, browser.selectedTier());
        assertTrue(browser.shownCharts().isEmpty());
        assertNull(browser.selectedChart());
    }

    @Test
    void theSelectedTierAndChartSurviveReloadsWhileTheChartIsStillStored() {
        Item low = stored("star_chart", 1L, 2);
        Item high = stored("star_chart", 2L, 9);
        StarChartBrowser browser = new StarChartBrowser();
        browser.reload(store.owned());

        assertTrue(browser.select(high));
        assertEquals(9, browser.selectedTier());
        browser.selectTier(2);
        browser.reload(store.owned());

        assertSame(high, browser.selectedChart(), "the chart stays selected while another tier is browsed");
        assertEquals(2, browser.selectedTier());
        assertEquals(List.of(low), browser.shownCharts());

        browser.selectTier(5);
        browser.reload(store.owned());
        assertEquals(5, browser.selectedTier(), "an empty tier the player picked stays picked");
    }

    @Test
    void aChartThatLeftStorageIsDeselected() {
        Item chart = stored("star_chart", 1L, 4);
        stored("star_chart", 2L, 4);
        StarChartBrowser browser = new StarChartBrowser();
        browser.reload(store.owned());
        browser.select(chart);

        store.remove(chart);
        browser.reload(store.owned());

        assertNull(browser.selectedChart());
        assertEquals(1, browser.count(4));
        assertFalse(browser.isSelected(chart));
    }

    @Test
    void selectingIgnoresChartsOutsideStorageAndTogglingClears() {
        Item chart = stored("star_chart", 1L, 4);
        Item loose = ItemData.rolledAtTier(ItemDefinitions.get("star_chart"), 5L, 4).preview();
        StarChartBrowser browser = new StarChartBrowser();
        browser.reload(store.owned());

        assertFalse(browser.select(loose));
        assertNull(browser.selectedChart());
        browser.toggle(chart);
        assertSame(chart, browser.selectedChart());
        browser.toggle(chart);
        assertNull(browser.selectedChart());
    }
}
