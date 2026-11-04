package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhaseExitArcListenerTest {

    private final List<Object> gridShips = new ArrayList<>();
    private MockedStatic<Global> globalMock;
    private CombatEngineAPI engine;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(call -> new ArrayList<>(gridShips).iterator());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private ShipAPI phaseShip(float arcs) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(0);
        when(ship.getLocation()).thenReturn(new Vector2f());
        when(ship.isAlive()).thenReturn(true);
        when(ship.isHulk()).thenReturn(false);
        when(ship.getHullSize()).thenReturn(HullSize.FRIGATE);
        when(ship.getMutableStats().getDynamic().getValue(PhaseExitArcListener.ARC_COUNT_KEY, 0f)).thenReturn(arcs);
        return ship;
    }

    private ShipAPI enemy() {
        ShipAPI enemy = mock(ShipAPI.class);
        when(enemy.getOwner()).thenReturn(1);
        when(enemy.getLocation()).thenReturn(new Vector2f(300f, 0f));
        when(enemy.isAlive()).thenReturn(true);
        when(enemy.getCollisionClass()).thenReturn(CollisionClass.SHIP);
        gridShips.add(enemy);
        return enemy;
    }

    private void verifyArcsAt(ShipAPI target, int arcs) {
        verify(engine, times(arcs)).spawnEmpArc(any(), any(), any(), eq(target), any(), anyFloat(), anyFloat(), anyFloat(), any(), anyFloat(),
                any(), any());
    }

    @Test
    void leavingPhaseDischargesTheArcs() {
        ShipAPI ship = phaseShip(3f);
        ShipAPI enemy = enemy();
        PhaseExitArcListener listener = new PhaseExitArcListener(ship);

        when(ship.isPhased()).thenReturn(true);
        listener.advance(0.1f);
        verifyArcsAt(enemy, 0);

        when(ship.isPhased()).thenReturn(false);
        listener.advance(0.1f);
        listener.advance(0.1f);
        verifyArcsAt(enemy, 3);
    }

    @Test
    void aShipThatNeverPhasesNeverDischarges() {
        ShipAPI ship = phaseShip(3f);
        enemy();
        PhaseExitArcListener listener = new PhaseExitArcListener(ship);

        listener.advance(0.1f);
        listener.advance(0.1f);

        verify(engine, never()).spawnEmpArc(any(), any(), any(), any(), any(), anyFloat(), anyFloat(), anyFloat(), any(), anyFloat(), any(), any());
    }
}
