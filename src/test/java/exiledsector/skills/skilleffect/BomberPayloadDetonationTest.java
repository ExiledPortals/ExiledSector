package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.WingRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BomberPayloadDetonationTest {

    private static final float FRAME = 0.1f;

    private MockedStatic<Global> global;
    private CombatEngineAPI engine;
    private ShipAPI carrier;

    private static final class AlwaysRolls extends Random {
        @Override
        public float nextFloat() {
            return 0f;
        }
    }

    private static final class NeverRolls extends Random {
        @Override
        public float nextFloat() {
            return 0.999f;
        }
    }

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getCombatEngine).thenReturn(engine);
        carrier = ship(new Vector2f(0f, 0f));
        when(carrier.getCollisionRadius()).thenReturn(200f);
    }

    @AfterEach
    void tearDown() {
        global.close();
    }

    private static ShipAPI ship(Vector2f location) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getLocation()).thenReturn(location);
        when(ship.getVelocity()).thenReturn(new Vector2f());
        when(ship.getHitpoints()).thenReturn(100f);
        when(ship.getMaxHitpoints()).thenReturn(100f);
        return ship;
    }

    private static WeaponAPI weapon(WeaponAPI.WeaponType type, boolean usesAmmo, int ammo) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getType()).thenReturn(type);
        when(weapon.usesAmmo()).thenReturn(usesAmmo);
        when(weapon.getAmmo()).thenReturn(ammo);
        when(weapon.getId()).thenReturn(type.name().toLowerCase() + "_" + ammo);
        return weapon;
    }

    private ShipAPI bomber(float distanceFromCarrier, WeaponAPI... weapons) {
        ShipAPI bomber = ship(new Vector2f(distanceFromCarrier, 0f));
        FighterWingAPI wing = mock(FighterWingAPI.class);
        when(wing.getRole()).thenReturn(WingRole.BOMBER);
        when(bomber.getWing()).thenReturn(wing);
        when(bomber.getAllWeapons()).thenReturn(List.of(weapons));
        return bomber;
    }

    private static void fly(BomberPayloadDetonation detonation, float seconds) {
        for (float elapsed = 0f; elapsed < seconds - 1e-4f; elapsed += FRAME) {
            detonation.advance(FRAME);
        }
    }

    private void verifyNoDetonation(ShipAPI bomber) {
        verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
        verify(engine, never()).applyDamage(eq(bomber), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void onlyBombersAreArmed() {
        ShipAPI fighter = ship(new Vector2f(2000f, 0f));
        FighterWingAPI wing = mock(FighterWingAPI.class);
        when(wing.getRole()).thenReturn(WingRole.FIGHTER);
        when(fighter.getWing()).thenReturn(wing);
        BomberPayloadDetonation.arm(fighter, carrier, 5f);
        verify(fighter, never()).addListener(any());

        ShipAPI bomber = bomber(2000f, weapon(WeaponAPI.WeaponType.MISSILE, true, 1));
        BomberPayloadDetonation.arm(bomber, carrier, 5f);
        verify(bomber, times(1)).addListener(any(BomberPayloadDetonation.class));
    }

    @Test
    void theDiceAreOnlyRolledEveryThreeSecondsAndOnlyFailingRollsSpareTheBomber() {
        ShipAPI bomber = bomber(2000f, weapon(WeaponAPI.WeaponType.MISSILE, true, 1));
        fly(new BomberPayloadDetonation(bomber, carrier, 5f, new NeverRolls()), 30f);
        verifyNoDetonation(bomber);

        BomberPayloadDetonation unlucky = new BomberPayloadDetonation(bomber, carrier, 5f, new AlwaysRolls());
        fly(unlucky, BomberPayloadDetonation.CHECK_INTERVAL_SECONDS - 0.2f);
        verifyNoDetonation(bomber);
        fly(unlucky, 0.3f);
        verify(engine).applyDamage(eq(bomber), any(), anyFloat(), eq(DamageType.HIGH_EXPLOSIVE), eq(0f), eq(true), eq(false), eq(bomber));
    }

    @Test
    void bombersCloseToTheirCarrierNeverDetonate() {
        float justInside = 200f + BomberPayloadDetonation.SAFE_DISTANCE_FROM_CARRIER - 10f;
        ShipAPI bomber = bomber(justInside, weapon(WeaponAPI.WeaponType.MISSILE, true, 1));
        fly(new BomberPayloadDetonation(bomber, carrier, 100f, new AlwaysRolls()), 30f);
        verifyNoDetonation(bomber);
    }

    @Test
    void landingBombersAndBombersThatDroppedTheirPayloadNeverDetonate() {
        ShipAPI landing = bomber(2000f, weapon(WeaponAPI.WeaponType.MISSILE, true, 1));
        when(landing.isLanding()).thenReturn(true);
        fly(new BomberPayloadDetonation(landing, carrier, 100f, new AlwaysRolls()), 10f);
        verifyNoDetonation(landing);

        ShipAPI spent = bomber(2000f, weapon(WeaponAPI.WeaponType.MISSILE, true, 0));
        fly(new BomberPayloadDetonation(spent, carrier, 100f, new AlwaysRolls()), 10f);
        verifyNoDetonation(spent);
    }

    @Test
    void theLoadedMissileIsThePayloadAndGunsWithoutAmmoNeverCount() {
        WeaponAPI gun = weapon(WeaponAPI.WeaponType.BALLISTIC, false, 0);
        WeaponAPI spentTorpedo = weapon(WeaponAPI.WeaponType.MISSILE, true, 0);
        WeaponAPI rockets = weapon(WeaponAPI.WeaponType.BALLISTIC, true, 4);
        WeaponAPI bomb = weapon(WeaponAPI.WeaponType.MISSILE, true, 2);

        assertSame(bomb, BomberPayloadDetonation.payload(bomber(2000f, gun, rockets, spentTorpedo, bomb)));
        assertSame(rockets, BomberPayloadDetonation.payload(bomber(2000f, gun, rockets, spentTorpedo)));
        assertNull(BomberPayloadDetonation.payload(bomber(2000f, gun, spentTorpedo)));
    }

    @Test
    void theFallbackBlastHitsEveryShipNearbyLikeAVanillaTorpedo() {
        DamagingExplosionSpec blast = BomberPayloadDetonation.blast(800f, DamageType.FRAGMENTATION, 100f, null);

        assertEquals(CollisionClass.HITS_SHIPS_AND_ASTEROIDS, blast.getCollisionClass());
        assertEquals(CollisionClass.HITS_SHIPS_AND_ASTEROIDS, blast.getCollisionClassIfByFighter());
        assertEquals(DamageType.FRAGMENTATION, blast.getDamageType());
    }

    @Test
    void aPayloadWithoutItsOwnExplosionBlowsUpWithTheShotsOwnDamage() {
        WeaponAPI torpedo = weapon(WeaponAPI.WeaponType.MISSILE, true, 1);
        ShipAPI bomber = bomber(2000f, torpedo);
        MissileAPI shot = mock(MissileAPI.class);
        MissileSpecAPI spec = mock(MissileSpecAPI.class);
        when(shot.getSpec()).thenReturn(spec);
        when(spec.getExplosionRadius()).thenReturn(120f);
        when(shot.getDamageAmount()).thenReturn(1500f);
        when(shot.getDamageType()).thenReturn(DamageType.HIGH_EXPLOSIVE);
        when(engine.spawnProjectile(eq(bomber), eq(torpedo), anyString(), any(), anyFloat(), any())).thenReturn(shot);

        fly(new BomberPayloadDetonation(bomber, carrier, 100f, new AlwaysRolls()), BomberPayloadDetonation.CHECK_INTERVAL_SECONDS + FRAME);

        ArgumentCaptor<DamagingExplosionSpec> blast = ArgumentCaptor.forClass(DamagingExplosionSpec.class);
        verify(engine).spawnDamagingExplosion(blast.capture(), eq(bomber), any());
        assertEquals(1500f, blast.getValue().getMaxDamage());
        assertEquals(120f, blast.getValue().getRadius());
        verify(shot, never()).explode();
        verify(engine).removeEntity(shot);
    }

    @Test
    void aPayloadWithItsOwnExplosionUsesIt() {
        WeaponAPI mine = weapon(WeaponAPI.WeaponType.MISSILE, true, 1);
        ShipAPI bomber = bomber(2000f, mine);
        MissileAPI shot = mock(MissileAPI.class);
        MissileSpecAPI spec = mock(MissileSpecAPI.class);
        when(shot.getSpec()).thenReturn(spec);
        when(spec.getExplosionSpec()).thenReturn(BomberPayloadDetonation.blast(500f, DamageType.ENERGY, 150f, null));
        when(engine.spawnProjectile(eq(bomber), eq(mine), anyString(), any(), anyFloat(), any())).thenReturn(shot);

        fly(new BomberPayloadDetonation(bomber, carrier, 100f, new AlwaysRolls()), BomberPayloadDetonation.CHECK_INTERVAL_SECONDS + FRAME);

        verify(shot).explode();
        verify(engine, never()).spawnDamagingExplosion(any(), any(), any());
    }

    @Test
    void aBomberOnlyEverDetonatesOnce() {
        ShipAPI bomber = bomber(2000f, weapon(WeaponAPI.WeaponType.MISSILE, true, 1));
        when(engine.spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any())).thenReturn(mock(DamagingProjectileAPI.class));

        fly(new BomberPayloadDetonation(bomber, carrier, 100f, new AlwaysRolls()), 30f);

        verify(engine, times(1)).applyDamage(eq(bomber), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
    }
}
