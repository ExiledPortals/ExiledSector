package exiledsector.ui.socket;

import exiledsector.MockSector;
import exiledsector.charts.StarChartSettings;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.Engram;
import exiledsector.items.ItemType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EngramTrayTest {

    private static final Engram REVENANT = engram("exiledSector_test_revenant");
    private static final Engram VIGIL = engram("exiledSector_test_vigil");
    private MockSector mockSector;

    private static Engram engram(String itemId) {
        return new Engram(itemId, itemId, "HULL_PERCENT", Set.of(ItemType.HULL_ITEM), 1, 1f, Color.WHITE);
    }

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
    void theEngramTrayListsOnlyTheEngramsYouHoldAsCraftingItems() {
        CraftingMaterials materials = CraftingMaterials.storedForTests(Map.of(VIGIL.itemId(), 3));
        BenchTrayContext context = mock(BenchTrayContext.class);
        when(context.materials()).thenReturn(materials);

        List<BenchTrayEntry> entries = EngramTray.held(List.of(REVENANT, VIGIL), materials);

        assertEquals(List.of(new CraftingItemTrayEntry(VIGIL)), entries);
        assertEquals("3", entries.get(0).badge(context));
    }

    @Test
    void theEngramTraySplitsHeldEngramsIntoAPrefixAndASuffixColumn() {
        CraftingMaterials materials = CraftingMaterials.storedForTests(Map.of(VIGIL.itemId(), 3, REVENANT.itemId(), 1));
        BenchTrayEntry vigil = new CraftingItemTrayEntry(VIGIL);

        List<BenchTray.Column> columns = EngramTray.sided(List.of(REVENANT, VIGIL), materials, vigil::equals);

        assertEquals(List.of(EngramTray.PREFIXES_KEY, EngramTray.SUFFIXES_KEY), columns.stream().map(BenchTray.Column::headingKey).toList());
        assertEquals(List.of(vigil), columns.stream().flatMap(column -> column.entries().stream()).toList());
    }

    @Test
    void theInfusedTrayOrdersRowsByFeedstockCostThenByAction() {
        InfusedCraftingTray.CraftRow cheapExchange = row(100, InfusedCraftingTray.Action.EXCHANGE);
        InfusedCraftingTray.CraftRow cheapReforge = row(100, InfusedCraftingTray.Action.REFORGE);
        InfusedCraftingTray.CraftRow dearReforge = row(1000, InfusedCraftingTray.Action.REFORGE);
        InfusedCraftingTray.CraftRow cheapBiomeReforge = row(100, InfusedCraftingTray.Action.BIOME_REFORGE);
        List<InfusedCraftingTray.CraftRow> rows = new java.util.ArrayList<>(List.of(dearReforge, cheapExchange, cheapReforge, cheapBiomeReforge));

        rows.sort(InfusedCraftingTray.ROW_ORDER);

        assertEquals(List.of(cheapReforge, cheapBiomeReforge, cheapExchange, dearReforge), rows);
    }

    private static InfusedCraftingTray.CraftRow row(int feedstockAmount, InfusedCraftingTray.Action action) {
        InfusedCraftingTray.CraftRow row = mock(InfusedCraftingTray.CraftRow.class);
        when(row.feedstockAmount()).thenReturn(feedstockAmount);
        when(row.action()).thenReturn(action);
        return row;
    }

    @Test
    void theHullCraftingBenchOffersTheChartTraysOnlyWhileStarChartsAreOn() {
        assertEquals(List.of(InhibitorTray.INSTANCE, EngramTray.INSTANCE, InfusedCraftingTray.INSTANCE), HullCraftingBench.INSTANCE.trays());

        StarChartSettings.setEnabledForTests(false);

        assertEquals(List.of(InhibitorTray.INSTANCE), HullCraftingBench.INSTANCE.trays());
    }
}
