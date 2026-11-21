package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReadoutBarTest {

    @Test
    void theBarFillsInProportionToHowCloseTheValueIsToItsMaximum() {
        assertEquals(0f, ReadoutBar.fillFraction(0f, 100f), 1e-6f);
        assertEquals(0.25f, ReadoutBar.fillFraction(25f, 100f), 1e-6f);
        assertEquals(1f, ReadoutBar.fillFraction(100f, 100f), 1e-6f);
    }

    @Test
    void theFillNeverLeavesTheBar() {
        assertEquals(1f, ReadoutBar.fillFraction(140f, 100f), 1e-6f);
        assertEquals(0f, ReadoutBar.fillFraction(-5f, 100f), 1e-6f);
    }

    @Test
    void aBarWithNoMaximumStaysEmpty() {
        assertEquals(0f, ReadoutBar.fillFraction(3f, 0f), 1e-6f);
    }
}
