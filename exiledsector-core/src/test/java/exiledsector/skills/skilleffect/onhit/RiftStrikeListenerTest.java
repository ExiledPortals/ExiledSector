package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
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
import static org.mockito.Mockito.times;
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

    private static DamagingProjectileAPI shot(ShipAPI firingShip, WeaponType type, boolean beam, float damagePerShot) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getShip()).thenReturn(firingShip);
        WeaponSpecAPI spec = spec(type, beam, damagePerShot);
        when(weapon.getType()).thenReturn(type);
        when(weapon.isBeam()).thenReturn(beam);
        when(weapon.getSpec()).thenReturn(spec);
        DamagingProjectileAPI shot = mock(DamagingProjectileAPI.class);
        when(shot.getWeapon()).thenReturn(weapon);
        return shot;
    }

    private static BeamAPI beam(ShipAPI firingShip, float damagePerSecond) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getShip()).thenReturn(firingShip);
        WeaponSpecAPI spec = spec(WeaponType.ENERGY, true, 0f);
        when(spec.getDerivedStats().getDps()).thenReturn(damagePerSecond);
        when(weapon.getType()).thenReturn(WeaponType.ENERGY);
        when(weapon.isBeam()).thenReturn(true);
        when(weapon.getSpec()).thenReturn(spec);
        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getWeapon()).thenReturn(weapon);
        return beam;
    }

    private static RiftStrikeListener listener(ShipAPI ship, Random random) {
        return new RiftStrikeListener(ship, ship, RiftStrike.CHANCE_PERCENT_KEY, RiftStrike.BEAM_CHANCE_PERCENT_KEY, random);
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

            assertNull(listener(ship, rolling(0.29f)).modifyDamageDealt(shot(ship, WeaponType.ENERGY, false, 500f), target,
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

            listener(ship, rolling(0.31f)).modifyDamageDealt(shot(ship, WeaponType.ENERGY, false, 100f), enemy,
                    mock(DamageAPI.class), new Vector2f(), false);
            listener(ship, rolling(0f)).modifyDamageDealt(shot(ship, WeaponType.ENERGY, true, 100f), enemy,
                    mock(DamageAPI.class), new Vector2f(), false);
            listener(ship, rolling(0f)).modifyDamageDealt(shot(ship, WeaponType.ENERGY, false, 100f), ally,
                    mock(DamageAPI.class), new Vector2f(), false);

            verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
        }
    }

    @Test
    void beamRiftDamageIsHalfTheBeamsBaseDamagePerSecond() {
        WeaponSpecAPI beamSpec = spec(WeaponType.ENERGY, true, 0f);
        when(beamSpec.getDerivedStats().getDps()).thenReturn(400f);

        assertEquals(200f, RiftStrike.baseRiftDamage(beamSpec), 0.001f);
        assertTrue(RiftStrike.beamFiresRifts(beamSpec, false));
        assertFalse(RiftStrike.beamFiresRifts(spec(WeaponType.ENERGY, false, 100f), false));
        assertFalse(RiftStrike.beamFiresRifts(spec(WeaponType.BALLISTIC, true, 100f), false));
        assertTrue(RiftStrike.beamFiresRifts(spec(WeaponType.BALLISTIC, true, 100f), true));
    }

    @Test
    void eachBeamWeaponRollsTheBeamChanceAtMostOnceASecond() {
        ShipAPI ship = ship("rifter", 0, 0f);
        when(ship.getMutableStats().getDynamic().getValue(RiftStrike.BEAM_CHANCE_PERCENT_KEY, 0f)).thenReturn(30f);
        ShipAPI enemy = ship("enemy", 1, 0f);
        MissileAPI rift = rift();
        CombatEngineAPI engine = engineSpawning(rift);
        BeamAPI firstBeam = beam(ship, 400f);
        BeamAPI secondBeam = beam(ship, 400f);
        RiftStrikeListener listener = listener(ship, rolling(0.29f));
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            when(engine.getTotalElapsedTime(false)).thenReturn(10f);
            listener.modifyDamageDealt(firstBeam, enemy, mock(DamageAPI.class), new Vector2f(), false);
            when(engine.getTotalElapsedTime(false)).thenReturn(10.5f);
            listener.modifyDamageDealt(firstBeam, enemy, mock(DamageAPI.class), new Vector2f(), false);
            listener.modifyDamageDealt(secondBeam, enemy, mock(DamageAPI.class), new Vector2f(), false);
            verify(engine, times(2)).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
            verify(rift.getDamage(), times(2)).setDamage(200f);

            when(engine.getTotalElapsedTime(false)).thenReturn(11f);
            listener.modifyDamageDealt(firstBeam, enemy, mock(DamageAPI.class), new Vector2f(), false);
            verify(engine, times(3)).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
        }
    }

    @Test
    void beamsNeverRollWithoutTheBeamChanceOrFromStrikecraft() {
        ShipAPI ship = ship("rifter", 0, 100f);
        ShipAPI fighter = ship("fighter", 0, 0f);
        ShipAPI carrier = ship("carrier", 0, 0f);
        when(carrier.getMutableStats().getDynamic().getValue(RiftStrike.STRIKECRAFT_CHANCE_PERCENT_KEY, 0f)).thenReturn(100f);
        when(carrier.getMutableStats().getDynamic().getValue(RiftStrike.BEAM_CHANCE_PERCENT_KEY, 0f)).thenReturn(100f);
        CombatEngineAPI engine = engineSpawning(rift());
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            listener(ship, rolling(0f)).modifyDamageDealt(beam(ship, 400f), ship("enemy", 1, 0f), mock(DamageAPI.class), new Vector2f(), false);
            new RiftStrikeListener(fighter, carrier, RiftStrike.STRIKECRAFT_CHANCE_PERCENT_KEY, null, rolling(0f))
                    .modifyDamageDealt(beam(fighter, 400f), ship("enemy", 1, 0f), mock(DamageAPI.class), new Vector2f(), false);

            verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
        }
    }

    @Test
    void shotsFromAnotherShipsWeaponNeverRoll() {
        ShipAPI ship = ship("rifter", 0, 100f);
        ShipAPI weaponDrone = ship("drone", 0, 0f);
        CombatEngineAPI engine = engineSpawning(rift());
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            listener(ship, rolling(0f)).modifyDamageDealt(shot(weaponDrone, WeaponType.ENERGY, false, 100f), ship("enemy", 1, 0f),
                    mock(DamageAPI.class), new Vector2f(), false);

            verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
        }
    }

    @Test
    void aStrikecraftRollsItsCarriersChanceAndSourcesTheRiftItself() {
        ShipAPI fighter = ship("fighter", 0, 0f);
        ShipAPI carrier = ship("carrier", 0, 0f);
        when(carrier.getMutableStats().getDynamic().getValue(RiftStrike.STRIKECRAFT_CHANCE_PERCENT_KEY, 0f)).thenReturn(10f);
        MissileAPI rift = rift();
        CombatEngineAPI engine = engineSpawning(rift);
        Vector2f point = new Vector2f(15f, 25f);
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            new RiftStrikeListener(fighter, carrier, RiftStrike.STRIKECRAFT_CHANCE_PERCENT_KEY, null, rolling(0.11f))
                    .modifyDamageDealt(shot(fighter, WeaponType.ENERGY, false, 200f), ship("enemy", 1, 0f), mock(DamageAPI.class), point, false);
            verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());

            new RiftStrikeListener(fighter, carrier, RiftStrike.STRIKECRAFT_CHANCE_PERCENT_KEY, null, rolling(0.09f))
                    .modifyDamageDealt(shot(fighter, WeaponType.ENERGY, false, 200f), ship("enemy", 1, 0f), mock(DamageAPI.class), point, false);

            verify(engine).spawnProjectile(eq(fighter), eq(null), eq(RiftExplosions.RIFT_MINE_WEAPON_ID), eq(point), anyFloat(), eq(null));
            verify(rift.getDamage()).setDamage(100f);
            verify(rift).addDamagedAlready(fighter);
        }
    }
}
