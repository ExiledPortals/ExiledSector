package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollisionDamageListenerTest {

    private static final float EPSILON = 1e-4f;

    private MockedStatic<Global> globalMock;
    private CollisionGridAPI shipGrid;
    private CollisionGridAPI asteroidGrid;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        shipGrid = mock(CollisionGridAPI.class);
        asteroidGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(engine.getAsteroidGrid()).thenReturn(asteroidGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> List.of().iterator());
        when(asteroidGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> List.of().iterator());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI ship(String id, int owner, float x) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.getCollisionRadius()).thenReturn(100f);
        when(ship.getCollisionClass()).thenReturn(CollisionClass.SHIP);
        when(ship.getMutableStats()).thenReturn(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS));
        return ship;
    }

    private static ShipAPI rammer(float percent) {
        ShipAPI rammer = ship("rammer", 0, 0f);
        when(rammer.getMutableStats().getDynamic().getValue(CollisionDamageListener.COLLISION_DAMAGE_PERCENT_KEY, 0f)).thenReturn(percent);
        return rammer;
    }

    private static DamageAPI collision() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getType()).thenReturn(DamageType.KINETIC);
        when(damage.getModifier()).thenReturn(mock(MutableStat.class));
        return damage;
    }

    private void touching(Object... colliders) {
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> List.of(colliders).iterator());
    }

    @Test
    void ramsDealMoreCollisionDamageToEnemyShipsInContact() {
        DamageAPI hit = collision();

        String modId = new CollisionDamageListener(rammer(50f)).modifyDamageDealt(null, ship("victim", 1, 150f), hit, new Vector2f(), false);

        assertEquals("exiledSector_ramProwCollisionDamage_rammer", modId);
        verify(hit.getModifier()).modifyMult(modId, 1.5f);
    }

    @Test
    void weaponHitsAlliesAndDistantShipsGetNoRamBonus() {
        CollisionDamageListener listener = new CollisionDamageListener(rammer(50f));
        DamageAPI hit = collision();
        DamageAPI weaponHit = collision();
        when(weaponHit.getStats()).thenReturn(mock(MutableShipStatsAPI.class));

        assertNull(listener.modifyDamageDealt(mock(DamagingProjectileAPI.class), ship("victim", 1, 150f), hit, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, ship("victim", 1, 150f), weaponHit, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, ship("ally", 0, 150f), hit, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, ship("distant", 1, 1000f), hit, new Vector2f(), false));
        verify(hit.getModifier(), never()).modifyMult(anyString(), anyFloat());
        verify(weaponHit.getModifier(), never()).modifyMult(anyString(), anyFloat());
    }

    @Test
    void collisionsWithAnythingInContactDealLessDamageToTheRammer() {
        ShipAPI rammer = rammer(50f);
        touching(rammer, ship("victim", 1, 150f));
        DamageAPI hit = collision();

        String modId = new CollisionDamageListener(rammer).modifyDamageTaken(null, rammer, hit, new Vector2f(), false);

        assertEquals("exiledSector_ramProwCollisionDamage_rammer", modId);
        verify(hit.getModifier()).modifyMult(modId, 0.5f);
    }

    @Test
    void scriptedDamageWithNothingInContactIsNotReduced() {
        ShipAPI rammer = rammer(50f);
        touching(rammer, ship("distant", 1, 1000f));
        DamageAPI hit = collision();
        DamageAPI highExplosive = collision();
        when(highExplosive.getType()).thenReturn(DamageType.HIGH_EXPLOSIVE);
        CollisionDamageListener listener = new CollisionDamageListener(rammer);

        assertNull(listener.modifyDamageTaken(null, rammer, hit, new Vector2f(), false));
        touching(ship("victim", 1, 150f));
        assertNull(listener.modifyDamageTaken(null, rammer, highExplosive, new Vector2f(), false));
        verify(hit.getModifier(), never()).modifyMult(anyString(), anyFloat());
        verify(highExplosive.getModifier(), never()).modifyMult(anyString(), anyFloat());
    }

    @Test
    void stackedReductionsStopAtNoDamageTaken() {
        assertEquals(0.25f, CollisionDamageListener.takenMult(75f), EPSILON);
        assertEquals(0f, CollisionDamageListener.takenMult(150f), EPSILON);
        assertEquals(2.5f, CollisionDamageListener.dealtMult(150f), EPSILON);
    }
}
