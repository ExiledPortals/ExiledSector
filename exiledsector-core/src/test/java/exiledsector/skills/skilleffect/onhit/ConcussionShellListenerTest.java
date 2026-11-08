package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConcussionShellListenerTest {

    private ShipAPI ship;
    private DynamicStatsAPI dynamic;
    private MutableStat ballisticRateOfFire;
    private ShipAPI enemy;
    private WeaponAPI nearWeapon;
    private WeaponAPI farWeapon;
    private ShipEngineAPI engine;

    @BeforeEach
    void setUp() {
        ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(0);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        ballisticRateOfFire = new MutableStat(1f);
        when(stats.getBallisticRoFMult()).thenReturn(ballisticRateOfFire);
        when(dynamic.getValue(ConcussionShellListener.CHANCE_PERCENT_KEY, 0f)).thenReturn(10f);

        enemy = mock(ShipAPI.class);
        when(enemy.getOwner()).thenReturn(1);
        when(enemy.isAlive()).thenReturn(true);
        nearWeapon = weaponAt(new Vector2f(10f, 0f));
        farWeapon = weaponAt(new Vector2f(200f, 0f));
        when(enemy.getAllWeapons()).thenReturn(List.of(farWeapon, nearWeapon));
        engine = mock(ShipEngineAPI.class);
        when(engine.getLocation()).thenReturn(new Vector2f(-100f, 0f));
        ShipEngineControllerAPI engineController = mock(ShipEngineControllerAPI.class);
        when(engineController.getShipEngines()).thenReturn(List.of(engine));
        when(enemy.getEngineController()).thenReturn(engineController);
    }

    private static WeaponAPI weaponAt(Vector2f location) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getLocation()).thenReturn(location);
        return weapon;
    }

    private static DamagingProjectileAPI shotFrom(WeaponSize size, WeaponType type) {
        return shotFrom(size, type, 1f);
    }

    private static DamagingProjectileAPI shotFrom(WeaponSize size, WeaponType type, float shotsPerSecond) {
        WeaponAPI.DerivedWeaponStatsAPI derivedStats = mock(WeaponAPI.DerivedWeaponStatsAPI.class);
        when(derivedStats.getRoF()).thenReturn(shotsPerSecond);
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getDerivedStats()).thenReturn(derivedStats);
        when(weapon.getSize()).thenReturn(size);
        when(weapon.getType()).thenReturn(type);
        DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class);
        when(projectile.getWeapon()).thenReturn(weapon);
        return projectile;
    }

    private void hit(double roll, DamagingProjectileAPI projectile, Vector2f point, boolean shieldHit) {
        new ConcussionShellListener(ship, () -> roll).modifyDamageDealt(projectile, enemy, mock(DamageAPI.class), point, shieldHit);
    }

    @Test
    void aSuccessfulRollDisablesTheWeaponNearestTheImpact() {
        hit(0.05, shotFrom(WeaponSize.LARGE, WeaponType.BALLISTIC), new Vector2f(), false);

        verify(nearWeapon).disable();
        verify(farWeapon, never()).disable();
        verify(engine, never()).disable();
    }

    @Test
    void anEngineNearerThanAnyWeaponIsDisabledInstead() {
        hit(0.05, shotFrom(WeaponSize.LARGE, WeaponType.BALLISTIC), new Vector2f(-90f, 0f), false);

        verify(engine).disable();
        verify(nearWeapon, never()).disable();
    }

    @Test
    void alreadyDisabledPartsAreSkipped() {
        when(nearWeapon.isDisabled()).thenReturn(true);

        hit(0.05, shotFrom(WeaponSize.LARGE, WeaponType.BALLISTIC), new Vector2f(), false);

        verify(nearWeapon, never()).disable();
        verify(engine).disable();
    }

    @Test
    void failedRollsShieldHitsAndOtherWeaponsDisableNothing() {
        hit(0.5, shotFrom(WeaponSize.LARGE, WeaponType.BALLISTIC), new Vector2f(), false);
        hit(0.05, shotFrom(WeaponSize.LARGE, WeaponType.BALLISTIC), new Vector2f(), true);
        hit(0.05, shotFrom(WeaponSize.MEDIUM, WeaponType.BALLISTIC), new Vector2f(), false);
        hit(0.05, shotFrom(WeaponSize.LARGE, WeaponType.ENERGY), new Vector2f(), false);

        verify(nearWeapon, never()).disable();
        verify(engine, never()).disable();
    }

    @Test
    void mediumBallisticHitsRollTheirOwnChance() {
        when(dynamic.getValue(ConcussionShellListener.MEDIUM_CHANCE_PERCENT_KEY, 0f)).thenReturn(10f);

        hit(0.5, shotFrom(WeaponSize.MEDIUM, WeaponType.BALLISTIC), new Vector2f(), false);
        hit(0.05, shotFrom(WeaponSize.SMALL, WeaponType.BALLISTIC), new Vector2f(), false);
        verify(nearWeapon, never()).disable();

        hit(0.05, shotFrom(WeaponSize.MEDIUM, WeaponType.BALLISTIC), new Vector2f(), false);
        verify(nearWeapon).disable();
    }

    @Test
    void theChanceScalesWithTheSecondsBetweenShots() {
        hit(0.05, shotFrom(WeaponSize.LARGE, WeaponType.BALLISTIC, 4f), new Vector2f(), false);
        verify(nearWeapon, never()).disable();

        hit(0.29, shotFrom(WeaponSize.LARGE, WeaponType.BALLISTIC, 1f / 3f), new Vector2f(), false);
        verify(nearWeapon).disable();
    }

    @Test
    void rateOfFireBonusesShortenTheSecondsBetweenShots() {
        ballisticRateOfFire.modifyPercent("test", 100f);

        hit(0.07, shotFrom(WeaponSize.LARGE, WeaponType.BALLISTIC), new Vector2f(), false);
        verify(nearWeapon, never()).disable();

        hit(0.04, shotFrom(WeaponSize.LARGE, WeaponType.BALLISTIC), new Vector2f(), false);
        verify(nearWeapon).disable();
    }

    @Test
    void alliedShipsAreNeverShaken() {
        when(enemy.getOwner()).thenReturn(0);

        hit(0.05, shotFrom(WeaponSize.LARGE, WeaponType.BALLISTIC), new Vector2f(), false);

        verify(nearWeapon, never()).disable();
    }
}
