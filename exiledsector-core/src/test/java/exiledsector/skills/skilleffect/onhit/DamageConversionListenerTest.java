package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.CombatTestShips;
import exiledsector.skills.skilleffect.CombatTestShips.ShipBuilder;
import exiledsector.skills.skilleffect.EngineArcs;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import exiledsector.skills.skilleffect.HitMix;
import exiledsector.skills.skilleffect.TypeShares;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DamageConversionListenerTest {

    private static final float TOLERANCE = 1e-4f;

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();
    private final List<Object> registered = new ArrayList<>();
    private float elapsedTime;
    private DynamicStatsAPI dynamic;
    private ShipAPI ownerShip;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = combat.engine();
        CombatListenerManagerAPI listeners = mock(CombatListenerManagerAPI.class);
        when(engine.getListenerManager()).thenReturn(listeners);
        when(engine.getCustomData()).thenReturn(new HashMap<>());
        when(engine.getTotalElapsedTime(false)).thenAnswer(call -> elapsedTime);
        doAnswer(call -> registered.add(call.getArgument(0))).when(listeners).addListener(any());
        when(listeners.getListeners(any())).thenAnswer(call -> {
            Class<?> listenerType = call.getArgument(0);
            return registered.stream().filter(listenerType::isInstance).toList();
        });
        ShipBuilder owner = CombatTestShips.ship();
        ownerShip = owner.build();
        dynamic = owner.dynamicStats();
    }

    private void owns(OnHitSkillEffect effect) {
        when(dynamic.getValue(effect.damageConversion().flagKey(), 0f)).thenReturn(1f);
    }

    private void adaptsWithChance(float chancePercent) {
        when(dynamic.getValue(DamageConversionListener.ADAPTIVE_CHANCE_PERCENT_KEY, 0f)).thenReturn(chancePercent);
    }

    private void nextFrame() {
        elapsedTime += 0.016f;
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
        AtomicReference<DamageType> currentType = new AtomicReference<>(type);
        when(damage.getType()).thenAnswer(call -> currentType.get());
        doAnswer(call -> {
            currentType.set(call.getArgument(0));
            return null;
        }).when(damage).setType(any());
        when(damage.getBaseDamage()).thenReturn(base);
        when(damage.getFluxComponent()).thenReturn(emp);
        return damage;
    }

    private static Random rolling(float roll) {
        Random random = mock(Random.class);
        when(random.nextFloat()).thenReturn(roll);
        return random;
    }

    private DamageListener registeredRestorer() {
        return registered.stream().filter(HitDamageRestorer.class::isInstance).map(DamageListener.class::cast).findFirst().orElseThrow();
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
    void aBeamHittingSomethingOtherThanAShipIsRetypedAndGetsItsTypeBackOnceItsMountNoLongerConverts() {
        owns(OnHitSkillEffect.BEAM_ENERGY_TO_HIGH_EXPLOSIVE);
        BeamAPI turretBeam = beam(weapon(mount(WeaponType.ENERGY, WeaponSize.MEDIUM, false), true, DamageType.ENERGY));
        DamageAPI beamDamage = damage(DamageType.ENERGY, 300f, 0f);
        DamagingProjectileAPI missile = mock(DamagingProjectileAPI.class);
        DamageConversionListener listener = new DamageConversionListener(ownerShip);

        listener.modifyDamageDealt(turretBeam, missile, beamDamage, null, false);
        assertEquals(DamageType.HIGH_EXPLOSIVE, beamDamage.getType());

        when(dynamic.getValue(OnHitSkillEffect.BEAM_ENERGY_TO_HIGH_EXPLOSIVE.damageConversion().flagKey(), 0f)).thenReturn(0f);
        nextFrame();
        listener.modifyDamageDealt(turretBeam, missile, beamDamage, null, false);

        assertEquals(DamageType.ENERGY, beamDamage.getType());
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

        registeredRestorer().reportDamageApplied(ownerShip, target, null);
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
    void anAdaptedHitDealsKineticDamageToShieldsUntilItLands() {
        adaptsWithChance(10f);
        ShipAPI target = mock(ShipAPI.class);
        DamageAPI hit = damage(DamageType.ENERGY, 100f, 0f);
        DamagingProjectileAPI shot = shell(weapon(mount(WeaponType.ENERGY, WeaponSize.MEDIUM, false), false, DamageType.ENERGY));

        new DamageConversionListener(ownerShip, rolling(0.05f)).modifyDamageDealt(shot, target, hit, null, true);
        assertEquals(DamageType.KINETIC, hit.getType());

        registeredRestorer().reportDamageApplied(ownerShip, target, null);
        assertEquals(DamageType.ENERGY, hit.getType());
    }

    @Test
    void armorHitsAdaptToHighExplosiveAndAHitItsMountConvertsToHighExplosiveStaysWhollyHighExplosive() {
        owns(OnHitSkillEffect.BALLISTIC_HARDPOINT_KINETIC_TO_HIGH_EXPLOSIVE);
        adaptsWithChance(10f);
        DamageAPI hardpointHit = damage(DamageType.KINETIC, 100f, 0f);
        DamageAPI energyHit = damage(DamageType.ENERGY, 100f, 0f);
        DamagingProjectileAPI hardpointShell = shell(weapon(mount(WeaponType.BALLISTIC, WeaponSize.LARGE, true), false, DamageType.KINETIC));
        DamagingProjectileAPI energyShot = shell(weapon(mount(WeaponType.ENERGY, WeaponSize.MEDIUM, false), false, DamageType.ENERGY));
        DamageConversionListener listener = new DamageConversionListener(ownerShip, rolling(0.05f));

        listener.modifyDamageDealt(hardpointShell, mock(ShipAPI.class), hardpointHit, null, false);
        listener.modifyDamageDealt(energyShot, mock(ShipAPI.class), energyHit, null, false);

        verify(hardpointHit, times(1)).setType(DamageType.HIGH_EXPLOSIVE);
        verify(hardpointHit, never()).setDamage(anyFloat());
        verify(energyHit).setType(DamageType.HIGH_EXPLOSIVE);
    }

    @Test
    void adaptiveLeavesAKineticShieldHitAloneEvenWhenItsMountConvertsItToHighExplosive() {
        owns(OnHitSkillEffect.BALLISTIC_HARDPOINT_KINETIC_TO_HIGH_EXPLOSIVE);
        adaptsWithChance(100f);
        Random random = rolling(0f);
        DamageConversionListener listener = new DamageConversionListener(ownerShip, random);
        DamagingProjectileAPI hardpointShell = shell(weapon(mount(WeaponType.BALLISTIC, WeaponSize.LARGE, true), false, DamageType.KINETIC));

        HitMix.Hit hit = HitMix.resolve(ownerShip, listener, hardpointShell, mock(ShipAPI.class), damage(DamageType.KINETIC, 100f, 0f), null, true);

        assertEquals(TypeShares.single(DamageType.HIGH_EXPLOSIVE), hit.shares());
        verify(random, never()).nextFloat();
    }

    @Test
    void anAdaptedTypeSharesTheWholeHitWithAPartialConversion() {
        adaptsWithChance(100f);
        HitMix.Resolver partialConversion = hit -> hit.convert(DamageType.ENERGY, DamageType.HIGH_EXPLOSIVE, 0.4f);
        when(ownerShip.hasListenerOfClass(HitMix.Resolver.class)).thenReturn(true);
        when(ownerShip.getListeners(HitMix.Resolver.class)).thenReturn(List.of(partialConversion));
        DamageConversionListener listener = new DamageConversionListener(ownerShip, rolling(0f));
        DamagingProjectileAPI energyShot = shell(weapon(mount(WeaponType.ENERGY, WeaponSize.MEDIUM, false), false, DamageType.ENERGY));
        DamageAPI damage = damage(DamageType.ENERGY, 100f, 0f);

        HitMix.Hit hit = HitMix.resolve(ownerShip, listener, energyShot, mock(ShipAPI.class), damage, null, true);

        assertEquals(1f / 1.4f, hit.shares().share(DamageType.KINETIC), TOLERANCE);
        assertEquals(0.4f / 1.4f, hit.shares().share(DamageType.HIGH_EXPLOSIVE), TOLERANCE);
        assertEquals(0f, hit.shares().share(DamageType.ENERGY), TOLERANCE);
        verify(damage, never()).setType(any());
    }

    @Test
    void aHitMovedIntoEmpByItsMountSharesItsDamageWithAnAdaptedType() {
        owns(OnHitSkillEffect.SMALL_ENERGY_MOUNT_DAMAGE_TO_EMP);
        adaptsWithChance(100f);
        DamageAPI hit = damage(DamageType.ENERGY, 100f, 0f);
        DamagingProjectileAPI smallEnergyShot = shell(weapon(mount(WeaponType.ENERGY, WeaponSize.SMALL, false), false, DamageType.ENERGY));

        new DamageConversionListener(ownerShip, rolling(0f)).modifyDamageDealt(smallEnergyShot, mock(ShipAPI.class), hit, null, true);

        verify(hit).setDamage(50f);
        verify(hit).setFluxComponent(50f);
        verify(hit).setType(DamageType.KINETIC);
    }

    @Test
    void aBeamIsAdaptedOnlyForTheTickThatRolledAndGetsItsTypeBackOnceThatTickLands() {
        adaptsWithChance(10f);
        ShipAPI target = mock(ShipAPI.class);
        DamageAPI beamDamage = damage(DamageType.ENERGY, 300f, 0f);
        BeamAPI beam = beam(weapon(mount(WeaponType.ENERGY, WeaponSize.MEDIUM, false), true, DamageType.ENERGY));
        Random random = mock(Random.class);
        when(random.nextFloat()).thenReturn(0.05f, 0.5f);
        DamageConversionListener listener = new DamageConversionListener(ownerShip, random);

        listener.modifyDamageDealt(beam, target, beamDamage, null, true);
        assertEquals(DamageType.KINETIC, beamDamage.getType());
        registeredRestorer().reportDamageApplied(ownerShip, target, null);
        assertEquals(DamageType.ENERGY, beamDamage.getType());

        nextFrame();
        listener.modifyDamageDealt(beam, target, beamDamage, null, true);
        assertEquals(DamageType.ENERGY, beamDamage.getType());
        verify(beamDamage, times(1)).setType(DamageType.KINETIC);
    }

    @Test
    void aFailedRollOrAHitOnSomethingOtherThanAShipKeepsItsType() {
        adaptsWithChance(10f);
        DamageAPI missedRoll = damage(DamageType.ENERGY, 100f, 0f);
        DamageAPI missileHit = damage(DamageType.ENERGY, 100f, 0f);
        DamagingProjectileAPI shot = shell(weapon(mount(WeaponType.ENERGY, WeaponSize.MEDIUM, false), false, DamageType.ENERGY));

        new DamageConversionListener(ownerShip, rolling(0.2f)).modifyDamageDealt(shot, mock(ShipAPI.class), missedRoll, null, true);
        new DamageConversionListener(ownerShip, rolling(0f)).modifyDamageDealt(shot, mock(DamagingProjectileAPI.class), missileHit, null, false);

        verify(missedRoll, never()).setType(any());
        verify(missileHit, never()).setType(any());
    }

    @Test
    void hitsWithoutAWeaponAreLeftAlone() {
        owns(OnHitSkillEffect.SMALL_ENERGY_MOUNT_DAMAGE_TO_EMP);
        DamageAPI noWeapon = damage(DamageType.ENERGY, 100f, 0f);

        new DamageConversionListener(ownerShip).modifyDamageDealt(shell(null), mock(ShipAPI.class), noWeapon, null, false);

        verify(noWeapon, never()).setType(any());
        verify(noWeapon, never()).setDamage(anyFloat());
    }

    @Test
    void anArcFromAScriptedWeaponAndTheArcItChainsIntoConvertByThatWeaponsMount() {
        owns(OnHitSkillEffect.SMALL_ENERGY_MOUNT_DAMAGE_TO_EMP);
        WeaponSlotAPI smallEnergyMount = mount(WeaponType.ENERGY, WeaponSize.SMALL, false);
        WeaponAPI shockRepeater = EngineArcs.scriptedWeapon(smallEnergyMount, new Vector2f(30f, -12f), new Vector2f(8f, 0f));
        when(ownerShip.getOwner()).thenReturn(0);
        when(ownerShip.getAllWeapons()).thenReturn(List.of(shockRepeater));
        ShipAPI firstTarget = mock(ShipAPI.class);
        when(firstTarget.getLocation()).thenReturn(new Vector2f(400f, 0f));
        when(firstTarget.getOwner()).thenReturn(1);
        DamageAPI firstHit = damage(DamageType.ENERGY, 100f, 50f);
        DamageAPI chainedHit = damage(DamageType.ENERGY, 100f, 50f);
        DamageConversionListener listener = new DamageConversionListener(ownerShip);

        listener.modifyDamageDealt(EngineArcs.startingAt(ownerShip, new Vector2f(38f, -12f)), firstTarget, firstHit, null, false);
        listener.modifyDamageDealt(EngineArcs.startingAt(firstTarget, new Vector2f()), mock(ShipAPI.class), chainedHit, null, false);

        verify(firstHit).setDamage(0f);
        verify(firstHit).setFluxComponent(150f);
        verify(chainedHit).setDamage(0f);
        verify(chainedHit).setFluxComponent(150f);
    }

    @Test
    void anArcThatStartsAwayFromEveryScriptedWeaponKeepsItsDamage() {
        owns(OnHitSkillEffect.SMALL_ENERGY_MOUNT_DAMAGE_TO_EMP);
        WeaponAPI shockRepeater = EngineArcs.scriptedWeapon(mount(WeaponType.ENERGY, WeaponSize.SMALL, false), new Vector2f(30f, -12f),
                new Vector2f(8f, 0f));
        when(ownerShip.getAllWeapons()).thenReturn(List.of(shockRepeater));
        DamageAPI systemArc = damage(DamageType.ENERGY, 100f, 50f);

        new DamageConversionListener(ownerShip).modifyDamageDealt(EngineArcs.startingAt(ownerShip, new Vector2f()), mock(ShipAPI.class), systemArc,
                null, false);

        verify(systemArc, never()).setDamage(anyFloat());
        verify(systemArc, never()).setType(any());
    }
}
