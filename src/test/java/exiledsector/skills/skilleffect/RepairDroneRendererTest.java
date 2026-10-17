package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RepairDroneRendererTest {

    @Test
    void aDeadCoordinatorsDronesFadeOutInsteadOfVanishing() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        ShipAPI coordinatorShip = mock(ShipAPI.class);
        when(coordinatorShip.getLocation()).thenReturn(new Vector2f());
        when(coordinatorShip.getCollisionRadius()).thenReturn(50f);
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);
            RepairCoordinatorField field = new RepairCoordinatorField();
            RepairDroneRenderer renderer = new RepairDroneRenderer(field);
            RepairCoordinatorListener coordinator = new RepairCoordinatorListener(coordinatorShip);
            field.sources.add(coordinator);
            renderer.advance(0.1f);
            assertEquals(1, renderer.swarmCount());

            field.sources.remove(coordinator);
            renderer.advance(RepairDroneRenderer.ORPHAN_FADE_SECONDS / 2f);
            assertEquals(1, renderer.swarmCount());

            renderer.advance(RepairDroneRenderer.ORPHAN_FADE_SECONDS);
            assertEquals(0, renderer.swarmCount());
        }
    }
}
