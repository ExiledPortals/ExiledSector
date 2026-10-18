package exiledsector.items;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MaterialItemPluginTest {

    @BeforeEach
    void setUp() {
        StarChartCurrency.registerAll();
    }

    @AfterEach
    void tearDown() {
        StarChartCurrency.unregisterAll();
    }

    @Test
    void onlyKernelsThatAlsoWorkOnChartsSayTheyDo() {
        for (CraftingCurrency kernel : CraftingCurrency.values()) {
            assertEquals(StarChartCurrency.KERNELS_ON_CHARTS.contains(kernel), MaterialItemPlugin.alsoWorksOnStarCharts(kernel.itemId()),
                    kernel.name());
        }
        for (StarChartCurrency cipher : StarChartCurrency.values()) {
            assertFalse(MaterialItemPlugin.alsoWorksOnStarCharts(cipher.itemId()), cipher.name());
        }
        assertFalse(MaterialItemPlugin.alsoWorksOnStarCharts("exiledSector_socketable_parts"));
    }
}
