package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PierceDroneTest {

    private static final float FRAME = 0.016f;

    private MockedStatic<Global> global;
    private MockedStatic<WeaponDroneStats> stats;
    private CombatEngineAPI engine;
    private final List<DamagingProjectileAPI> projectiles = new ArrayList<>();
    private final Vector2f droneLocation = new Vector2f();
    private ShipAPI droneShip;
    private ShipAPI firingShip;
    private ShipAPI hitShip;
    private WeaponAPI weapon;
    private PierceDrones pool;
    private PierceDrone drone;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.getProjectiles()).thenReturn(projectiles);
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getCombatEngine).thenReturn(engine);
        stats = Mockito.mockStatic(WeaponDroneStats.class);

        weapon = mock(WeaponAPI.class);
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        when(weapon.getSpec()).thenReturn(spec);
        when(spec.getWeaponId()).thenReturn("ballistic_gun");
        when(weapon.getRange()).thenReturn(600f);
        when(weapon.getProjectileSpeed()).thenReturn(1000f);
        when(weapon.getCooldown()).thenReturn(WeaponDroneFactory.SINGLE_SHOT_REFIRE_DELAY);

        droneShip = mock(ShipAPI.class);
        MutableShipStatsAPI droneStats = mock(MutableShipStatsAPI.class);
        when(droneShip.getMutableStats()).thenReturn(droneStats);
        when(droneStats.getBallisticWeaponRangeBonus()).thenReturn(new StatBonus());
        when(droneShip.getAllWeapons()).thenReturn(List.of(weapon));
        when(droneShip.getLocation()).thenReturn(droneLocation);
        when(droneShip.getVelocity()).thenReturn(new Vector2f(30f, 40f));
        when(droneShip.getMouseTarget()).thenReturn(new Vector2f());
        when(engine.isEntityInPlay(droneShip)).thenReturn(true);
        ShipVariantAPI droneVariant = mock(ShipVariantAPI.class);
        when(droneVariant.getHullMods()).thenReturn(new LinkedHashSet<>());
        when(droneShip.getVariant()).thenReturn(droneVariant);

        firingShip = mock(ShipAPI.class);
        when(firingShip.getMutableStats()).thenReturn(mock(MutableShipStatsAPI.class));
        when(firingShip.getVariant()).thenReturn(mock(ShipVariantAPI.class));
        hitShip = mock(ShipAPI.class);
        when(hitShip.getLocation()).thenReturn(new Vector2f());
        when(hitShip.getCollisionRadius()).thenReturn(100f);
        when(engine.isEntityInPlay(hitShip)).thenReturn(true);
        pool = mock(PierceDrones.class);
        when(pool.firingShip()).thenReturn(firingShip);
        drone = new PierceDrone(droneShip, pool);
    }

    @AfterEach
    void tearDown() {
        stats.close();
        global.close();
    }

    private DamagingProjectileAPI projectileFrom(WeaponAPI firedBy) {
        DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class);
        Map<String, Object> data = new HashMap<>();
        when(projectile.getWeapon()).thenReturn(firedBy);
        when(projectile.getCustomData()).thenReturn(data);
        doAnswer(invocation -> data.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(projectile).setCustomData(any(), any());
        return projectile;
    }

    private void launchThroughTheHitShip() {
        drone.launch(hitShip, new Vector2f(-100f, 0f), 0f, 600f);
    }

    @Test
    void launchParksTheDroneBeyondTheFarSideAndHoldsFireWhileTheShellCrosses() {
        launchThroughTheHitShip();
        drone.advance(FRAME);

        assertEquals(110f, droneLocation.x, 0.01f);
        assertEquals(0f, droneLocation.y, 0.01f);
        assertEquals(new Vector2f(), droneShip.getVelocity());
        verify(weapon, never()).setForceFireOneFrame(anyBoolean());
        assertFalse(drone.isReady());
    }

    @Test
    void onceTheShellHasCrossedItFiresDeviatedAwayFromTheHitShip() {
        launchThroughTheHitShip();

        drone.advance(0.25f);

        ArgumentCaptor<Float> facing = ArgumentCaptor.forClass(Float.class);
        verify(weapon).setFacing(facing.capture());
        assertTrue(facing.getValue() >= 0f && facing.getValue() <= PierceDrone.MAX_DEVIATION_DEGREES);
        verify(weapon).setForceFireOneFrame(true);
        verify(droneShip).setShipTarget(null);
        assertFalse(drone.isReady());
    }

    @Test
    void itMarksItsOwnShotsPiercedHandsThemBackAndRearms() {
        launchThroughTheHitShip();
        drone.advance(0.25f);
        DamagingProjectileAPI own = projectileFrom(weapon);
        DamagingProjectileAPI other = projectileFrom(mock(WeaponAPI.class));
        projectiles.addAll(List.of(own, other));
        when(weapon.getChargeLevel()).thenReturn(1f);

        drone.advance(FRAME);

        assertTrue(BallisticPierceListener.isPierced(own));
        verify(own).setSource(firingShip);
        assertFalse(BallisticPierceListener.isPierced(other));
        assertTrue(drone.isReady());
        verify(weapon).setRemainingCooldownTo(0f);
    }

    @Test
    void removingADroneMidCrossingTakesItOutOfThePool() {
        launchThroughTheHitShip();

        drone.remove();

        verify(engine).removeEntity(droneShip);
        verify(pool).forget(drone);
        assertFalse(drone.isReady());
        assertFalse(drone.isInPlay());
    }
}
