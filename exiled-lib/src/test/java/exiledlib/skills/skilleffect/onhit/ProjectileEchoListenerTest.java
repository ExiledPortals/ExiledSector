package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectileEchoListenerTest {

    private MockedStatic<Global> global;
    private CombatEngineAPI engine;
    private CombatEntityAPI echo;
    private ShipAPI ship;
    private ShipAPI enemy;
    private ProjectileEchoListener listener;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        echo = mock(CombatEntityAPI.class);
        when(engine.spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any())).thenReturn(echo);
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getCombatEngine).thenReturn(engine);
        ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(0);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getVelocity()).thenReturn(new Vector2f());
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(ProjectileEchoListener.CHANCE_KEY, 0f)).thenReturn(100f);
        enemy = mock(ShipAPI.class);
        when(enemy.getOwner()).thenReturn(1);
        when(enemy.isAlive()).thenReturn(true);
        when(enemy.getLocation()).thenReturn(new Vector2f(100f, 0f));
        listener = new ProjectileEchoListener(ship);
    }

    @AfterEach
    void tearDown() {
        global.close();
    }

    private static WeaponAPI weapon(WeaponAPI.WeaponType type, boolean beam) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getId()).thenReturn("gun");
        when(weapon.getType()).thenReturn(type);
        when(weapon.isBeam()).thenReturn(beam);
        when(weapon.getLocation()).thenReturn(new Vector2f());
        return weapon;
    }

    private DamagingProjectileAPI shot(DamagingProjectileAPI projectile, WeaponAPI weapon, Map<String, Object> data) {
        when(projectile.getWeapon()).thenReturn(weapon);
        when(projectile.getSource()).thenReturn(ship);
        when(projectile.getCustomData()).thenReturn(data);
        return projectile;
    }

    @Test
    void aHitFromANonMissileProjectileFiresAMarkedEchoFromTheSameWeaponTowardTheTarget() {
        WeaponAPI gun = weapon(WeaponAPI.WeaponType.BALLISTIC, false);

        listener.handleHit(shot(mock(DamagingProjectileAPI.class), gun, new HashMap<>()), enemy);

        verify(engine).spawnProjectile(eq(ship), eq(gun), eq("gun"), any(), eq(0f), any());
        verify(echo).setCustomData(ProjectileEchoListener.ECHO_KEY, Boolean.TRUE);
    }

    @Test
    void echoesMissilesAndBeamsNeverEcho() {
        Map<String, Object> echoData = new HashMap<>(Map.of(ProjectileEchoListener.ECHO_KEY, Boolean.TRUE));

        listener.handleHit(shot(mock(DamagingProjectileAPI.class), weapon(WeaponAPI.WeaponType.ENERGY, false), echoData), enemy);
        listener.handleHit(shot(mock(MissileAPI.class), weapon(WeaponAPI.WeaponType.MISSILE, false), new HashMap<>()), enemy);
        listener.handleHit(shot(mock(DamagingProjectileAPI.class), weapon(WeaponAPI.WeaponType.ENERGY, true), new HashMap<>()), enemy);

        verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
        assertTrue(ProjectileEchoListener.isEcho(shot(mock(DamagingProjectileAPI.class), null, echoData)));
        assertFalse(ProjectileEchoListener.canEcho(shot(mock(DamagingProjectileAPI.class), null, new HashMap<>()), null));
    }
}
