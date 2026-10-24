package exiledsector.charts;

import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import exiledsector.items.StarChart;
import exiledsector.items.StarChartFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartSupplyTest {

    private ItemStore store;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        store = ItemStore.forStarCharts();
    }

    @AfterEach
    void tearDown() {
        StarChartFixtures.clear();
    }

    private Item stored(long seed, int tier) {
        return store.add(ItemData.rolledAtTier(ItemDefinitions.get("star_chart"), seed, tier));
    }

    @Test
    void threeChartsOfATierBecomeOneOfTheNextAndTheSelectedChartIsKept() {
        Item kept = stored(1L, 4);
        stored(2L, 4);
        stored(3L, 4);
        stored(4L, 4);
        stored(5L, 6);

        assertEquals(3, ChartSupply.combinable(store, 4, kept).size());
        Item made = ChartSupply.combine(store, 4, kept, new Random(9L));

        assertNotNull(made);
        assertEquals(5, ((StarChart) made).tier());
        assertFalse(((StarChart) made).definition().unique());
        assertTrue(store.owned().contains(kept));
        assertEquals(3, store.owned().size());
        assertTrue(ChartSupply.combinable(store, 4, kept).isEmpty());
        assertNull(ChartSupply.combine(store, 4, kept, new Random(9L)));
    }

    @Test
    void uniqueChartsAreNeverSpentByCombining() {
        stored(1L, 3);
        stored(2L, 3);
        store.add(ItemData.rolledAtTier(ItemDefinitions.get(StarChartFixtures.TEST_UNIQUE), 3L, 3));

        assertEquals(List.of(), ChartSupply.combinable(store, 3, null));
    }

    @Test
    void topTierChartsCannotBeCombined() {
        for (long seed = 0; seed < 3; seed++) {
            stored(seed, ChartTiers.HIGHEST_TIER);
        }

        assertEquals(List.of(), ChartSupply.combinable(store, ChartTiers.HIGHEST_TIER, null));
    }
}
