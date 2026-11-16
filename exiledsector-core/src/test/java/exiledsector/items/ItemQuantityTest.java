package exiledsector.items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ItemQuantityTest {

    private static final float EPSILON = 1e-6f;

    @Test
    void increasedPercentsAddUpInsteadOfMultiplying() {
        ItemQuantity chartAndPlayer = ItemQuantity.increasedBy(10f).plusPercent(10f);

        assertEquals(1.2f, chartAndPlayer.mult(), EPSILON);
        assertEquals(1.2f, ItemChances.of(ItemQuantity.increasedBy(10f)).generalPlusPercent(10f), EPSILON);
    }

    @Test
    void aShareScalesTheSummedPercentAsItsOwnMultiplier() {
        ItemQuantity halfShare = ItemQuantity.increasedBy(50f).scaled(0.5f).plusPercent(50f);

        assertEquals(1f, halfShare.mult(), EPSILON);
        assertEquals(0f, ItemQuantity.atShare(0f).plusPercent(100f).mult(), EPSILON);
        assertEquals(0f, ItemQuantity.increasedBy(-150f).mult(), EPSILON);
    }

    @Test
    void addingNothingKeepsTheSameQuantity() {
        ItemQuantity quantity = ItemQuantity.increasedBy(25f);

        assertSame(quantity, quantity.plusPercent(0f));
        assertSame(quantity, quantity.scaled(1f));
        assertEquals(ItemQuantity.BASE, ItemQuantity.atShare(1f));
    }
}
