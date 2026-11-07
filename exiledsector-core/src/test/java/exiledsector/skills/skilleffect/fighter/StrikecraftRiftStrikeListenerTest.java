package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StrikecraftRiftStrikeListenerTest {

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

    private static ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    private static DamagingProjectileAPI shot(WeaponType type, boolean beam, float damagePerShot) {
        DerivedWeaponStatsAPI derived = mock(DerivedWeaponStatsAPI.class);
        when(derived.getDamagePerShot()).thenReturn(damagePerShot);
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        when(spec.getDerivedStats()).thenReturn(derived);
        WeaponAPI weapon = mock(WeaponAPI.class);
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

    @Test
    void aSuccessfulRollDropsARiftSourcedByTheStrikeCraftWithTheWeaponsRiftDamage() {
        ShipAPI fighter = ship(0);
        ShipAPI carrier = FighterCarriers.with(StrikecraftRiftStrikeListener.CHANCE_PERCENT_KEY, 10f);
        Vector2f point = new Vector2f(15f, 25f);

        assertNull(new StrikecraftRiftStrikeListener(fighter, carrier, rolling(0.09f))
                .modifyDamageDealt(shot(WeaponType.ENERGY, false, 200f), ship(1), mock(DamageAPI.class), point, false));

        verify(engine).spawnProjectile(eq(fighter), eq(null), eq(RiftExplosions.RIFT_MINE_WEAPON_ID), eq(point), anyFloat(), eq(null));
        verify(rift.getDamage()).setDamage(100f);
        verify(rift).addDamagedAlready(fighter);
    }

    @Test
    void noRiftOnAFailedRollABeamANonEnergyWeaponOrAFriendlyHit() {
        ShipAPI fighter = ship(0);
        ShipAPI carrier = FighterCarriers.with(StrikecraftRiftStrikeListener.CHANCE_PERCENT_KEY, 10f);

        new StrikecraftRiftStrikeListener(fighter, carrier, rolling(0.11f))
                .modifyDamageDealt(shot(WeaponType.ENERGY, false, 200f), ship(1), mock(DamageAPI.class), new Vector2f(), false);
        new StrikecraftRiftStrikeListener(fighter, carrier, rolling(0f))
                .modifyDamageDealt(shot(WeaponType.ENERGY, true, 200f), ship(1), mock(DamageAPI.class), new Vector2f(), false);
        new StrikecraftRiftStrikeListener(fighter, carrier, rolling(0f))
                .modifyDamageDealt(shot(WeaponType.MISSILE, false, 200f), ship(1), mock(DamageAPI.class), new Vector2f(), false);
        new StrikecraftRiftStrikeListener(fighter, carrier, rolling(0f))
                .modifyDamageDealt(shot(WeaponType.ENERGY, false, 200f), ship(0), mock(DamageAPI.class), new Vector2f(), false);

        verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
    }
}
