package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeViewportTest {

    @Test
    void worldCoordinatesAreScaledByZoomAroundTheCenterWithYPointingUp() {
        TreeViewport viewport = new TreeViewport(400f, 300f, 2f);

        assertEquals(400f, viewport.screenX(0f));
        assertEquals(300f, viewport.screenY(0f));
        assertEquals(420f, viewport.screenX(10f));
        assertEquals(280f, viewport.screenY(10f));
        assertEquals(390f, viewport.screenX(-5f));
    }

    @Test
    void somethingIsVisibleWhenItsExtentTouchesThePanel() {
        TreeViewport viewport = new TreeViewport(400f, 300f, 1f, 0f, 0f, 800f, 600f);

        assertTrue(viewport.isVisible(400f, 300f, 1f));
        assertTrue(viewport.isVisible(-20f, 300f, 25f));
        assertFalse(viewport.isVisible(-20f, 300f, 15f));
        assertFalse(viewport.overlaps(810f, 0f, 900f, 600f));
        assertTrue(viewport.overlaps(790f, 590f, 900f, 700f));
    }

    @Test
    void aViewportWithoutBoundsShowsEverything() {
        assertTrue(new TreeViewport(0f, 0f, 1f).isVisible(1e9f, -1e9f, 1f));
    }
}
