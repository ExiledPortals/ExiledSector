package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DepthOrderTest {

    @Test
    void itemsComeOutFarthestFirstAndTiesKeepTheirQueueOrder() {
        DepthOrder<String> order = new DepthOrder<>();
        order.add("front", 0, 0, -50f);
        order.add("back", 1, 2, 80f);
        order.add("middle", 2, 0, 0f);
        order.add("middle", 3, 1, 0f);
        order.sortBackToFront();

        assertEquals(4, order.size());
        assertEquals("back", order.kind(0));
        assertEquals(2, order.part(0));
        assertEquals(2, order.index(1));
        assertEquals(3, order.index(2));
        assertEquals("front", order.kind(3));
        assertEquals(-50f, order.depth(3));
    }

    @Test
    void aClearedOrderIsReusedAndGrowsPastItsStartingSize() {
        DepthOrder<String> order = new DepthOrder<>();
        order.add("old", 0, 0, 1f);
        order.clear();
        for (int index = 0; index < 100; index++) {
            order.add("item", index, 0, index);
        }
        order.sortBackToFront();

        assertEquals(100, order.size());
        assertEquals(99, order.index(0));
        assertEquals(0, order.index(99));
    }
}
