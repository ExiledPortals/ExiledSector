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
        StarChartCipher.registerAll();
    }

    @AfterEach
    void tearDown() {
        StarChartFixtures.unregisterCraftingItems();
        StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
    }

    @Test
    void onlyKernelsThatAlsoWorkOnChartsSayTheyDo() {
        for (Nanoswarm kernel : Nanoswarm.values()) {
            assertEquals(StarChartCipher.KERNELS_ON_CHARTS.contains(kernel), MaterialItemPlugin.alsoWorksOnStarCharts(kernel.itemId()),
                    kernel.name());
        }
        for (StarChartCipher cipher : StarChartCipher.values()) {
            assertFalse(MaterialItemPlugin.alsoWorksOnStarCharts(cipher.itemId()), cipher.name());
        }
        assertFalse(MaterialItemPlugin.alsoWorksOnStarCharts("exiledSector_nanite_feedstock"));
    }

    @Test
    void withStarChartsOffNoKernelMentionsThem() {
        StarChartSettings.setEnabledForTests(false);

        for (Nanoswarm kernel : Nanoswarm.values()) {
            assertFalse(MaterialItemPlugin.alsoWorksOnStarCharts(kernel.itemId()), kernel.name());
        }
    }

    @Test
    void ciphersAreUsableOnlyWhileStarChartsAreOn() {
        for (StarChartCipher cipher : StarChartCipher.values()) {
            assertTrue(ItemUnlock.craftingItemUsable(cipher.itemId(), null), cipher.name());
        }

        StarChartSettings.setEnabledForTests(false);

        for (StarChartCipher cipher : StarChartCipher.values()) {
            assertFalse(ItemUnlock.craftingItemUsable(cipher.itemId(), null), cipher.name());
        }
    }
}
