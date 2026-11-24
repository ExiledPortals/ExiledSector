package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmpArcLandingsTest {

    private static final Vector2f LANDING = new Vector2f(30f, 40f);

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();
    private CombatEngineAPI engine;
    private EmpOnlyHits router;
    private EmpArcLandings landings;
    private ShipAPI dealer;
    private ShipAPI target;
    private final List<HitMix.Resolver> resolvers = new ArrayList<>();
    private final Map<EmpArcEntityAPI, EmpArcLandings.ArcState> arcStates = new IdentityHashMap<>();

    @BeforeEach
    void setUp() {
        engine = combat.engine();
        router = new EmpOnlyHits(this::stateOf);
        landings = router.arcLandings();
        dealer = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(dealer.hasListenerOfClass(HitMix.Resolver.class)).thenReturn(true);
        when(dealer.getListeners(HitMix.Resolver.class)).thenAnswer(call -> resolvers);
        router.addDealer(dealer);
        target = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(target.isAlive()).thenReturn(true);
        when(target.isHulk()).thenReturn(false);
        when(target.getShield().getRadius()).thenReturn(0f);
        when(target.getShield().isOn()).thenReturn(true);
        when(target.getShield().isWithinArc(any())).thenReturn(true);
        when(target.getLocation()).thenReturn(new Vector2f());
        when(target.getCollisionRadius()).thenReturn(100f);
    }

    private void withIncreasedEmp(float percent) {
        resolvers.add(hit -> hit.increaseEmp(percent));
    }

    private void withMove(DamageType type, float share, boolean onShields) {
        resolvers.add(hit -> {
            if (hit.shieldHit() == onShields) {
                hit.convertEmp(type, share);
            }
        });
    }

    private EmpArcLandings.ArcState stateOf(EmpArcEntityAPI arc) {
        EmpArcLandings.ArcState state = arcStates.get(arc);
        return state == null ? null : new EmpArcLandings.ArcState(state.dealer(), state.target(), state.damage(), state.emp(), landed(arc));
    }

    private EmpArcEntityAPI arc(ShipAPI source, ShipAPI struck, float damage, float emp, Vector2f landing, boolean shieldHit) {
        EmpArcEntityAPI arc = EngineArcs.landingOn(landing, shieldHit);
        arcStates.put(arc, new EmpArcLandings.ArcState(source, struck, damage, emp, false));
        return arc;
    }

    private EmpArcEntityAPI arcInGrid(float damage, float emp, Vector2f landing, boolean shieldHit) {
        EmpArcEntityAPI arc = arc(dealer, target, damage, emp, landing, shieldHit);
        combat.allObjectGridOver(List.of(arc));
        return arc;
    }

    private void advanceWatching() {
        landings.watch(target);
        landings.advance(0.016f);
    }

    private static boolean landed(EmpArcEntityAPI arc) {
        return Boolean.TRUE.equals(EmpArcLandings.ARC_LANDED.read(arc));
    }

    private DamageDealtModifier recorder() {
        ArgumentCaptor<Object> listener = ArgumentCaptor.forClass(Object.class);
        verify(dealer).addListener(listener.capture());
        return (DamageDealtModifier) listener.getValue();
    }

    private ApplyDamageResultAPI empLanding(EmpArcEntityAPI arc) {
        when(target.getParamAboutToApplyDamage()).thenReturn(arc);
        when(target.getMutableStats().getEmpDamageTakenMult().getModifiedValue()).thenReturn(1f);
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getEmpDamage()).thenReturn(400f);
        return result;
    }

    private void verifyHit(EmpArcEntityAPI arc, float damage, DamageType type, float emp, boolean bypassShields) {
        verify(engine).applyDamage(eq(arc), eq(target), eq(LANDING), AdditionalMatchers.eq(damage, 0.01f), eq(type), AdditionalMatchers.eq(emp, 0.01f),
                eq(bypassShields), eq(false), eq(dealer), eq(false));
    }

    private void verifyHitCount(int count) {
        verify(engine, times(count)).applyDamage(any(), any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any(), anyBoolean());
    }

    private void verifyNothingApplied() {
        verifyHitCount(0);
    }

    private void verifyNoLateEmp() {
        verify(engine, never()).applyDamage(any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void aStoppedArcLandsItsLeftoverEmpAndItsMovedDamageAsSeparateHits() {
        withIncreasedEmp(50f);
        withMove(DamageType.HIGH_EXPLOSIVE, 0.5f, false);
        EmpArcEntityAPI arc = arcInGrid(0f, 400f, LANDING, false);

        advanceWatching();

        assertTrue(landed(arc));
        verifyHit(arc, 0f, DamageType.ENERGY, 300f, true);
        verifyHit(arc, 200f, DamageType.HIGH_EXPLOSIVE, 0f, true);
        verifyHitCount(2);
    }

    @Test
    void theEmpIsMovedFromTheArcsOwnEmpSoTheMovedDamageCarriesNoEmpBonus() {
        withIncreasedEmp(100f);
        withMove(DamageType.HIGH_EXPLOSIVE, 0.25f, false);
        EmpArcEntityAPI arc = arcInGrid(0f, 400f, LANDING, false);

        advanceWatching();

        verifyHit(arc, 100f, DamageType.HIGH_EXPLOSIVE, 0f, true);
        verifyHit(arc, 0f, DamageType.ENERGY, 600f, true);
    }

    private static float kineticLeftAfterConverting(EmpArcEntityAPI arc) {
        DamageAPI hitFromArc = MixedHitCombat.damage(DamageType.KINETIC);
        when(hitFromArc.getBaseDamage()).thenReturn(100f);
        HitMix.Resolver halfToEnergy = hit -> hit.convert(DamageType.KINETIC, DamageType.ENERGY, 0.5f);
        return HitMix.resolve(null, halfToEnergy, arc, mock(CombatEntityAPI.class), hitFromArc, LANDING, false).shares().share(DamageType.KINETIC);
    }

    @Test
    void theMovedDamageLandsAsAlreadyConverted() {
        withMove(DamageType.HIGH_EXPLOSIVE, 0.5f, false);
        EmpArcEntityAPI arc = arcInGrid(0f, 400f, LANDING, false);
        Map<DamageType, Float> kineticLeftByLandedType = new IdentityHashMap<>();
        doAnswer(call -> {
            kineticLeftByLandedType.put(call.getArgument(4), kineticLeftAfterConverting(arc));
            return null;
        }).when(engine).applyDamage(any(), any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any(), anyBoolean());

        advanceWatching();

        assertEquals(1f, kineticLeftByLandedType.get(DamageType.HIGH_EXPLOSIVE), 0.0001f);
        assertEquals(0.5f, kineticLeftAfterConverting(arc), 0.0001f);
    }

    @Test
    void anArcWhoseEmpIsAllMovedLandsOnlyTheMovedDamage() {
        withIncreasedEmp(50f);
        withMove(DamageType.HIGH_EXPLOSIVE, 1f, false);
        EmpArcEntityAPI arc = arcInGrid(0f, 400f, LANDING, false);

        advanceWatching();

        assertTrue(landed(arc));
        verifyHit(arc, 400f, DamageType.HIGH_EXPLOSIVE, 0f, true);
        verifyHitCount(1);
    }

    @Test
    void anArcWhoseEmpOnlyGrowsStillLandsAndTheExtraEmpLandsWithItInTheSameFrame() {
        withIncreasedEmp(50f);
        EmpArcEntityAPI arc = arcInGrid(0f, 400f, LANDING, false);

        advanceWatching();

        assertFalse(landed(arc));
        verifyHit(arc, 0f, DamageType.ENERGY, 200f, true);
        verifyHitCount(1);
    }

    @Test
    void anArcWhoseEmpShrinksIsStoppedAndLandsItsReducedEmpAsItsOwnHit() {
        withIncreasedEmp(-50f);
        EmpArcEntityAPI arc = arcInGrid(0f, 400f, LANDING, false);

        advanceWatching();

        assertTrue(landed(arc));
        verifyHit(arc, 0f, DamageType.ENERGY, 200f, true);
        verifyHitCount(1);
    }

    @Test
    void aShieldArcStillLandsAndOnlyItsMovedEmpIsAddedAsDamageOnTheShield() {
        withIncreasedEmp(50f);
        withMove(DamageType.ENERGY, 0.25f, true);
        EmpArcEntityAPI arc = arcInGrid(0f, 400f, LANDING, true);

        advanceWatching();

        assertFalse(landed(arc));
        verifyHit(arc, 100f, DamageType.ENERGY, 0f, false);
        verifyHitCount(1);
    }

    @Test
    void aShieldArcWithOnlyAnEmpBonusAddsNothing() {
        withIncreasedEmp(50f);
        EmpArcEntityAPI arc = arcInGrid(0f, 400f, LANDING, true);

        advanceWatching();

        assertFalse(landed(arc));
        verifyNothingApplied();
    }

    @Test
    void theRouterGivesNoLateEmpToHitsFromAnArcThisLanded() {
        withIncreasedEmp(50f);
        EmpArcEntityAPI arc = arcInGrid(0f, 400f, LANDING, false);

        advanceWatching();
        router.reportDamageApplied(dealer, target, empLanding(arc));
        landings.advance(0.016f);

        verifyNoLateEmp();
    }

    @Test
    void hitsThatCarryMovedDamageGetTheDealersStatsLikeTheArcWouldHave() {
        withIncreasedEmp(50f);
        withMove(DamageType.HIGH_EXPLOSIVE, 0.5f, false);
        DamageDealtModifier recorder = recorder();
        EmpArcEntityAPI arc = arcInGrid(0f, 400f, LANDING, false);
        DamageAPI landedHit = mock(DamageAPI.class);
        doAnswer(call -> recorder.modifyDamageDealt(arc, target, landedHit, LANDING, false)).when(engine)
                .applyDamage(any(), any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any(), anyBoolean());

        advanceWatching();

        verify(landedHit, times(2)).setStats(dealer.getMutableStats());
        DamageAPI otherHit = mock(DamageAPI.class);
        recorder.modifyDamageDealt(arc, target, otherHit, LANDING, false);
        verify(otherHit, never()).setStats(any());
    }

    @Test
    void theLeftoverEmpReachesPartsThroughTheDealersWeaponAndEngineMultipliers() {
        withIncreasedEmp(50f);
        when(dealer.getMutableStats().getDamageToTargetWeaponsMult().getModifiedValue()).thenReturn(1.3f);
        when(dealer.getMutableStats().getDamageToTargetEnginesMult().getModifiedValue()).thenReturn(1.2f);
        MutableStat weaponsTaken = target.getMutableStats().getWeaponDamageTakenMult();
        MutableStat enginesTaken = target.getMutableStats().getEngineDamageTakenMult();
        arcInGrid(0f, 400f, LANDING, false);

        advanceWatching();

        InOrder order = inOrder(weaponsTaken, enginesTaken, engine);
        order.verify(weaponsTaken).modifyMult(DealerComponentMults.MOD_ID, 1.3f);
        order.verify(enginesTaken).modifyMult(DealerComponentMults.MOD_ID, 1.2f);
        order.verify(engine).applyDamage(any(), any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any(), anyBoolean());
        order.verify(weaponsTaken).unmodify(DealerComponentMults.MOD_ID);
        order.verify(enginesTaken).unmodify(DealerComponentMults.MOD_ID);
    }

    @Test
    void aShieldThatDroppedBeforeTheArcLandsTakesNoMovedDamage() {
        withMove(DamageType.ENERGY, 0.25f, true);
        EmpArcEntityAPI arc = arcInGrid(0f, 400f, LANDING, true);
        when(target.getShield().isOn()).thenReturn(false);

        advanceWatching();
        router.reportDamageApplied(dealer, target, empLanding(arc));
        landings.advance(0.016f);

        assertFalse(landed(arc));
        verifyNothingApplied();
        verifyNoLateEmp();
    }

    @Test
    void arcsThatCarryDamageHaveLandedComeFromOtherShipsOrStrikeWrecksAreLeftToTheEngine() {
        withIncreasedEmp(50f);
        ShipAPI destroyed = mock(ShipAPI.class);
        when(destroyed.getLocation()).thenReturn(new Vector2f());
        EmpArcEntityAPI atWreck = arc(dealer, destroyed, 0f, 400f, LANDING, false);
        EmpArcEntityAPI withDamage = arc(dealer, target, 50f, 400f, LANDING, false);
        EmpArcEntityAPI alreadyLanded = arc(dealer, target, 0f, 400f, LANDING, false);
        EmpArcEntityAPI otherSource = arc(mock(ShipAPI.class), target, 0f, 400f, LANDING, false);
        EmpArcEntityAPI farAway = arc(dealer, target, 0f, 400f, new Vector2f(500f, 0f), false);
        EmpArcLandings.ARC_LANDED.write(alreadyLanded, true);
        combat.allObjectGridOver(List.of(withDamage, alreadyLanded, otherSource, farAway, atWreck, mock(EmpArcEntityAPI.class)));

        advanceWatching();

        verifyNothingApplied();
    }

    @Test
    void anArcWithNothingToChangeIsLeftToTheEngineAndGetsNoLateEmp() {
        withMove(DamageType.ENERGY, 0.25f, true);
        EmpArcEntityAPI arc = arcInGrid(0f, 400f, LANDING, false);

        advanceWatching();
        router.reportDamageApplied(dealer, target, empLanding(arc));
        landings.advance(0.016f);

        assertFalse(landed(arc));
        verifyNothingApplied();
        verifyNoLateEmp();
    }

    @Test
    void onlyTargetsStruckByProjectilesWithOnHitEffectsAreSearched() {
        withIncreasedEmp(50f);
        DamageDealtModifier modifiedHits = recorder();
        DamagingProjectileAPI plain = mock(DamagingProjectileAPI.class, RETURNS_DEEP_STUBS);
        when(plain.getProjectileSpec().getOnHitEffect()).thenReturn(null);

        modifiedHits.modifyDamageDealt(plain, target, null, LANDING, false);
        landings.advance(0.016f);
        verify(engine, never()).getAllObjectGrid();

        MissileAPI arcing = mock(MissileAPI.class, RETURNS_DEEP_STUBS);
        when(arcing.getSpec().getOnHitEffect()).thenReturn(mock(OnHitEffectPlugin.class));
        arcInGrid(0f, 400f, LANDING, false);
        modifiedHits.modifyDamageDealt(arcing, target, null, LANDING, false);
        landings.advance(0.016f);

        verify(engine).getAllObjectGrid();
    }

    @Test
    void theEngineArcStillHasTheFieldsThisReadsAndWrites() {
        EmpArcEntityAPI arc = EngineArcs.landingOn(LANDING, false);

        assertTrue(EmpArcLandings.ARC_TARGET.reachable());
        assertEquals(0f, EmpArcLandings.ARC_DAMAGE.read(arc));
        assertEquals(false, EmpArcLandings.ARC_LANDED.read(arc));
        assertTrue(EmpArcLandings.ARC_LANDED.write(arc, true));
        assertTrue(landed(arc));
    }
}
