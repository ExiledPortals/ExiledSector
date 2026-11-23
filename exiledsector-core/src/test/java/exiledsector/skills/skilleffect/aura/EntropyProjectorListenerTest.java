package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntropyProjectorListenerTest {

    private final List<Object> gridShips = new ArrayList<>();
    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();
    private CombatEngineAPI engine;

    @BeforeEach
    void setUp() {
        engine = combat.engine();
        combat.shipGridOver(gridShips);
    }

    private static ShipAPI projector(float arcs) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(0);
        when(ship.getLocation()).thenReturn(new Vector2f());
        when(ship.isAlive()).thenReturn(true);
        when(ship.isHulk()).thenReturn(false);
        when(ship.isPhased()).thenReturn(false);
        when(ship.getMutableStats().getDynamic().getValue(EntropyProjectorListener.ARC_COUNT_KEY, 0f)).thenReturn(arcs);
        return ship;
    }

    private ShipAPI enemyAt(float x) {
        ShipAPI enemy = mock(ShipAPI.class);
        when(enemy.getOwner()).thenReturn(1);
        when(enemy.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(enemy.isAlive()).thenReturn(true);
        when(enemy.getCollisionClass()).thenReturn(CollisionClass.SHIP);
        gridShips.add(enemy);
        return enemy;
    }

    private void verifyArcs(ShipAPI source, ShipAPI target, int arcs) {
        verify(engine, times(arcs)).spawnEmpArc(eq(source), any(Vector2f.class), eq(source), eq(target), eq(DamageType.ENERGY),
                eq(EntropyProjectorListener.ENERGY_DAMAGE), eq(EntropyProjectorListener.EMP_DAMAGE), anyFloat(), any(), anyFloat(),
                any(Color.class), any(Color.class));
    }

    @Test
    void firesEveryArcAtTheNearestEnemyOnceCharged() {
        ShipAPI ship = projector(3f);
        ShipAPI farther = enemyAt(800f);
        ShipAPI nearest = enemyAt(400f);
        EntropyProjectorListener listener = new EntropyProjectorListener(ship);

        listener.advance(9f);
        verifyArcs(ship, nearest, 0);

        listener.advance(1f);
        verifyArcs(ship, nearest, 3);
        verifyArcs(ship, farther, 0);
        assertEquals(0f, listener.chargedSeconds(), 0.001f);
    }

    @Test
    void holdsTheChargeWhilePhasedOrWithNoEnemyInRange() {
        ShipAPI ship = projector(2f);
        ShipAPI distant = enemyAt(EntropyProjectorListener.RANGE + 500f);
        EntropyProjectorListener listener = new EntropyProjectorListener(ship);

        listener.advance(12f);
        verifyArcs(ship, distant, 0);
        assertEquals(EntropyProjectorListener.CHARGE_SECONDS, listener.chargedSeconds(), 0.001f);

        ShipAPI close = enemyAt(300f);
        when(ship.isPhased()).thenReturn(true);
        listener.advance(1f);
        verifyArcs(ship, close, 0);

        when(ship.isPhased()).thenReturn(false);
        listener.advance(EntropyProjectorListener.TARGET_SEARCH_SECONDS);
        verifyArcs(ship, close, 2);
    }

    @Test
    void doesNothingWithoutArcs() {
        ShipAPI ship = projector(0f);
        ShipAPI close = enemyAt(300f);
        EntropyProjectorListener listener = new EntropyProjectorListener(ship);

        listener.advance(20f);
        verifyArcs(ship, close, 0);
    }
}
