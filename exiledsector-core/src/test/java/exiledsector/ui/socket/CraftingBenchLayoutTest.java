package exiledsector.ui.socket;

import exiledsector.items.Nanoswarm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CraftingBenchLayoutTest {

    @Test
    void nanoswarmTilesSpreadEvenlyOverTheTopOfTheSlotFromLeftToRight() {
        int tileCount = Nanoswarm.values().length;
        float firstAngle = (float) Math.PI + BenchScene.ORBIT_DROP_RADIANS;
        float lastAngle = -BenchScene.ORBIT_DROP_RADIANS;
        float spacing = (firstAngle - lastAngle) / (tileCount - 1);

        assertEquals(firstAngle, CraftingBenchPanel.tileAngle(0, tileCount), 1e-6f);
        for (int index = 1; index < tileCount; index++) {
            assertEquals(spacing, CraftingBenchPanel.tileAngle(index - 1, tileCount) - CraftingBenchPanel.tileAngle(index, tileCount), 1e-5f);
        }
        assertEquals(lastAngle, CraftingBenchPanel.tileAngle(tileCount - 1, tileCount), 1e-5f);
    }

    @Test
    void aLoneTileSitsAboveTheSlot() {
        assertEquals((float) (Math.PI / 2.0), CraftingBenchPanel.tileAngle(0, 1), 1e-6f);
    }
}
