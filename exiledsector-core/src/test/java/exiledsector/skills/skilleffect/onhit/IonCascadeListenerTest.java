package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import exiledsector.skills.skilleffect.EmpOnlyHits;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IonCascadeListenerTest {

    private MockedStatic<Global> globalMock;
    private CombatEngineAPI engine;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

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
        ShipAPI target = mock(ShipAPI.class);
        when(target.getOwner()).thenReturn(1);
        when(target.isAlive()).thenReturn(true);
        when(target.getLocation()).thenReturn(new Vector2f());
        MutableShipStatsAPI targetStats = stats(1f, 1f);
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

    private static ShipAPI owner(float sharePercent) {
        ShipAPI owner = mock(ShipAPI.class, Mockito.RETURNS_DEEP_STUBS);
        when(owner.getOwner()).thenReturn(0);
        when(owner.getMutableStats().getDynamic().getValue(IonCascadeListener.HIGH_EXPLOSIVE_SHARE_PERCENT_KEY, 0f)).thenReturn(sharePercent);
        return owner;
    }

    private static DamageAPI empHit(float emp) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.computeFluxDealt(anyFloat())).thenReturn(emp);
        return damage;
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

        assertEquals(100f * IonCascadeListener.INNER_CELL_EMP_SHARE, convertible, 1e-4f);
    }

    @Test
    void anOuterRingPartAloneGivesTheOuterShare() {
        ShipAPI target = target(List.of(weaponAt(new Vector2f(5f, 0f), true)), List.of(engineAt(new Vector2f(0f, 2f), true)));

        float convertible = IonCascadeListener.empOnClosestDisabledPart(target, new Vector2f(), 100f, null);

        assertEquals(100f * IonCascadeListener.OUTER_CELL_EMP_SHARE, convertible, 1e-4f);
    }

    @Test
    void noDisabledPartInReachConvertsNothing() {
        ShipAPI target = target(List.of(weaponAt(new Vector2f(0f, 0f), false), weaponAt(new Vector2f(3f, 0f), true)), List.of());

        assertEquals(0f, IonCascadeListener.empOnClosestDisabledPart(target, new Vector2f(), 100f, null));
    }

    @Test
    void theHighExplosiveDamageLandsAtTheHitOnTheNextFrameScaledByTheShare() {
        ShipAPI target = target(List.of(weaponAt(new Vector2f(0f, 0f), true)), List.of());
        ShipAPI owner = owner(50f);
        IonCascadeListener listener = new IonCascadeListener(owner);
        Vector2f hitPoint = new Vector2f(0.2f, 0.1f);

        listener.modifyDamageDealt(null, target, empHit(200f), hitPoint, false);
        verifyNoDamageDealt();

        listener.advance(0.016f);
        verify(engine).applyDamage(eq(target), eq(hitPoint), eq(200f * IonCascadeListener.INNER_CELL_EMP_SHARE * 0.5f),
                eq(DamageType.HIGH_EXPLOSIVE), eq(0f), eq(true), eq(false), eq(owner));
    }

    @Test
    void anEmpOnlyHullHitCascadesItsEmpIncludingTheBonus() {
        ShipAPI target = target(List.of(weaponAt(new Vector2f(0f, 0f), true)), List.of());
        ShipAPI owner = owner(50f);
        when(owner.getMutableStats().getDamageToTargetWeaponsMult().getModifiedValue()).thenReturn(1f);
        IonCascadeListener listener = new IonCascadeListener(owner);

        listener.onEmpOnlyHit(new EmpOnlyHits.EmpOnlyHit(target, new Vector2f(), 100f, 200f, false));
        listener.onEmpOnlyHit(new EmpOnlyHits.EmpOnlyHit(target, new Vector2f(), 100f, 200f, true));
        listener.advance(0.016f);

        verify(engine).applyDamage(eq(target), any(), eq(200f * IonCascadeListener.INNER_CELL_EMP_SHARE * 0.5f),
                eq(DamageType.HIGH_EXPLOSIVE), eq(0f), eq(true), eq(false), eq(owner));
    }

    private void verifyNoDamageDealt() {
        verify(engine, never()).applyDamage(any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void shieldHitsAndHitsWithoutEmpDoNothing() {
        ShipAPI target = target(List.of(weaponAt(new Vector2f(0f, 0f), true)), List.of());
        IonCascadeListener listener = new IonCascadeListener(owner(100f));

        listener.modifyDamageDealt(null, target, empHit(200f), new Vector2f(), true);
        listener.modifyDamageDealt(null, target, empHit(0f), new Vector2f(), false);
        listener.advance(0.016f);

        verifyNoDamageDealt();
    }
}
