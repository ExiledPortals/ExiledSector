package exiledlib.skills.skilleffect.aura;

import exiledlib.skills.skilleffect.CombatQueries;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepairDroneTest {

    private static final Vector2f HOME = new Vector2f(0f, 0f);
    private static final float DOCK_RADIUS = 30f;

    @Test
    void aDroneStaysDockedUntilThereIsWorkToDo() {
        RepairDrone drone = new RepairDrone(0f);

        drone.advance(1f, HOME, DOCK_RADIUS, null);

        assertTrue(drone.isDocked());
    }

    @Test
    void aDroneLaunchesFliesToTheWorkPointAtItsSpeedAndFacesWhereItIsGoing() {
        RepairDrone drone = new RepairDrone(0f);
        Vector2f workPoint = new Vector2f(1000f, 0f);

        drone.advance(0.5f, HOME, DOCK_RADIUS, workPoint);

        assertFalse(drone.isDocked());
        assertFalse(drone.isWorking());
        assertEquals(RepairDrone.SPEED * 0.5f, drone.position().length(), 1f);
        assertEquals(0f, drone.facingDeg(), 5f);
    }

    @Test
    void aDroneHoversAtTheWorkPointWhileRepairing() {
        RepairDrone drone = new RepairDrone(0f);
        Vector2f workPoint = new Vector2f(300f, 400f);

        for (int frame = 0; frame < 120; frame++) {
            drone.advance(1f / 60f, HOME, DOCK_RADIUS, workPoint);
        }

        assertTrue(drone.isWorking());
        assertTrue(CombatQueries.withinRadius(drone.position(), workPoint, RepairDrone.WORKING_DISTANCE));
    }

    @Test
    void aDroneFliesHomeAndDocksWhenTheWorkStops() {
        RepairDrone drone = new RepairDrone(0f);
        Vector2f workPoint = new Vector2f(300f, 0f);
        for (int frame = 0; frame < 120; frame++) {
            drone.advance(1f / 60f, HOME, DOCK_RADIUS, workPoint);
        }

        for (int frame = 0; frame < 120; frame++) {
            drone.advance(1f / 60f, HOME, DOCK_RADIUS, null);
        }

        assertTrue(drone.isDocked());
        assertFalse(drone.isWorking());
    }
}
