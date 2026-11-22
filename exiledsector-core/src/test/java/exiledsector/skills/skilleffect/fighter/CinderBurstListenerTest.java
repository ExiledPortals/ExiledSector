package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.CombatTestShips;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CinderBurstListenerTest {

    private final List<Object> gridShips = new ArrayList<>();
    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();
    private CombatEngineAPI engine;

    @BeforeEach
    void setUp() {
        engine = combat.engine();
        combat.shipGridOver(gridShips);
    }

    private static ShipAPI ship(int owner, float x, float collisionRadius) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.getVelocity()).thenReturn(new Vector2f());
        when(ship.getCollisionRadius()).thenReturn(collisionRadius);
        when(ship.getCollisionClass()).thenReturn(CollisionClass.SHIP);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getHitpoints()).thenReturn(100f);
        return ship;
    }

    private static ShipAPI carrier(float burstDamage) {
        return CombatTestShips.withDynamic(CinderBurstListener.DAMAGE_KEY, burstDamage);
    }

    @Test
    void aLethalHitBurstsOnceOntoEnemiesInRange() {
        ShipAPI fighter = ship(0, 0f, 10f);
        ShipAPI enemy = ship(1, 100f, 20f);
        ShipAPI enemyCapital = ship(1, 400f, 300f);
        ShipAPI ally = ship(0, 50f, 20f);
        gridShips.addAll(List.of(fighter, enemy, enemyCapital, ally));
        CinderBurstListener listener = new CinderBurstListener(fighter, carrier(120f));

        assertFalse(listener.notifyAboutToTakeHullDamage(null, fighter, new Vector2f(), 50f));
        verify(engine, never()).applyDamage(any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());

        listener.notifyAboutToTakeHullDamage(null, fighter, new Vector2f(), 150f);
        listener.notifyAboutToTakeHullDamage(null, fighter, new Vector2f(), 150f);

        verify(engine, times(1)).applyDamage(eq(enemy), any(), eq(120f), eq(DamageType.FRAGMENTATION), eq(0f), eq(false), eq(false), eq(fighter));
        verify(engine, times(1)).applyDamage(eq(enemyCapital), any(), eq(120f), eq(DamageType.FRAGMENTATION), eq(0f), eq(false), eq(false), eq(fighter));
        verify(engine, never()).applyDamage(eq(ally), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void theBurstReachesShipsWhoseEdgeIsInRange() {
        ShipAPI fighter = ship(0, 0f, 10f);
        Vector2f burstPoint = new Vector2f();

        assertTrue(StrikecraftDeathBurst.caughtInBurst(fighter, ship(1, 200f, 60f), burstPoint, CinderBurstListener.BURST_RADIUS));
        assertFalse(StrikecraftDeathBurst.caughtInBurst(fighter, ship(1, 200f, 40f), burstPoint, CinderBurstListener.BURST_RADIUS));
        assertFalse(StrikecraftDeathBurst.caughtInBurst(fighter, fighter, burstPoint, CinderBurstListener.BURST_RADIUS));
    }
}
