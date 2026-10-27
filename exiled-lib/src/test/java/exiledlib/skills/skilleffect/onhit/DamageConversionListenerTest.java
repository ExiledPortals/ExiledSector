package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DamageConversionListenerTest {

    private MockedStatic<Global> globalMock;
    private CombatListenerManagerAPI listeners;
    private DynamicStatsAPI dynamic;
    private ShipAPI ownerShip;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        listeners = mock(CombatListenerManagerAPI.class);
        when(engine.getListenerManager()).thenReturn(listeners);
        when(listeners.getListeners(any())).thenReturn(List.of());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
        ownerShip = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        dynamic = mock(DynamicStatsAPI.class);
        when(ownerShip.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private void owns(OnHitSkillEffect effect) {
        when(dynamic.getValue(effect.damageConversion().flagKey(), 0f)).thenReturn(1f);
    }

    private static WeaponSlotAPI mount(WeaponType type, WeaponSize size, boolean hardpoint) {
        WeaponSlotAPI mount = mock(WeaponSlotAPI.class);
        when(mount.getWeaponType()).thenReturn(type);
        when(mount.getSlotSize()).thenReturn(size);
        when(mount.isHardpoint()).thenReturn(hardpoint);
        when(mount.isTurret()).thenReturn(!hardpoint);
        return mount;
    }

    private static WeaponAPI weapon(WeaponSlotAPI mount, boolean beam, DamageType damageType) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getSlot()).thenReturn(mount);
        when(weapon.isBeam()).thenReturn(beam);
        when(weapon.getDamageType()).thenReturn(damageType);
        return weapon;
    }

    private static DamagingProjectileAPI shell(WeaponAPI weapon) {
        DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class);
        when(projectile.getWeapon()).thenReturn(weapon);
        return projectile;
    }

    private static BeamAPI beam(WeaponAPI weapon) {
        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getWeapon()).thenReturn(weapon);
        return beam;
    }

    private static DamageAPI damage(DamageType type, float base, float emp) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getType()).thenReturn(type);
        when(damage.getBaseDamage()).thenReturn(base);
        when(damage.getFluxComponent()).thenReturn(emp);
        return damage;
    }

    @Test
    void kineticShellsFromBallisticHardpointsDealHighExplosiveDamageWhileTurretShellsStayKinetic() {
        owns(OnHitSkillEffect.BALLISTIC_HARDPOINT_KINETIC_TO_HIGH_EXPLOSIVE);
        DamageAPI hardpointHit = damage(DamageType.KINETIC, 100f, 0f);
        DamageAPI turretHit = damage(DamageType.KINETIC, 100f, 0f);
        DamageConversionListener listener = new DamageConversionListener(ownerShip);
        DamagingProjectileAPI hardpointShell = shell(weapon(mount(WeaponType.BALLISTIC, WeaponSize.LARGE, true), false, DamageType.KINETIC));
        DamagingProjectileAPI turretShell = shell(weapon(mount(WeaponType.BALLISTIC, WeaponSize.LARGE, false), false, DamageType.KINETIC));

        assertNull(listener.modifyDamageDealt(hardpointShell, mock(ShipAPI.class), hardpointHit, null, false));
        listener.modifyDamageDealt(turretShell, mock(ShipAPI.class), turretHit, null, false);

        verify(hardpointHit).setType(DamageType.HIGH_EXPLOSIVE);
        verify(turretHit, never()).setType(any());
        verify(hardpointHit, never()).setDamage(anyFloat());
    }

    @Test
    void aBeamReadsItsTypeFromTheWeaponAndIsSetBackOnceItsMountNoLongerConverts() {
        owns(OnHitSkillEffect.BEAM_ENERGY_TO_HIGH_EXPLOSIVE);
        BeamAPI turretBeam = beam(weapon(mount(WeaponType.ENERGY, WeaponSize.MEDIUM, false), true, DamageType.ENERGY));
        DamageAPI beamDamage = damage(DamageType.HIGH_EXPLOSIVE, 300f, 0f);
        DamageConversionListener listener = new DamageConversionListener(ownerShip);

        listener.modifyDamageDealt(turretBeam, mock(ShipAPI.class), beamDamage, null, false);
        verify(beamDamage).setType(DamageType.HIGH_EXPLOSIVE);
        when(dynamic.getValue(OnHitSkillEffect.BEAM_ENERGY_TO_HIGH_EXPLOSIVE.damageConversion().flagKey(), 0f)).thenReturn(0f);
        listener.modifyDamageDealt(turretBeam, mock(ShipAPI.class), beamDamage, null, false);

        verify(beamDamage).setType(DamageType.ENERGY);
    }

    @Test
    void energyBeamsConvertInAnyMountWhileEnergyShotsStayEnergy() {
        owns(OnHitSkillEffect.BEAM_ENERGY_TO_HIGH_EXPLOSIVE);
        DamageAPI beamDamage = damage(DamageType.ENERGY, 300f, 0f);
        DamageAPI shotDamage = damage(DamageType.ENERGY, 100f, 0f);
        DamageConversionListener listener = new DamageConversionListener(ownerShip);
        BeamAPI hardpointBeam = beam(weapon(mount(WeaponType.UNIVERSAL, WeaponSize.LARGE, true), true, DamageType.ENERGY));
        DamagingProjectileAPI energyShot = shell(weapon(mount(WeaponType.ENERGY, WeaponSize.MEDIUM, false), false, DamageType.ENERGY));

        listener.modifyDamageDealt(hardpointBeam, mock(ShipAPI.class), beamDamage, null, false);
        listener.modifyDamageDealt(energyShot, mock(ShipAPI.class), shotDamage, null, false);

        verify(beamDamage).setType(DamageType.HIGH_EXPLOSIVE);
        verify(shotDamage, never()).setType(any());
    }

    @Test
    void weaponsInSmallEnergyMountsMoveAllTheirDamageIntoEmpUntilTheHitLands() {
        owns(OnHitSkillEffect.SMALL_ENERGY_MOUNT_DAMAGE_TO_EMP);
        ShipAPI target = mock(ShipAPI.class);
        DamageAPI hit = damage(DamageType.ENERGY, 100f, 10f);
        DamagingProjectileAPI smallEnergyShot = shell(weapon(mount(WeaponType.ENERGY, WeaponSize.SMALL, false), false, DamageType.ENERGY));

        new DamageConversionListener(ownerShip).modifyDamageDealt(smallEnergyShot, target, hit, null, false);
        verify(hit).setDamage(0f);
        verify(hit).setFluxComponent(110f);

        ArgumentCaptor<Object> restorer = ArgumentCaptor.forClass(Object.class);
        verify(listeners).addListener(restorer.capture());
        ((DamageListener) restorer.getValue()).reportDamageApplied(ownerShip, target, null);
        verify(hit).setDamage(100f);
        verify(hit).setFluxComponent(10f);
    }

    @Test
    void aDroneShotConvertsByTheMountOfTheWeaponItCopies() {
        owns(OnHitSkillEffect.BALLISTIC_HARDPOINT_KINETIC_TO_HIGH_EXPLOSIVE);
        WeaponSlotAPI sourceMount = mount(WeaponType.BALLISTIC, WeaponSize.MEDIUM, true);
        ShipAPI drone = mock(ShipAPI.class);
        when(drone.getCustomData()).thenReturn(Map.of(WeaponDroneFactory.SOURCE_MOUNT_KEY, sourceMount));
        WeaponAPI droneWeapon = weapon(mount(WeaponType.UNIVERSAL, WeaponSize.MEDIUM, false), false, DamageType.KINETIC);
        when(droneWeapon.getShip()).thenReturn(drone);
        DamageAPI hit = damage(DamageType.KINETIC, 100f, 0f);

        new DamageConversionListener(ownerShip).modifyDamageDealt(shell(droneWeapon), mock(ShipAPI.class), hit, null, false);

        verify(hit).setType(DamageType.HIGH_EXPLOSIVE);
    }

    @Test
    void hitsWithoutAWeaponAreLeftAlone() {
        owns(OnHitSkillEffect.SMALL_ENERGY_MOUNT_DAMAGE_TO_EMP);
        DamageAPI noWeapon = damage(DamageType.ENERGY, 100f, 0f);

        new DamageConversionListener(ownerShip).modifyDamageDealt(shell(null), mock(ShipAPI.class), noWeapon, null, false);

        verify(noWeapon, never()).setType(any());
        verify(noWeapon, never()).setDamage(anyFloat());
    }
}
