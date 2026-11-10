package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.combat.systems.EmpArcEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.objenesis.ObjenesisStd;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmpOnlyHitsTest {

    private MockedStatic<Global> globalMock;
    private CombatEngineAPI engine;
    private EmpOnlyHits router;
    private ShipAPI dealer;
    private final List<EmpOnlyHits.EmpOnlyHit> seen = new ArrayList<>();

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
        router = new EmpOnlyHits();
        dealer = mock(ShipAPI.class);
        router.addHandler(dealer, seen::add);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI target(Object param, float empTakenMult) {
        ShipAPI target = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(target.getParamAboutToApplyDamage()).thenReturn(param);
        when(target.getMutableStats().getEmpDamageTakenMult().getModifiedValue()).thenReturn(empTakenMult);
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

    @Test
    void anEmpOnlyHullHitReachesTheDealersHandlersWithItsEmpBeforeResistancesAndAfterTheBonus() {
        Vector2f landing = new Vector2f(30f, 40f);
        ShipAPI target = target(arcLandingAt(landing), 0.5f);
        router.addEmpBonus(dealer, () -> 1.2f);

        router.reportDamageApplied(dealer, target, empApplied(100f));

        assertEquals(1, seen.size());
        EmpOnlyHits.EmpOnlyHit hit = seen.get(0);
        assertSame(target, hit.target());
        assertEquals(landing, hit.point());
        assertEquals(200f, hit.baseEmp(), 0.01f);
        assertEquals(240f, hit.emp(), 0.01f);
        assertFalse(hit.shieldHit());
    }

    @Test
    void anEmpOnlyShieldHitTakesItsEmpFromTheProjectile() {
        ShipAPI target = target(projectileWithEmp(150f), 1f);

        router.reportDamageApplied(dealer, target, empApplied(0f));

        assertEquals(1, seen.size());
        assertTrue(seen.get(0).shieldHit());
        assertEquals(150f, seen.get(0).emp(), 0.01f);
    }

    @Test
    void aHitTheModifierPassSawIsNotAnEmpOnlyHit() {
        EmpArcEntityAPI arc = arcLandingAt(new Vector2f());
        ShipAPI target = target(arc, 1f);

        router.modifyDamageDealt(arc, target, mock(DamageAPI.class), new Vector2f(), false);
        router.reportDamageApplied(dealer, target, empApplied(100f));

        assertTrue(seen.isEmpty());
    }

    @Test
    void aModifierPassFromAnEarlierFrameDoesNotHideALaterHit() {
        EmpArcEntityAPI arc = arcLandingAt(new Vector2f());
        ShipAPI target = target(arc, 1f);

        router.modifyDamageDealt(arc, target, mock(DamageAPI.class), new Vector2f(), false);
        when(engine.getTotalElapsedTime(false)).thenReturn(1f);
        router.reportDamageApplied(dealer, target, empApplied(100f));

        assertEquals(1, seen.size());
    }

    @Test
    void hitsWithoutAKnownPointOrEmpOrFromOtherShipsReachNoHandler() {
        router.reportDamageApplied(dealer, target(null, 1f), empApplied(100f));
        router.reportDamageApplied(dealer, target(arcLandingAt(new Vector2f()), 1f), empApplied(0f));
        router.reportDamageApplied(mock(ShipAPI.class), target(arcLandingAt(new Vector2f()), 1f), empApplied(100f));
        router.reportDamageApplied(null, target(arcLandingAt(new Vector2f()), 1f), empApplied(100f));

        assertTrue(seen.isEmpty());
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
    void aHullHitFromAnArcWhoseEmpCannotBeReadFallsBackToTheEmpItApplied() {
        ShipAPI target = target(arcLandingAt(new Vector2f()), 0.5f);

        router.reportDamageApplied(dealer, target, empApplied(100f));

        assertEquals(200f, seen.get(0).baseEmp(), 0.01f);
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
