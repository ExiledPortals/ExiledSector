package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanvasDragTest {

    private enum Mode { TREE, ROOT_CHOICE }

    private static final Object NODE = new Object();

    private final CanvasCamera camera = new CanvasCamera();
    private final CanvasDrag<Mode> drag = new CanvasDrag<>(camera);

    @Test
    void aTargetPressDraggedPastTheThresholdPansFromWhereItWasPressed() {
        float startX = camera.centreTreeX();
        drag.gesture().pressTarget(Mode.TREE, NODE, 100f, 100f, false, false, true);

        assertEquals(CanvasDrag.Move.NONE, drag.pointerMoved(103f, 100f, 3f, 0f));
        assertEquals(CanvasDrag.Move.STARTED_PANNING, drag.pointerMoved(150f, 100f, 47f, 0f));

        assertTrue(camera.isDragging());
        assertEquals(startX - 50f, camera.centreTreeX(), 1e-3f);
        assertEquals(CanvasDrag.Move.PANNED, drag.pointerMoved(160f, 100f, 10f, 0f));
        assertEquals(startX - 60f, camera.centreTreeX(), 1e-3f);
    }

    @Test
    void aPanPressDragsTheCameraUntilReleased() {
        drag.pressPan(Mode.TREE, 10f, 10f);

        assertTrue(camera.isDragging());
        assertTrue(drag.gesture().isPanning());

        drag.release();

        assertFalse(camera.isDragging());
        assertFalse(drag.gesture().isActive());
        assertEquals(CanvasDrag.Move.NONE, drag.pointerMoved(40f, 10f, 30f, 0f));
    }

    @Test
    void aGestureFromAnotherModeIsDroppedAndStopsTheDrag() {
        drag.pressPan(Mode.ROOT_CHOICE, 0f, 0f);

        assertFalse(drag.dropIfStaleIn(Mode.ROOT_CHOICE));
        assertTrue(drag.dropIfStaleIn(Mode.TREE));

        assertFalse(camera.isDragging());
        assertFalse(drag.gesture().isActive());
        assertFalse(drag.dropIfStaleIn(Mode.TREE));
    }
}
