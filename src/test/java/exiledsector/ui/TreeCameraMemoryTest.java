package exiledsector.ui;

import exiledsector.ui.hyperspace.HyperspaceCamera;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeCameraMemoryTest {

    @AfterEach
    void tearDown() {
        TreeCameraMemory.forgetAll();
    }

    @Test
    void eachShipKeepsItsOwnViewUntilASaveIsLoaded() {
        TreeCameraMemory.View eagle = TreeCameraMemory.forShip("eagle");
        assertFalse(eagle.isSaved());

        eagle.save(120f, -40f, 1.5f);

        assertSame(eagle, TreeCameraMemory.forShip("eagle"));
        assertTrue(TreeCameraMemory.forShip("eagle").isSaved());
        assertEquals(new HyperspaceCamera(120f, -40f, 1.5f), TreeCameraMemory.forShip("eagle").camera());
        assertFalse(TreeCameraMemory.forShip("lasher").isSaved());

        TreeCameraMemory.forgetAll();
        assertFalse(TreeCameraMemory.forShip("eagle").isSaved());
    }

    @Test
    void aShipWithNoIdIsNeverRemembered() {
        TreeCameraMemory.forShip(null).save(1f, 2f, 1f);

        assertNotSame(TreeCameraMemory.forShip(null), TreeCameraMemory.forShip(null));
        assertFalse(TreeCameraMemory.forShip(null).isSaved());
    }
}
