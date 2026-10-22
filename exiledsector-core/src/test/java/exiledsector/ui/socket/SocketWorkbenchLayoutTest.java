package exiledsector.ui.socket;

import exiledsector.items.CraftingCurrency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SocketWorkbenchLayoutTest {

    @Test
    void nanoswarmTilesSpreadEvenlyOverTheTopOfTheSlotFromLeftToRight() {
        int tileCount = CraftingCurrency.values().length;
        float firstAngle = (float) Math.PI + BenchScene.ORBIT_DROP_RADIANS;
        float lastAngle = -BenchScene.ORBIT_DROP_RADIANS;
        float spacing = (firstAngle - lastAngle) / (tileCount - 1);

        assertEquals(firstAngle, SocketWorkbenchPanel.tileAngle(0, tileCount), 1e-6f);
        for (int index = 1; index < tileCount; index++) {
            assertEquals(spacing, SocketWorkbenchPanel.tileAngle(index - 1, tileCount) - SocketWorkbenchPanel.tileAngle(index, tileCount), 1e-5f);
        }
        assertEquals(lastAngle, SocketWorkbenchPanel.tileAngle(tileCount - 1, tileCount), 1e-5f);
    }

    @Test
    void aLoneTileSitsAboveTheSlot() {
        assertEquals((float) (Math.PI / 2.0), SocketWorkbenchPanel.tileAngle(0, 1), 1e-6f);
    }
}
