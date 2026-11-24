package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.HitMix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IonCascadeListenerTest {

    private static final float TOLERANCE = 1e-4f;

    @RegisterExtension
    final CombatEngineExtension combat = CombatEngineExtension.withListenerManager();

    private static MutableStat stat(float value) {
        MutableStat stat = mock(MutableStat.class);
        when(stat.getModifiedValue()).thenReturn(value);
        return stat;
    }

    private static MutableShipStatsAPI stats(float empTaken, float hullTaken) {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, Mockito.RETURNS_DEEP_STUBS);
        MutableStat neutral = stat(1f);
        MutableStat emp = stat(empTaken);
        MutableStat hull = stat(hullTaken);
        when(stats.getEmpDamageTakenMult()).thenReturn(emp);
        when(stats.getHullDamageTakenMult()).thenReturn(hull);
        when(stats.getWeaponDamageTakenMult()).thenReturn(neutral);
        when(stats.getEngineDamageTakenMult()).thenReturn(neutral);
        when(stats.getDamageToTargetWeaponsMult()).thenReturn(neutral);
        when(stats.getDamageToTargetEnginesMult()).thenReturn(neutral);
        return stats;
    }

    private static WeaponAPI weaponAt(Vector2f location, boolean disabled) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getLocation()).thenReturn(location);
        when(weapon.isDisabled()).thenReturn(disabled);
        return weapon;
    }

    private static ShipEngineControllerAPI.ShipEngineAPI engineAt(Vector2f location, boolean disabled) {
        ShipEngineControllerAPI.ShipEngineAPI shipEngine = mock(ShipEngineControllerAPI.ShipEngineAPI.class);
        when(shipEngine.getLocation()).thenReturn(location);
        when(shipEngine.isDisabled()).thenReturn(disabled);
        return shipEngine;
    }

    private static ShipAPI target(List<WeaponAPI> weapons, List<ShipEngineControllerAPI.ShipEngineAPI> engines) {
        return target(1f, weapons, engines);
    }

    private static ShipAPI target(float empTaken, List<WeaponAPI> weapons, List<ShipEngineControllerAPI.ShipEngineAPI> engines) {
        ShipAPI target = mock(ShipAPI.class);
        when(target.getOwner()).thenReturn(1);
        when(target.isAlive()).thenReturn(true);
        when(target.getLocation()).thenReturn(new Vector2f());
        MutableShipStatsAPI targetStats = stats(empTaken, 1f);
        when(target.getMutableStats()).thenReturn(targetStats);
        ArmorGridAPI grid = mock(ArmorGridAPI.class);
        when(grid.getCellAtLocation(Mockito.any())).thenAnswer(call -> {
            Vector2f location = call.getArgument(0);
            return new int[]{Math.round(location.x), Math.round(location.y)};
        });
        when(target.getArmorGrid()).thenReturn(grid);
        when(target.getAllWeapons()).thenReturn(weapons);
        ShipEngineControllerAPI controller = mock(ShipEngineControllerAPI.class);
        when(controller.getShipEngines()).thenReturn(engines);
        when(target.getEngineController()).thenReturn(controller);
        return target;
    }

    private static ShipAPI disabledWeaponUnderTheHit(float empTaken) {
        return target(empTaken, List.of(weaponAt(new Vector2f(0f, 0f), true)), List.of());
    }

    private static ShipAPI owner(float sharePercent) {
        return owner(sharePercent, 1f);
    }

    private static ShipAPI owner(float sharePercent, float damageToWeaponsMult) {
        ShipAPI owner = mock(ShipAPI.class, Mockito.RETURNS_DEEP_STUBS);
        when(owner.getOwner()).thenReturn(0);
        when(owner.getMutableStats().getDynamic().getValue(IonCascadeListener.HIGH_EXPLOSIVE_SHARE_PERCENT_KEY, 0f)).thenReturn(sharePercent);
        when(owner.getMutableStats().getDamageToTargetWeaponsMult().getModifiedValue()).thenReturn(damageToWeaponsMult);
        when(owner.getMutableStats().getDamageToTargetEnginesMult().getModifiedValue()).thenReturn(1f);
        return owner;
    }

    private static HitMix.EmpOnlyResult empOnlyHit(ShipAPI owner, ShipAPI target, boolean shieldHit, HitMix.Resolver... otherResolvers) {
        List<HitMix.Resolver> resolvers = new ArrayList<>();
        resolvers.add(new IonCascadeListener(owner));
        resolvers.addAll(List.of(otherResolvers));
        when(owner.hasListenerOfClass(HitMix.Resolver.class)).thenReturn(true);
        when(owner.getListeners(HitMix.Resolver.class)).thenReturn(resolvers);
        return HitMix.resolveEmpOnly(owner, null, target, new Vector2f(0.2f, 0.1f), shieldHit, 200f);
    }

    @Test
    void theEmpShareFollowsVanillasTwoRingsWithoutCorners() {
        assertEquals(IonCascadeListener.INNER_CELL_EMP_SHARE, IonCascadeListener.empShareAtCellOffset(0, 0));
        assertEquals(IonCascadeListener.INNER_CELL_EMP_SHARE, IonCascadeListener.empShareAtCellOffset(1, -1));
        assertEquals(IonCascadeListener.OUTER_CELL_EMP_SHARE, IonCascadeListener.empShareAtCellOffset(-2, 1));
        assertEquals(0f, IonCascadeListener.empShareAtCellOffset(2, 2));
        assertEquals(0f, IonCascadeListener.empShareAtCellOffset(3, 0));
    }

    @Test
    void onlyTheClosestDisabledPartInReachCounts() {
        ShipAPI target = target(
                List.of(weaponAt(new Vector2f(0f, 0f), true), weaponAt(new Vector2f(1f, 0f), false), weaponAt(new Vector2f(1f, 1f), true),
                        weaponAt(new Vector2f(5f, 0f), true)),
                List.of(engineAt(new Vector2f(0f, 2f), true)));

        float convertible = IonCascadeListener.empOnClosestDisabledPart(target, new Vector2f(), 100f, null);

        assertEquals(100f * IonCascadeListener.INNER_CELL_EMP_SHARE, convertible, TOLERANCE);
    }

    @Test
    void anOuterRingPartAloneGivesTheOuterShare() {
        ShipAPI target = target(List.of(weaponAt(new Vector2f(5f, 0f), true)), List.of(engineAt(new Vector2f(0f, 2f), true)));

        float convertible = IonCascadeListener.empOnClosestDisabledPart(target, new Vector2f(), 100f, null);

        assertEquals(100f * IonCascadeListener.OUTER_CELL_EMP_SHARE, convertible, TOLERANCE);
    }

    @Test
    void noDisabledPartInReachConvertsNothing() {
        ShipAPI target = target(List.of(weaponAt(new Vector2f(0f, 0f), false), weaponAt(new Vector2f(3f, 0f), true)), List.of());

        assertEquals(0f, IonCascadeListener.empOnClosestDisabledPart(target, new Vector2f(), 100f, null));
    }

    @Test
    void theShareOfEmpOnTheClosestDisabledPartIsConvertedToHighExplosiveDamageInTheSameHit() {
        ShipAPI target = disabledWeaponUnderTheHit(1f);
        ShipAPI owner = owner(50f);
        IonCascadeListener listener = new IonCascadeListener(owner);
        DamageAPI damage = IonSpillListenerTest.hit(DamageType.KINETIC, 100f, 200f);

        HitMix.Hit hit = HitMix.resolve(owner, listener, null, target, damage, new Vector2f(0.2f, 0.1f), false);

        float moved = 200f * IonCascadeListener.INNER_CELL_EMP_SHARE * 0.5f;
        assertEquals(100f + moved, damage.getBaseDamage(), TOLERANCE);
        assertEquals(200f - moved, damage.getFluxComponent(), TOLERANCE);
        assertEquals(moved / (100f + moved), hit.shares().share(DamageType.HIGH_EXPLOSIVE), TOLERANCE);
    }

    @Test
    void theShareIsTheEmpReachingThePartOverTheHitsEmpTimesTheSharePercent() {
        HitMix.EmpOnlyResult hit = empOnlyHit(owner(50f), disabledWeaponUnderTheHit(0.5f), false);

        float reachingThePart = 200f * 0.5f * IonCascadeListener.INNER_CELL_EMP_SHARE;
        float moved = 200f * (reachingThePart / 200f) * 0.5f;
        assertEquals(moved, hit.movedDamage().get(DamageType.HIGH_EXPLOSIVE), TOLERANCE);
        assertEquals(200f - moved, hit.emp(), TOLERANCE);
    }

    @Test
    void theDealersDamageToWeaponsRaisesTheShare() {
        HitMix.EmpOnlyResult hit = empOnlyHit(owner(50f, 1.6f), disabledWeaponUnderTheHit(1f), false);

        float moved = 200f * 1.6f * IonCascadeListener.INNER_CELL_EMP_SHARE * 0.5f;
        assertEquals(moved, hit.movedDamage().get(DamageType.HIGH_EXPLOSIVE), TOLERANCE);
        assertEquals(200f - moved, hit.emp(), TOLERANCE);
    }

    @Test
    void theShareNeverMovesMoreThanAllTheEmp() {
        HitMix.EmpOnlyResult hit = empOnlyHit(owner(300f, 2.5f), disabledWeaponUnderTheHit(1f), false);

        assertEquals(200f, hit.movedDamage().get(DamageType.HIGH_EXPLOSIVE), TOLERANCE);
        assertEquals(0f, hit.emp(), TOLERANCE);
    }

    @Test
    void theEmpIsMovedBeforeTheEmpBonusSoOnlyTheLeftoverEmpGetsIt() {
        HitMix.EmpOnlyResult hit = empOnlyHit(owner(50f), disabledWeaponUnderTheHit(1f), false, resolved -> resolved.increaseEmp(100f));

        float moved = 200f * IonCascadeListener.INNER_CELL_EMP_SHARE * 0.5f;
        assertEquals(moved, hit.movedDamage().get(DamageType.HIGH_EXPLOSIVE), TOLERANCE);
        assertEquals(2f, hit.empMult(), TOLERANCE);
        assertEquals((200f - moved) * 2f, hit.emp(), TOLERANCE);
    }

    @Test
    void anEmpOnlyShieldHitCascadesNothing() {
        HitMix.EmpOnlyResult hit = empOnlyHit(owner(50f), disabledWeaponUnderTheHit(1f), true);

        assertTrue(hit.movedDamage().isEmpty());
        assertEquals(200f, hit.emp(), TOLERANCE);
    }

    @Test
    void shieldHitsAndHitsWithoutEmpDoNothing() {
        ShipAPI target = disabledWeaponUnderTheHit(1f);
        IonCascadeListener listener = new IonCascadeListener(owner(100f));
        DamageAPI shieldHit = IonSpillListenerTest.hit(DamageType.KINETIC, 100f, 200f);
        DamageAPI noEmp = IonSpillListenerTest.hit(DamageType.KINETIC, 100f, 0f);

        listener.modifyDamageDealt(null, target, shieldHit, new Vector2f(), true);
        listener.modifyDamageDealt(null, target, noEmp, new Vector2f(), false);

        assertEquals(100f, shieldHit.getBaseDamage(), TOLERANCE);
        assertEquals(200f, shieldHit.getFluxComponent(), TOLERANCE);
        assertEquals(100f, noEmp.getBaseDamage(), TOLERANCE);
    }
}
