package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ProximityFuseAIAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.DerivedWeaponStatsAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import exiledsector.skills.skilleffect.weapon.RiftExplosions;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class RiftStrikeListenerTest {

    private static ShipAPI ship(String id, int owner, float chancePercent) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic().getValue(RiftStrike.CHANCE_PERCENT_KEY, 0f)).thenReturn(chancePercent);
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    private static WeaponSpecAPI spec(WeaponType type, boolean beam, float damagePerShot) {
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        DerivedWeaponStatsAPI derived = mock(DerivedWeaponStatsAPI.class);
        when(derived.getDamagePerShot()).thenReturn(damagePerShot);
        when(spec.getDerivedStats()).thenReturn(derived);
        when(spec.getType()).thenReturn(type);
        when(spec.isBeam()).thenReturn(beam);
        return spec;
    }

    private static DamagingProjectileAPI shot(WeaponType type, boolean beam, float damagePerShot) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        WeaponSpecAPI spec = spec(type, beam, damagePerShot);
        when(weapon.getType()).thenReturn(type);
        when(weapon.isBeam()).thenReturn(beam);
        when(weapon.getSpec()).thenReturn(spec);
        DamagingProjectileAPI shot = mock(DamagingProjectileAPI.class);
        when(shot.getWeapon()).thenReturn(weapon);
        return shot;
    }

    private static Random rolling(float roll) {
        Random random = mock(Random.class);
        when(random.nextFloat()).thenReturn(roll);
        return random;
    }

    private static CombatEngineAPI engineSpawning(MissileAPI rift) {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any())).thenReturn(rift);
        return engine;
    }

    private static MissileAPI rift() {
        MissileAPI rift = mock(MissileAPI.class);
        MissileAIPlugin fuse = mock(MissileAIPlugin.class, withSettings().extraInterfaces(ProximityFuseAIAPI.class));
        when(rift.getMissileAI()).thenReturn(fuse);
        when(rift.getDamage()).thenReturn(mock(DamageAPI.class));
        when(rift.getVelocity()).thenReturn(new Vector2f(10f, 10f));
        when(rift.getMaxFlightTime()).thenReturn(20f);
        return rift;
    }

    @Test
    void riftDamageIsHalfTheWeaponsBaseDamageBetweenTheMinimumAndMaximum() {
        assertEquals(30f, RiftStrike.baseRiftDamage(spec(WeaponType.ENERGY, false, 25f)), 0.001f);
        assertEquals(50f, RiftStrike.baseRiftDamage(spec(WeaponType.ENERGY, false, 100f)), 0.001f);
        assertEquals(250f, RiftStrike.baseRiftDamage(spec(WeaponType.ENERGY, false, 500f)), 0.001f);
        assertEquals(500f, RiftStrike.baseRiftDamage(spec(WeaponType.ENERGY, false, 2500f)), 0.001f);
        assertEquals(330f, RiftStrike.riftDamage(spec(WeaponType.ENERGY, false, 600f), 10f), 0.001f);
    }

    @Test
    void onlyNonBeamEnergyWeaponsFireRiftsUnlessBallisticsCountAsEnergy() {
        assertTrue(RiftStrike.firesRifts(spec(WeaponType.ENERGY, false, 100f), false));
        assertFalse(RiftStrike.firesRifts(spec(WeaponType.ENERGY, true, 100f), false));
        assertFalse(RiftStrike.firesRifts(spec(WeaponType.BALLISTIC, false, 100f), false));
        assertTrue(RiftStrike.firesRifts(spec(WeaponType.BALLISTIC, false, 100f), true));
        assertFalse(RiftStrike.firesRifts(spec(WeaponType.MISSILE, false, 100f), true));
        assertFalse(RiftStrike.firesRifts(null, true));
    }

    @Test
    void aSuccessfulRollDropsARiftAtTheHitPointWithTheWeaponsRiftDamage() {
        ShipAPI ship = ship("rifter", 0, 30f);
        ShipAPI target = ship("target", 1, 0f);
        MissileAPI rift = rift();
        CombatEngineAPI engine = engineSpawning(rift);
        Vector2f point = new Vector2f(40f, 60f);
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            assertNull(new RiftStrikeListener(ship, rolling(0.29f)).modifyDamageDealt(shot(WeaponType.ENERGY, false, 500f), target,
                    mock(DamageAPI.class), point, true));

            verify(engine).spawnProjectile(eq(ship), eq(null), eq(RiftExplosions.RIFT_MINE_WEAPON_ID), eq(point), anyFloat(), eq(null));
            InOrder damageThenFuse = inOrder(rift.getDamage(), rift.getMissileAI());
            damageThenFuse.verify(rift.getDamage()).setDamage(250f);
            damageThenFuse.verify((ProximityFuseAIAPI) rift.getMissileAI()).updateDamage();
            verify(rift).setFlightTime(20f);
            verify(rift).addDamagedAlready(ship);
            assertEquals(0f, rift.getVelocity().length(), 0.001f);
        }
    }

    @Test
    void noRiftOnAFailedRollABeamOrAFriendlyHit() {
        ShipAPI ship = ship("rifter", 0, 30f);
        ShipAPI enemy = ship("enemy", 1, 0f);
        ShipAPI ally = ship("ally", 0, 0f);
        CombatEngineAPI engine = engineSpawning(rift());
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            new RiftStrikeListener(ship, rolling(0.31f)).modifyDamageDealt(shot(WeaponType.ENERGY, false, 100f), enemy,
                    mock(DamageAPI.class), new Vector2f(), false);
            new RiftStrikeListener(ship, rolling(0f)).modifyDamageDealt(shot(WeaponType.ENERGY, true, 100f), enemy,
                    mock(DamageAPI.class), new Vector2f(), false);
            new RiftStrikeListener(ship, rolling(0f)).modifyDamageDealt(shot(WeaponType.ENERGY, false, 100f), ally,
                    mock(DamageAPI.class), new Vector2f(), false);

            verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
        }
    }
}
