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
    void theHullCraftingBenchOffersTheEngramTrayOnlyWhileStarChartsAreOn() {
        assertEquals(List.of(ExcavationInhibitorTray.INSTANCE, EngramTray.INSTANCE), HullCraftingBench.INSTANCE.trays());

        StarChartSettings.setEnabledForTests(false);

        assertEquals(List.of(ExcavationInhibitorTray.INSTANCE), HullCraftingBench.INSTANCE.trays());
    }
}
