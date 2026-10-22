package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BallisticPierceListenerTest {

    private MockedStatic<Global> global;
    private MockedStatic<WeaponDroneFactory> factory;
    private DynamicStatsAPI dynamic;
    private PierceDrones drones;
    private ShipAPI ship;
    private ShipAPI enemy;
    private BallisticPierceListener listener;

    @BeforeEach
    void setUp() {
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getCombatEngine).thenReturn(mock(CombatEngineAPI.class));
        factory = Mockito.mockStatic(WeaponDroneFactory.class);
        factory.when(() -> WeaponDroneFactory.supportsProjectile(any())).thenReturn(true);
        ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(0);
        when(ship.isAlive()).thenReturn(true);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(BallisticPierceListener.CHANCE_KEY, 0f)).thenReturn(100f);
        enemy = mock(ShipAPI.class);
        when(enemy.getOwner()).thenReturn(1);
        when(enemy.isAlive()).thenReturn(true);
        drones = mock(PierceDrones.class);
        listener = new BallisticPierceListener(ship, drones);
    }

    @AfterEach
    void tearDown() {
        factory.close();
        global.close();
    }

    private static WeaponAPI weapon(WeaponAPI.WeaponType type, boolean beam) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getId()).thenReturn("gun");
        when(weapon.getType()).thenReturn(type);
        when(weapon.isBeam()).thenReturn(beam);
        return weapon;
    }

    private static DamagingProjectileAPI shot(WeaponAPI weapon) {
        DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class);
        Map<String, Object> data = new HashMap<>();
        when(projectile.getWeapon()).thenReturn(weapon);
        when(projectile.getCustomData()).thenReturn(data);
        when(projectile.getFacing()).thenReturn(30f);
        Mockito.doAnswer(call -> data.put(call.getArgument(0), call.getArgument(1))).when(projectile)
                .setCustomData(Mockito.anyString(), any());
        return projectile;
    }

    private void hit(DamagingProjectileAPI projectile, ShipAPI target, boolean shieldHit) {
        listener.modifyDamageDealt(projectile, target, mock(DamageAPI.class), new Vector2f(5f, 6f), shieldHit);
    }

    @Test
    void aBallisticShotOnHullOrArmorPiercesAlongItsLineOfTravel() {
        WeaponAPI gun = weapon(WeaponAPI.WeaponType.BALLISTIC, false);

        hit(shot(gun), enemy, false);

        verify(drones).launch(Mockito.eq(gun), Mockito.eq(enemy), Mockito.eq(new Vector2f(5f, 6f)), Mockito.eq(30f));
    }

    @Test
    void shieldHitsOtherWeaponTypesAndAlreadyPiercedShotsNeverPierce() {
        hit(shot(weapon(WeaponAPI.WeaponType.BALLISTIC, false)), enemy, true);
        hit(shot(weapon(WeaponAPI.WeaponType.ENERGY, false)), enemy, false);
        hit(shot(weapon(WeaponAPI.WeaponType.BALLISTIC, true)), enemy, false);
        DamagingProjectileAPI copy = shot(weapon(WeaponAPI.WeaponType.BALLISTIC, false));
        BallisticPierceListener.markPierced(copy);
        hit(copy, enemy, false);

        assertTrue(BallisticPierceListener.isPierced(copy));
        verify(drones, never()).launch(any(), any(), any(), anyFloat());
    }

    @Test
    void proximityFusedShellsNeverPierceSoTheirVisualCannotDetonate() {
        DamagingProjectileAPI flak = shot(weapon(WeaponAPI.WeaponType.BALLISTIC, false));
        when(flak.getAI()).thenReturn(mock(com.fs.starfarer.api.combat.ProximityFuseAIAPI.class));

        hit(flak, enemy, false);

        verify(drones, never()).launch(any(), any(), any(), anyFloat());
    }

    @Test
    void aShellsRealPathComesFromItsVelocitySoStrafingShotsPierceAlongTheirTrueLine() {
        DamagingProjectileAPI strafed = shot(weapon(WeaponAPI.WeaponType.BALLISTIC, false));
        when(strafed.getSpawnType()).thenReturn(com.fs.starfarer.api.loading.ProjectileSpawnType.BALLISTIC);
        when(strafed.getVelocity()).thenReturn(new Vector2f(0f, 100f));
        DamagingProjectileAPI ray = shot(weapon(WeaponAPI.WeaponType.BALLISTIC, false));
        when(ray.getSpawnType()).thenReturn(com.fs.starfarer.api.loading.ProjectileSpawnType.BALLISTIC_AS_BEAM);
        when(ray.getVelocity()).thenReturn(new Vector2f(0f, 100f));

        org.junit.jupiter.api.Assertions.assertEquals(90f, BallisticPierceListener.travelFacing(strafed), 1e-3f);
        org.junit.jupiter.api.Assertions.assertEquals(30f, BallisticPierceListener.travelFacing(ray), 1e-3f);
    }

    @Test
    void fightersFriendliesAndAZeroChanceAreLeftAlone() {
        WeaponAPI gun = weapon(WeaponAPI.WeaponType.BALLISTIC, false);
        ShipAPI fighter = mock(ShipAPI.class);
        when(fighter.getOwner()).thenReturn(1);
        when(fighter.isAlive()).thenReturn(true);
        when(fighter.isFighter()).thenReturn(true);
        ShipAPI friend = mock(ShipAPI.class);
        when(friend.getOwner()).thenReturn(0);
        when(friend.isAlive()).thenReturn(true);

        hit(shot(gun), fighter, false);
        hit(shot(gun), friend, false);
        when(dynamic.getValue(BallisticPierceListener.CHANCE_KEY, 0f)).thenReturn(0f);
        hit(shot(gun), enemy, false);

        verify(drones, never()).launch(any(), any(), any(), anyFloat());
    }
}
