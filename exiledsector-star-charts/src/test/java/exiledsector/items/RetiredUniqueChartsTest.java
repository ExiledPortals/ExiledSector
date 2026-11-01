package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.MockSector;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetiredUniqueChartsTest {

    private static final String RETIRED_ID = "unique_chart_silent_reach";

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

    private static ItemData chart(String definitionId) {
        ItemData shipped = ItemData.rolledAtTier(ItemDefinitions.get("star_chart"), 7L, 3);
        return new ItemData(definitionId, shipped.seed(), shipped.explicits(), shipped.frozenName(), shipped.traits());
    }

    private static CargoStackAPI stack(String specialId, ItemData item, float size) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(new SpecialItemData(specialId, item.encode()));
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    @Test
    void retiredUniqueChartsLeaveCargoAndEveryOtherStackStays() {
        CargoAPI cargo = mockSector.cargo();
        CargoStackAPI retired = stack(ItemData.STAR_CHART_ITEM_ID, chart(RETIRED_ID), 2f);
        CargoStackAPI kept = stack(ItemData.STAR_CHART_ITEM_ID, chart("star_chart"), 1f);
        CargoStackAPI shipItem = stack(ItemData.ITEM_ID, chart(RETIRED_ID), 1f);
        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>(List.of(retired, kept, shipItem)));

        assertEquals(2, RetiredUniqueCharts.removeFrom(mockSector.sector));
        verify(cargo).removeStack(retired);
        verify(cargo, never()).removeStack(kept);
        verify(cargo, never()).removeStack(shipItem);
    }

    @Test
    void retiredUniqueChartsLeaveChartStorage() throws Exception {
        StarChartDefinitions.register(HullItemRows.shippedRows(StarChartDefinitions.DATA_PATH).put(new JSONObject().put("id", RETIRED_ID)
                .put("system_type", "BINARY_FAR").put("name", "The Silent Reach").put("rarity", "1").put("prefixes", "@HYBRID_CHART_FLEET_QUALITY")));
        ItemStore store = ItemStore.starCharts();
        Item kept = store.add(chart("star_chart"));
        store.add(chart(RETIRED_ID));

        assertEquals(1, RetiredUniqueCharts.removeFrom(store));
        assertEquals(List.of(kept), store.owned());
        assertEquals(0, RetiredUniqueCharts.removeFrom((ItemStore) null));
        assertEquals(0, RetiredUniqueCharts.removeFrom((CargoAPI) null));
    }

    @Test
    void aFailingSweepKeepsTheLoadGoing() {
        when(mockSector.cargo().getStacksCopy()).thenThrow(new IllegalStateException("broken cargo"));

        assertEquals(0, RetiredUniqueCharts.removeFrom(mockSector.sector));
        verify(mockSector.cargo(), never()).removeStack(any());
    }
}
