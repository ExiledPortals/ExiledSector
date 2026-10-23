package exiledlib.skills.skilleffect.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeathExplosionListenerTest {

    private static final String FUEL_DAMAGE_PERCENT_KEY = "exiledSector_explodeOnDeathFuelDamagePercent";

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

    private ShipAPI explodingShip(float fuelCapacity, float fuelDamagePercent) {
        ShipAPI ship = ship(0f, 0f, Map.of(FUEL_DAMAGE_PERCENT_KEY, fuelDamagePercent));
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getFuelCapacity()).thenReturn(fuelCapacity);
        when(ship.getMutableStats().getFleetMember()).thenReturn(member);
        when(ship.getCollisionRadius()).thenReturn(50f);
        when(ship.getHitpoints()).thenReturn(100f);
        return ship;
    }

    @Test
    void aLethalHitDetonatesTheFuelWithDamageFallingOffOverTheBlastRadius() {
        ShipAPI ship = explodingShip(200f, 50f);
        ShipAPI near = ship(150f, 0f, Map.of());
        ShipAPI outside = ship(400f, 0f, Map.of());
        ShipAPI hulk = ship(100f, 0f, Map.of());
        when(hulk.isHulk()).thenReturn(true);
        ships.addAll(List.of(ship, near, outside, hulk));
        HullDamageAboutToBeTakenListener listener = attachedListener(SpecialSkillEffect.EXPLODE_ON_DEATH, ship,
                HullDamageAboutToBeTakenListener.class);

        assertFalse(listener.notifyAboutToTakeHullDamage(null, ship, new Vector2f(), 150f));

        verify(engine).applyDamage(near, near.getLocation(), 50f, DamageType.HIGH_EXPLOSIVE, 0f, true, false, ship);
        verify(engine, never()).applyDamage(eq(outside), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
        verify(engine, never()).applyDamage(eq(hulk), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
        verify(engine, never()).applyDamage(eq(ship), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
        verify(engine, times(2)).spawnExplosion(any(), any(), any(), anyFloat(), anyFloat());
    }

    @Test
    void theShipOnlyExplodesOnceAndNotOnAHitItSurvives() {
        ShipAPI ship = explodingShip(200f, 50f);
        ships.add(ship);
        HullDamageAboutToBeTakenListener listener = attachedListener(SpecialSkillEffect.EXPLODE_ON_DEATH, ship,
                HullDamageAboutToBeTakenListener.class);

        listener.notifyAboutToTakeHullDamage(null, ship, new Vector2f(), 50f);
        verify(engine, never()).spawnExplosion(any(), any(), any(), anyFloat(), anyFloat());

        listener.notifyAboutToTakeHullDamage(null, ship, new Vector2f(), 150f);
        listener.notifyAboutToTakeHullDamage(null, ship, new Vector2f(), 150f);
        verify(engine, times(2)).spawnExplosion(any(), any(), any(), anyFloat(), anyFloat());
    }

    @Test
    void aShipWithoutAFleetMemberExplodesWithoutDamagingAnything() {
        ShipAPI ship = explodingShip(200f, 50f);
        when(ship.getMutableStats().getFleetMember()).thenReturn(null);
        ShipAPI near = ship(150f, 0f, Map.of());
        ships.addAll(List.of(ship, near));
        HullDamageAboutToBeTakenListener listener = attachedListener(SpecialSkillEffect.EXPLODE_ON_DEATH, ship,
                HullDamageAboutToBeTakenListener.class);

        listener.notifyAboutToTakeHullDamage(null, ship, new Vector2f(), 150f);

        verify(engine, never()).applyDamage(any(CombatEntityAPI.class), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
        verify(engine, times(2)).spawnExplosion(any(), any(), any(), anyFloat(), anyFloat());
    }
}
