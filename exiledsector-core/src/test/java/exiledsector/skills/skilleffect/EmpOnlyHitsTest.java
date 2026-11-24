package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.combat.systems.EmpArcEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.objenesis.ObjenesisStd;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmpOnlyHitsTest {

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();
    private CombatEngineAPI engine;
    private EmpOnlyHits router;
    private ShipAPI dealer;
    private final List<HitMix.Resolver> resolvers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        engine = combat.engine();
        router = new EmpOnlyHits();
        dealer = dealerWith(resolvers);
        router.addDealer(dealer);
    }

    private static ShipAPI dealerWith(List<HitMix.Resolver> dealerResolvers) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.hasListenerOfClass(HitMix.Resolver.class)).thenReturn(true);
        when(ship.getListeners(HitMix.Resolver.class)).thenAnswer(call -> dealerResolvers);
        return ship;
    }

    private void withEmpBonus(float increasedPercent, float moreMult) {
        resolvers.add(hit -> {
            hit.increaseEmp(increasedPercent);
            hit.moreEmp(moreMult);
        });
    }

    private DamageDealtModifier dealersModifierPass() {
        ArgumentCaptor<Object> listener = ArgumentCaptor.forClass(Object.class);
        verify(dealer).addListener(listener.capture());
        return (DamageDealtModifier) listener.getValue();
    }

    private static ShipAPI target(Object param, float empTakenMult) {
        ShipAPI target = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(target.getParamAboutToApplyDamage()).thenReturn(param);
        when(target.getMutableStats().getEmpDamageTakenMult().getModifiedValue()).thenReturn(empTakenMult);
        when(target.isAlive()).thenReturn(true);
        when(target.isHulk()).thenReturn(false);
        when(target.isPhased()).thenReturn(false);
        when(target.getLocation()).thenReturn(new Vector2f());
        when(target.getFacing()).thenReturn(0f);
        return target;
    }

    private static ApplyDamageResultAPI empApplied(float emp) {
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getEmpDamage()).thenReturn(emp);
        return result;
    }

    private static EmpArcEntityAPI arcLandingAt(Vector2f point) {
        EmpArcEntityAPI arc = mock(EmpArcEntityAPI.class);
        when(arc.getTargetLocation()).thenReturn(point);
        return arc;
    }

    private static DamagingProjectileAPI projectileWithEmp(float emp) {
        DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class);
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.computeFluxDealt(anyFloat())).thenReturn(emp);
        when(projectile.getDamage()).thenReturn(damage);
        when(projectile.getLocation()).thenReturn(new Vector2f(5f, 5f));
        return projectile;
    }

    private static Vector2f at(float x, float y) {
        return argThat(point -> point != null && Math.abs(point.x - x) < 0.01f && Math.abs(point.y - y) < 0.01f);
    }

    private void verifyLateEmp(ShipAPI target, float emp) {
        verify(engine).applyDamage(eq(target), any(), eq(0f), eq(DamageType.ENERGY), AdditionalMatchers.eq(emp, 0.01f), eq(true), eq(false),
                eq(dealer));
    }

    private void verifyNoLateEmp() {
        verify(engine, never()).applyDamage(any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void anEmpOnlyHullHitGetsItsExtraEmpAsAZeroDamageHitOnTheNextFrame() {
        ShipAPI target = target(arcLandingAt(new Vector2f(30f, 40f)), 0.5f);
        withEmpBonus(10f, 1.2f);

        router.reportDamageApplied(dealer, target, empApplied(100f));
        verifyNoLateEmp();
        router.applyLateEmpHits(engine);

        verify(engine).applyDamage(eq(target), at(30f, 40f), eq(0f), eq(DamageType.ENERGY), AdditionalMatchers.eq(64f, 0.01f), eq(true),
                eq(false), eq(dealer));
    }

    @Test
    void theDealersResolversSeeAHullHitWithItsEmpBeforeResistances() {
        Vector2f landing = new Vector2f(30f, 40f);
        EmpArcEntityAPI arc = arcLandingAt(landing);
        ShipAPI target = target(arc, 0.5f);
        List<HitMix.Hit> seen = new ArrayList<>();
        resolvers.add(seen::add);

        router.reportDamageApplied(dealer, target, empApplied(100f));

        assertEquals(1, seen.size());
        HitMix.Hit hit = seen.get(0);
        assertSame(arc, hit.param());
        assertSame(target, hit.target());
        assertEquals(landing, hit.point());
        assertFalse(hit.shieldHit());
        assertEquals(200f, hit.originalEmp(), 0.01f);
    }

    @Test
    void aHullHitTakesItsEmpFromTheProjectileWhenItCarriesSome() {
        ShipAPI target = target(projectileWithEmp(150f), 0.5f);
        withEmpBonus(20f, 1f);

        router.reportDamageApplied(dealer, target, empApplied(75f));
        router.applyLateEmpHits(engine);

        verifyLateEmp(target, 30f);
    }

    @Test
    void hullHitsWithoutAnEmpScaleAboveOneGetNoLateEmp() {
        List<HitMix.Resolver> shrinking = List.<HitMix.Resolver>of(hit -> hit.increaseEmp(-50f));
        ShipAPI shrinkingDealer = dealerWith(shrinking);
        router.addDealer(shrinkingDealer);

        router.reportDamageApplied(dealer, target(arcLandingAt(new Vector2f()), 1f), empApplied(100f));
        router.reportDamageApplied(shrinkingDealer, target(arcLandingAt(new Vector2f()), 1f), empApplied(100f));
        router.applyLateEmpHits(engine);

        verifyNoLateEmp();
    }

    @Test
    void aHullHitWhoseEmpWouldBeMovedGetsNoMovedDamageAndItsWholeExtraEmpLate() {
        ShipAPI target = target(arcLandingAt(new Vector2f()), 1f);
        resolvers.add(hit -> hit.convertEmp(DamageType.HIGH_EXPLOSIVE, 0.5f));
        withEmpBonus(50f, 1f);

        router.reportDamageApplied(dealer, target, empApplied(200f));
        router.applyLateEmpHits(engine);

        verifyLateEmp(target, 100f);
        verify(engine, never()).applyDamage(any(), any(), anyFloat(), eq(DamageType.HIGH_EXPLOSIVE), anyFloat(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void aMovingResolverAloneGivesAHullHitNoLateHit() {
        resolvers.add(hit -> hit.convertEmp(DamageType.HIGH_EXPLOSIVE, 0.5f));

        router.reportDamageApplied(dealer, target(arcLandingAt(new Vector2f()), 1f), empApplied(200f));
        router.applyLateEmpHits(engine);

        verifyNoLateEmp();
    }

    @Test
    void anEmpOnlyShieldHitGetsNoLateEmpAndResolvesNothing() {
        ShipAPI target = target(projectileWithEmp(150f), 1f);
        List<HitMix.Hit> seen = new ArrayList<>();
        resolvers.add(seen::add);
        withEmpBonus(50f, 1f);

        router.reportDamageApplied(dealer, target, empApplied(0f));
        router.applyLateEmpHits(engine);

        assertTrue(seen.isEmpty());
        verifyNoLateEmp();
    }

    @Test
    void theLateEmpLandsWhereTheHitStruckAfterTheTargetMovesAndTurns() {
        ShipAPI target = target(arcLandingAt(new Vector2f(10f, 0f)), 1f);
        withEmpBonus(20f, 1f);

        router.reportDamageApplied(dealer, target, empApplied(200f));
        when(target.getLocation()).thenReturn(new Vector2f(100f, 100f));
        when(target.getFacing()).thenReturn(90f);
        router.applyLateEmpHits(engine);

        verify(engine).applyDamage(eq(target), at(100f, 110f), eq(0f), eq(DamageType.ENERGY), anyFloat(), eq(true), eq(false), eq(dealer));
    }

    @Test
    void targetsPhasedOrDestroyedByTheNextFrameGetNoLateEmp() {
        ShipAPI phased = target(arcLandingAt(new Vector2f()), 1f);
        ShipAPI destroyed = target(arcLandingAt(new Vector2f()), 1f);
        withEmpBonus(20f, 1f);

        router.reportDamageApplied(dealer, phased, empApplied(200f));
        router.reportDamageApplied(dealer, destroyed, empApplied(200f));
        when(phased.isPhased()).thenReturn(true);
        when(destroyed.isAlive()).thenReturn(false);
        router.applyLateEmpHits(engine);

        verifyNoLateEmp();
    }

    @Test
    void aLateEmpHitLandsOnlyOnce() {
        ShipAPI target = target(arcLandingAt(new Vector2f()), 1f);
        withEmpBonus(20f, 1f);

        router.reportDamageApplied(dealer, target, empApplied(200f));
        router.applyLateEmpHits(engine);
        router.applyLateEmpHits(engine);

        verify(engine, times(1)).applyDamage(any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void aHitTheModifierPassSawGetsNoLateEmp() {
        EmpArcEntityAPI arc = arcLandingAt(new Vector2f());
        ShipAPI target = target(arc, 1f);
        withEmpBonus(20f, 1f);

        dealersModifierPass().modifyDamageDealt(arc, target, mock(DamageAPI.class), new Vector2f(), false);
        router.reportDamageApplied(dealer, target, empApplied(100f));
        router.applyLateEmpHits(engine);

        verifyNoLateEmp();
    }

    @Test
    void aModifierPassFromAnEarlierFrameDoesNotHideALaterHit() {
        EmpArcEntityAPI arc = arcLandingAt(new Vector2f());
        ShipAPI target = target(arc, 1f);
        withEmpBonus(20f, 1f);

        dealersModifierPass().modifyDamageDealt(arc, target, mock(DamageAPI.class), new Vector2f(), false);
        when(engine.getTotalElapsedTime(false)).thenReturn(1f);
        router.reportDamageApplied(dealer, target, empApplied(100f));
        router.applyLateEmpHits(engine);

        verifyLateEmp(target, 20f);
    }

    @Test
    void aModifierPassOnAnotherTargetDoesNotHideAHit() {
        EmpArcEntityAPI arc = arcLandingAt(new Vector2f());
        ShipAPI target = target(arc, 1f);
        withEmpBonus(20f, 1f);

        dealersModifierPass().modifyDamageDealt(arc, mock(ShipAPI.class), mock(DamageAPI.class), new Vector2f(), false);
        router.reportDamageApplied(dealer, target, empApplied(100f));
        router.applyLateEmpHits(engine);

        verifyLateEmp(target, 20f);
    }

    @Test
    void aDealerIsRecordedOnceAndOnlyRegisteredDealersAreRead() {
        ShipAPI unregistered = dealerWith(resolvers);
        withEmpBonus(20f, 1f);
        router.addDealer(dealer);

        verify(dealer, times(1)).addListener(any());
        verify(unregistered, never()).addListener(any());
        assertTrue(router.landsArcsOf(dealer));
        assertFalse(router.landsArcsOf(unregistered));
        assertFalse(DamageDealtModifier.class.isAssignableFrom(EmpOnlyHits.class));

        ShipAPI target = target(arcLandingAt(new Vector2f()), 1f);
        router.reportDamageApplied(unregistered, target, empApplied(100f));

        verify(target, never()).getParamAboutToApplyDamage();
    }

    @Test
    void registeringOutsideCombatAddsNoRecorder() {
        ShipAPI ship = dealerWith(resolvers);

        EmpOnlyHits.register(ship);

        verify(ship, never()).addListener(any());
    }

    @Test
    void hitsWithoutAKnownPointOrEmpOrFromOtherShipsGetNoLateEmp() {
        withEmpBonus(20f, 1f);

        router.reportDamageApplied(dealer, target(null, 1f), empApplied(100f));
        router.reportDamageApplied(dealer, target(arcLandingAt(new Vector2f()), 1f), empApplied(0f));
        router.reportDamageApplied(dealer, target(arcLandingAt(new Vector2f()), 0f), empApplied(100f));
        router.reportDamageApplied(mock(ShipAPI.class), target(arcLandingAt(new Vector2f()), 1f), empApplied(100f));
        router.reportDamageApplied(null, target(arcLandingAt(new Vector2f()), 1f), empApplied(100f));
        router.applyLateEmpHits(engine);

        verifyNoLateEmp();
    }

    @Test
    void theEngineArcStillKeepsItsEmpInTheFieldThisReads() throws ClassNotFoundException {
        Object engineArc = new ObjenesisStd().newInstance(Class.forName(EmpOnlyHits.ENGINE_ARC_CLASS));

        assertTrue(EmpOnlyHits.ARC_EMP.reachable());
        assertEquals(0f, EmpOnlyHits.arcEmp((EmpArcEntityAPI) engineArc));
        assertTrue(EmpOnlyHits.ARC_EMP.read(engineArc) instanceof Float);
    }

    static final class ModdedArc extends EmpArcEntity {
        ModdedArc() {
            super(null, null, null, null, null, 0f, 0f, 0f, null, 0f, null, null, false, null);
        }
    }

    @Test
    void engineArcsAndTheirSubclassesAreReadButOtherArcClassesAreNot() {
        ModdedArc engineArcSubclass = new ObjenesisStd().newInstance(ModdedArc.class);

        assertTrue(EmpOnlyHits.isEngineArc(engineArcSubclass));
        assertTrue(EmpOnlyHits.ARC_EMP.read(engineArcSubclass) instanceof Float);
        assertFalse(EmpOnlyHits.isEngineArc(mock(EmpArcEntityAPI.class)));
        assertEquals(0f, EmpOnlyHits.arcEmp(mock(EmpArcEntityAPI.class)));
    }

    @Test
    void theHitPointComesFromTheArcProjectileOrBeam() {
        Vector2f arcEnd = new Vector2f(1f, 1f);
        DamagingProjectileAPI projectile = projectileWithEmp(0f);
        Vector2f beamEnd = new Vector2f(3f, 3f);
        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getTo()).thenReturn(beamEnd);

        assertSame(arcEnd, EmpOnlyHits.hitPoint(arcLandingAt(arcEnd)));
        assertSame(projectile.getLocation(), EmpOnlyHits.hitPoint(projectile));
        assertSame(beamEnd, EmpOnlyHits.hitPoint(beam));
        assertNull(EmpOnlyHits.hitPoint(null));
    }
}
