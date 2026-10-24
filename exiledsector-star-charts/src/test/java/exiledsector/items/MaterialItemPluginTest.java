package exiledsector.items;

import exiledsector.charts.StarChartSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialItemPluginTest {

    @BeforeEach
    void setUp() {
        StarChartCurrency.registerAll();
    }

    @AfterEach
    void tearDown() {
        StarChartCurrency.unregisterAll();
        StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
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

    @Test
    void withStarChartsOffNoKernelMentionsThem() {
        StarChartSettings.setEnabledForTests(false);

        for (CraftingCurrency kernel : CraftingCurrency.values()) {
            assertFalse(MaterialItemPlugin.alsoWorksOnStarCharts(kernel.itemId()), kernel.name());
        }
    }

    @Test
    void ciphersAreUsableOnlyWhileStarChartsAreOn() {
        for (StarChartCurrency cipher : StarChartCurrency.values()) {
            assertTrue(ItemUnlock.currencyUsable(cipher.itemId(), null), cipher.name());
        }

        StarChartSettings.setEnabledForTests(false);

        for (StarChartCurrency cipher : StarChartCurrency.values()) {
            assertFalse(ItemUnlock.currencyUsable(cipher.itemId(), null), cipher.name());
        }
    }
}
