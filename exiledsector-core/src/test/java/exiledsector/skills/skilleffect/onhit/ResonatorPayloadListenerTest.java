package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.DerivedWeaponStatsAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import exiledsector.skills.skilleffect.weapon.RiftExplosions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResonatorPayloadListenerTest {

    private CombatEngineAPI engine;
    private MissileAPI rift;
    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() {
        rift = mock(MissileAPI.class, RETURNS_DEEP_STUBS);
        engine = mock(CombatEngineAPI.class);
        when(engine.spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any())).thenReturn(rift);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI ship(int owner, float chancePercent) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic().getValue(ResonatorPayloadListener.CHANCE_PERCENT_KEY, 0f)).thenReturn(chancePercent);
        return ship;
    }

    private static WeaponSpecAPI spec(float damagePerShot) {
        DerivedWeaponStatsAPI derived = mock(DerivedWeaponStatsAPI.class);
        when(derived.getDamagePerShot()).thenReturn(damagePerShot);
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        when(spec.getDerivedStats()).thenReturn(derived);
        return spec;
    }

    private static MissileAPI missile(ShipAPI firingShip, WeaponType type, float damagePerShot) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getShip()).thenReturn(firingShip);
        WeaponSpecAPI spec = spec(damagePerShot);
        when(weapon.getType()).thenReturn(type);
        when(weapon.getSpec()).thenReturn(spec);
        MissileAPI missile = mock(MissileAPI.class);
        when(missile.getWeapon()).thenReturn(weapon);
        return missile;
    }

    private static Random rolling(float roll) {
        Random random = mock(Random.class);
        when(random.nextFloat()).thenReturn(roll);
        return random;
    }

    @Test
    void riftDamageIsATenthOfTheMissilesBaseDamageUpToTheMaximum() {
        assertEquals(25f, ResonatorPayloadListener.riftDamage(spec(250f)), 0.001f);
        assertEquals(100f, ResonatorPayloadListener.riftDamage(spec(4000f)), 0.001f);
    }

    @Test
    void aSuccessfulRollSpawnsTheWholeSeriesNearTheImpactOverTime() {
        ShipAPI ship = ship(0, 5f);
        ResonatorPayloadListener listener = new ResonatorPayloadListener(ship, rolling(0.04f));
        Vector2f impact = new Vector2f(500f, -300f);

        assertNull(listener.modifyDamageDealt(missile(ship, WeaponType.MISSILE, 750f), ship(1, 0f), mock(DamageAPI.class), impact, false));
        verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());

        listener.advance(0f);
        verify(engine, times(1)).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
        listener.advance(ResonatorPayloadListener.SECONDS_BETWEEN_RIFTS * (ResonatorPayloadListener.RIFTS_PER_SERIES - 1));

        ArgumentCaptor<Vector2f> riftPoints = ArgumentCaptor.forClass(Vector2f.class);
        verify(engine, times(ResonatorPayloadListener.RIFTS_PER_SERIES)).spawnProjectile(eq(ship), eq(null),
                eq(RiftExplosions.RIFT_MINE_WEAPON_ID), riftPoints.capture(), anyFloat(), eq(null));
        for (Vector2f riftPoint : riftPoints.getAllValues()) {
            assertTrue(Vector2f.sub(riftPoint, impact, null).length() <= ResonatorPayloadListener.SCATTER_RADIUS + 0.001f);
        }
        verify(rift.getDamage(), times(ResonatorPayloadListener.RIFTS_PER_SERIES)).setDamage(75f);

        listener.advance(1f);
        verify(engine, times(ResonatorPayloadListener.RIFTS_PER_SERIES)).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
    }

    @Test
    void theRestOfASeriesIsDroppedWhenTheShipDies() {
        ShipAPI ship = ship(0, 5f);
        ResonatorPayloadListener listener = new ResonatorPayloadListener(ship, rolling(0.04f));

        listener.modifyDamageDealt(missile(ship, WeaponType.MISSILE, 750f), ship(1, 0f), mock(DamageAPI.class), new Vector2f(), false);
        listener.advance(0f);
        when(ship.isAlive()).thenReturn(false);
        listener.advance(1f);
        when(ship.isAlive()).thenReturn(true);
        listener.advance(1f);

        verify(engine, times(1)).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
    }

    @Test
    void noSeriesOnAFailedRollANonMissileWeaponAProjectileOrAFriendlyHit() {
        ShipAPI ship = ship(0, 5f);
        ResonatorPayloadListener failing = new ResonatorPayloadListener(ship, rolling(0.06f));
        ResonatorPayloadListener passing = new ResonatorPayloadListener(ship, rolling(0f));

        failing.modifyDamageDealt(missile(ship, WeaponType.MISSILE, 750f), ship(1, 0f), mock(DamageAPI.class), new Vector2f(), false);
        passing.modifyDamageDealt(missile(ship, WeaponType.ENERGY, 750f), ship(1, 0f), mock(DamageAPI.class), new Vector2f(), false);
        passing.modifyDamageDealt(mock(DamagingProjectileAPI.class), ship(1, 0f), mock(DamageAPI.class), new Vector2f(), false);
        passing.modifyDamageDealt(missile(ship, WeaponType.MISSILE, 750f), ship(0, 0f), mock(DamageAPI.class), new Vector2f(), false);
        failing.advance(1f);
        passing.advance(1f);

        verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
    }

    @Test
    void missilesFromAnotherShipsWeaponNeverRoll() {
        ShipAPI ship = ship(0, 100f);
        ResonatorPayloadListener listener = new ResonatorPayloadListener(ship, rolling(0f));

        listener.modifyDamageDealt(missile(ship(0, 0f), WeaponType.MISSILE, 750f), ship(1, 0f), mock(DamageAPI.class), new Vector2f(), false);
        listener.advance(1f);

        verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
    }
}
