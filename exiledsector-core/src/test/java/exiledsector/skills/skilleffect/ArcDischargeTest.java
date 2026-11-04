package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArcDischargeTest {

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

    private ShipAPI ship(int owner, float x, boolean fighter, HullSize hullSize) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.isAlive()).thenReturn(true);
        when(ship.isFighter()).thenReturn(fighter);
        when(ship.getHullSize()).thenReturn(hullSize);
        when(ship.getCollisionClass()).thenReturn(CollisionClass.SHIP);
        gridShips.add(ship);
        return ship;
    }

    private void verifyArcs(ShipAPI source, ShipAPI target, int arcs, float damage) {
        verify(engine, times(arcs)).spawnEmpArc(eq(source), any(Vector2f.class), eq(source), eq(target), eq(DamageType.ENERGY), eq(damage),
                eq(damage), anyFloat(), any(), anyFloat(), any(Color.class), any(Color.class));
    }

    @Test
    void arcsCycleThroughTheNearestEnemiesWhenThereAreFewerEnemiesThanArcs() {
        ShipAPI nearest = mock(ShipAPI.class);
        ShipAPI next = mock(ShipAPI.class);

        assertEquals(List.of(nearest, next, nearest), ArcDischarge.arcTargets(List.of(nearest, next), 3));
        assertEquals(List.of(), ArcDischarge.arcTargets(List.of(), 3));
    }

    @Test
    void arcsStrikeTheNearestEnemyShipsInRangeWithDamageByTheSourceHullSize() {
        ShipAPI source = ship(0, 0f, false, HullSize.CRUISER);
        ShipAPI near = ship(1, 200f, false, HullSize.FRIGATE);
        ShipAPI far = ship(1, 400f, false, HullSize.FRIGATE);
        ShipAPI fighter = ship(1, 100f, true, HullSize.FIGHTER);
        ShipAPI outOfRange = ship(1, ArcDischarge.RANGE + 100f, false, HullSize.FRIGATE);
        ShipAPI ally = ship(0, 50f, false, HullSize.FRIGATE);
        float damage = ArcDischarge.DAMAGE.of(HullSize.CRUISER);

        assertTrue(ArcDischarge.discharge(source, 3));

        verifyArcs(source, near, 2, damage);
        verifyArcs(source, far, 1, damage);
        verifyArcs(source, fighter, 0, damage);
        verifyArcs(source, outOfRange, 0, damage);
        verifyArcs(source, ally, 0, damage);
    }

    @Test
    void withNoEnemyInRangeNothingIsDischarged() {
        ShipAPI source = ship(0, 0f, false, HullSize.DESTROYER);
        ship(1, ArcDischarge.RANGE + 100f, false, HullSize.FRIGATE);

        assertFalse(ArcDischarge.discharge(source, 3));
        verify(engine, never()).spawnEmpArc(any(), any(), any(), any(), any(), anyFloat(), anyFloat(), anyFloat(), any(), anyFloat(), any(), any());
    }
}
