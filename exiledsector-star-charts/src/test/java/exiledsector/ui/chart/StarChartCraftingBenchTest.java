package exiledsector.ui.chart;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.items.ItemStore;
import exiledsector.ui.socket.BenchModeSupport;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class StarChartCraftingBenchTest {

    private final SectorAPI sector = mock(SectorAPI.class);
    private final ItemStore store = new ItemStore();

    @Test
    void aChartOnlyOpensOnAnAtlasSystem() {
        BenchModeSupport.OpenCheck untargeted = StarChartCraftingBench.INSTANCE.openCheck(sector, store, null);
        BenchModeSupport.OpenCheck otherTarget = StarChartCraftingBench.INSTANCE.openCheck(sector, store, null, "not a cell");

        assertFalse(untargeted.allowsOpening());
        assertEquals(StarChartCraftingBench.NO_SYSTEM_KEY, untargeted.reasonKey());
        assertFalse(otherTarget.allowsOpening());
        assertEquals(StarChartCraftingBench.NO_SYSTEM_KEY, otherTarget.reasonKey());
    }

    @Test
    void openingWithoutAnAtlasSystemRefuses() {
        BenchModeSupport.OpenOutcome untargeted = StarChartCraftingBench.INSTANCE.open(sector, store, null, 1, new Random(1L));
        BenchModeSupport.OpenOutcome noCell = StarChartCraftingBench.INSTANCE.openAt(sector, store, null, 1, new Random(1L), null);

        assertFalse(untargeted.opened());
        assertEquals(StarChartCraftingBench.NO_SYSTEM_KEY, untargeted.messageKey());
        assertFalse(noCell.opened());
        assertEquals(StarChartCraftingBench.NO_SYSTEM_KEY, noCell.messageKey());
    }
}
