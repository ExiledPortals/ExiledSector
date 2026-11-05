package exiledsector.skills.skilleffect.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoRetreatListenerTest {

    private static ShipAPI ship(boolean retreating) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.isAlive()).thenReturn(true);
        when(ship.isRetreating()).thenReturn(retreating);
        when(ship.getOwner()).thenReturn(0);
        return ship;
    }

    @Test
    void aRetreatingShipTurnsBackToSearchAndDestroy() {
        ShipAPI ship = ship(true);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        CombatFleetManagerAPI fleetManager = mock(CombatFleetManagerAPI.class);
        CombatTaskManagerAPI taskManager = mock(CombatTaskManagerAPI.class);
        DeployedFleetMemberAPI deployed = mock(DeployedFleetMemberAPI.class);
        when(engine.getFleetManager(0)).thenReturn(fleetManager);
        when(fleetManager.getTaskManager(false)).thenReturn(taskManager);
        when(fleetManager.getDeployedFleetMember(ship)).thenReturn(deployed);

        try (MockedStatic<Global> global = mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);
            new NoRetreatListener(ship).advance(0.1f);
        }

        verify(ship).setRetreating(false, false);
        verify(taskManager).orderSearchAndDestroy(deployed, false);
    }

    @Test
    void aShipThatIsNotRetreatingIsLeftAlone() {
        ShipAPI ship = ship(false);

        new NoRetreatListener(ship).advance(0.1f);

        verify(ship, never()).setRetreating(anyBoolean(), anyBoolean());
    }

    @Test
    void withoutAFleetManagerOnlyTheRetreatFlagIsCleared() {
        ShipAPI ship = ship(true);

        try (MockedStatic<Global> global = mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(mock(CombatEngineAPI.class));
            new NoRetreatListener(ship).advance(0.1f);
        }

        verify(ship).setRetreating(false, false);
    }
}
