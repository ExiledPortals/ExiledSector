package exiledlib.skills.skilleffect.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledlib.skills.skilleffect.SkillEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollisionDeathListenerTest {

    private final List<ShipAPI> ships = new ArrayList<>();

    private final List<Object> gridShips = new ArrayList<>();

    private final List<Object> gridAsteroids = new ArrayList<>();

    private MockedStatic<Global> globalMock;

    private CombatEngineAPI engine;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.getShips()).thenReturn(ships);
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        CollisionGridAPI asteroidGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(engine.getAsteroidGrid()).thenReturn(asteroidGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<>(gridShips).iterator());
        when(asteroidGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<>(gridAsteroids).iterator());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI ship(float x, float y, Map<String, Float> dynamicValues) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        dynamicValues.forEach((key, value) -> when(dynamic.getValue(key, 0f)).thenReturn(value));
        when(ship.getLocation()).thenReturn(new Vector2f(x, y));
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    private static <T> T attachedListener(SkillEffect effect, ShipAPI ship, Class<T> type) {
        effect.applyAfterShipCreation(ship, "mod_id", 0f);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ship).addListener(captor.capture());
        return type.cast(captor.getValue());
    }

    private static BoundsAPI boundsWithSegment(float x1, float y1, float x2, float y2) {
        BoundsAPI bounds = mock(BoundsAPI.class);
        BoundsAPI.SegmentAPI segment = mock(BoundsAPI.SegmentAPI.class);
        when(segment.getP1()).thenReturn(new Vector2f(x1, y1));
        when(segment.getP2()).thenReturn(new Vector2f(x2, y2));
        when(bounds.getSegments()).thenReturn(List.of(segment));
        return bounds;
    }

    private ShipAPI collidingShip() {
        ShipAPI ship = ship(0f, 0f, Map.of());
        when(ship.getCollisionRadius()).thenReturn(50f);
        when(ship.getCollisionClass()).thenReturn(CollisionClass.SHIP);
        BoundsAPI bounds = boundsWithSegment(-10f, 0f, 10f, 0f);
        when(ship.getExactBounds()).thenReturn(bounds);
        return ship;
    }

    private static ShipAPI otherShipAt(float x, BoundsAPI bounds) {
        ShipAPI other = ship(x, 0f, Map.of());
        when(other.getCollisionRadius()).thenReturn(50f);
        when(other.getCollisionClass()).thenReturn(CollisionClass.SHIP);
        when(other.getExactBounds()).thenReturn(bounds);
        return other;
    }

    private void assertDestroyedByCollision(ShipAPI ship, int times) {
        verify(engine, times(times)).applyDamage(ship, ship.getLocation(), 999999f, DamageType.HIGH_EXPLOSIVE, 0f, true, false, ship);
    }

    @Test
    void aShipTouchingAnotherHullIsDestroyedOnceTheGracePeriodHasPassed() {
        ShipAPI ship = collidingShip();
        gridShips.addAll(List.of(ship, otherShipAt(5f, boundsWithSegment(0f, -10f, 0f, 10f))));
        AdvanceableListener listener = attachedListener(SpecialSkillEffect.DEATH_ON_COLLISION, ship, AdvanceableListener.class);

        listener.advance(1f);
        assertDestroyedByCollision(ship, 0);

        listener.advance(1f);
        listener.advance(1f);
        assertDestroyedByCollision(ship, 1);
    }

    @Test
    void nearbyHullsThatDoNotOverlapAndFightersAreIgnored() {
        ShipAPI ship = collidingShip();
        ShipAPI fighter = otherShipAt(5f, boundsWithSegment(0f, -10f, 0f, 10f));
        when(fighter.isFighter()).thenReturn(true);
        gridShips.addAll(List.of(ship, fighter, otherShipAt(30f, boundsWithSegment(20f, -10f, 20f, 10f))));
        AdvanceableListener listener = attachedListener(SpecialSkillEffect.DEATH_ON_COLLISION, ship, AdvanceableListener.class);

        listener.advance(2f);

        assertDestroyedByCollision(ship, 0);
    }

    @Test
    void touchingAnAsteroidNeverDestroysTheShip() {
        ShipAPI ship = collidingShip();
        CombatEntityAPI asteroid = mock(CombatEntityAPI.class);
        when(asteroid.getLocation()).thenReturn(new Vector2f(5f, 0f));
        when(asteroid.getCollisionRadius()).thenReturn(20f);
        BoundsAPI asteroidBounds = boundsWithSegment(0f, -10f, 0f, 10f);
        when(asteroid.getExactBounds()).thenReturn(asteroidBounds);
        gridAsteroids.add(asteroid);
        AdvanceableListener listener = attachedListener(SpecialSkillEffect.DEATH_ON_COLLISION, ship, AdvanceableListener.class);

        listener.advance(2f);

        assertDestroyedByCollision(ship, 0);
    }

    @Test
    void theShipsOwnModulesNeverCountAsACollision() {
        ShipAPI ship = collidingShip();
        ShipAPI module = otherShipAt(5f, boundsWithSegment(0f, -10f, 0f, 10f));
        when(module.getParentStation()).thenReturn(ship);
        gridShips.addAll(List.of(ship, module));
        AdvanceableListener listener = attachedListener(SpecialSkillEffect.DEATH_ON_COLLISION, ship, AdvanceableListener.class);

        listener.advance(2f);

        assertDestroyedByCollision(ship, 0);
    }

    @Test
    void aShipWithCollisionsDisabledIsNeverChecked() {
        ShipAPI ship = collidingShip();
        when(ship.getCollisionClass()).thenReturn(CollisionClass.NONE);
        AdvanceableListener listener = attachedListener(SpecialSkillEffect.DEATH_ON_COLLISION, ship, AdvanceableListener.class);

        listener.advance(2f);

        verify(engine, never()).getShipGrid();
    }
}
