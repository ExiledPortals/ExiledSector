package exiledsector.ui.socket;

import exiledsector.items.CraftingCurrency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SocketWorkbenchLayoutTest {

    @Test
    void kernelTilesSitEvenlyAroundTheSlotStartingAtTheTop() {
        int tileCount = CraftingCurrency.values().length;
        float spacing = (float) (2.0 * Math.PI / tileCount);

        assertEquals((float) (Math.PI / 2.0), SocketWorkbenchPanel.tileAngle(0, tileCount), 1e-6f);
        for (int index = 1; index < tileCount; index++) {
            assertEquals(spacing, SocketWorkbenchPanel.tileAngle(index - 1, tileCount) - SocketWorkbenchPanel.tileAngle(index, tileCount), 1e-5f);
        }
        assertEquals((float) (Math.PI / 2.0 - 2.0 * Math.PI), SocketWorkbenchPanel.tileAngle(tileCount, tileCount), 1e-5f);
    }
}
