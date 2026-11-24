package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HitDamageRestorerTest {

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();
    private CombatListenerManagerAPI listeners;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = combat.engine();
        listeners = mock(CombatListenerManagerAPI.class);
        when(engine.getListenerManager()).thenReturn(listeners);
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of());
    }

    private DamageListener registeredRestorer() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(listeners).addListener(captor.capture());
        return (DamageListener) captor.getValue();
    }

    private static DamageAPI trackedDamage(float[] base, float[] emp) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getBaseDamage()).thenAnswer(call -> base[0]);
        doAnswer(call -> base[0] = call.getArgument(0)).when(damage).setDamage(anyFloat());
        when(damage.getFluxComponent()).thenAnswer(call -> emp[0]);
        doAnswer(call -> emp[0] = call.getArgument(0)).when(damage).setFluxComponent(anyFloat());
        return damage;
    }

    @Test
    void scalesTheBaseDamageForTheHitAndRestoresItOnceTheHitLandsOnThatTarget() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getBaseDamage()).thenReturn(100f);
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.scaleDamageForThisHit(damage, 0.8f, target);
        verify(damage).setDamage(80f);

        DamageListener restorer = registeredRestorer();
        restorer.reportDamageApplied(null, target, null);
        restorer.reportDamageApplied(null, target, null);

        verify(damage, times(1)).setDamage(100f);
    }

    @Test
    void settingTheHitsDamageAndEmpIsUndoneOnceTheHitLands() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getBaseDamage()).thenReturn(100f);
        when(damage.getFluxComponent()).thenReturn(200f);
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.setForThisHit(damage, 150f, 150f, target);
        verify(damage).setDamage(150f);
        verify(damage).setFluxComponent(150f);

        registeredRestorer().reportDamageApplied(null, target, null);

        verify(damage).setDamage(100f);
        verify(damage).setFluxComponent(200f);
    }

    @Test
    void anotherModsChangeToTheHitSurvivesTheRestore() {
        float[] base = {100f};
        float[] emp = {200f};
        DamageAPI damage = trackedDamage(base, emp);
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.scaleDamageForThisHit(damage, 1.2f, target);
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));
        HitDamageRestorer.scaleEmpForThisHit(damage, 1.5f, target);
        damage.setDamage(base[0] * 0.5f);
        damage.setFluxComponent(emp[0] + 30f);
        restorer.reportDamageApplied(null, target, null);

        assertEquals(50f, base[0], 1e-3f);
        assertEquals(200f * 330f / 300f, emp[0], 1e-3f);
    }

    @Test
    void anotherModsChangeBetweenTwoOfOurEditsIsKept() {
        float[] base = {100f};
        float[] emp = {0f};
        DamageAPI damage = trackedDamage(base, emp);
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.scaleDamageForThisHit(damage, 2f, target);
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));
        damage.setDamage(base[0] * 0.5f);
        HitDamageRestorer.scaleDamageForThisHit(damage, 1.5f, target);
        restorer.reportDamageApplied(null, target, null);

        assertEquals(50f, base[0], 1e-3f);
        assertEquals(0f, emp[0], 1e-3f);
    }

    @Test
    void damageAnotherModAddsAfterTheHitWasFullyConvertedIsAddedBack() {
        float[] base = {100f};
        float[] emp = {0f};
        DamageAPI damage = trackedDamage(base, emp);
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.setForThisHit(damage, 0f, 100f, target);
        damage.setDamage(25f);
        registeredRestorer().reportDamageApplied(null, target, null);

        assertEquals(125f, base[0], 1e-3f);
        assertEquals(0f, emp[0], 1e-3f);
    }

    @Test
    void scalesTheEmpForTheHitAndRestoresItOnceTheHitLands() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getFluxComponent()).thenReturn(40f);
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.scaleEmpForThisHit(damage, 1.5f, target);
        verify(damage).setFluxComponent(60f);

        registeredRestorer().reportDamageApplied(null, target, null);

        verify(damage).setFluxComponent(40f);
    }

    @Test
    void restoresAHitThatWasNeverReportedBeforeScalingTheNextOne() {
        DamageAPI unreported = mock(DamageAPI.class);
        when(unreported.getBaseDamage()).thenReturn(100f);
        DamageAPI next = mock(DamageAPI.class);
        when(next.getBaseDamage()).thenReturn(60f);
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.scaleDamageForThisHit(unreported, 0.5f, target);
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));
        HitDamageRestorer.scaleDamageForThisHit(next, 0.5f, target);

        verify(unreported).setDamage(100f);
        verify(next).setDamage(30f);
    }

    @Test
    void aRetypedHitGetsItsOriginalTypeBackOnceItLands() {
        DamageAPI damage = mock(DamageAPI.class);
        DamageType[] type = {DamageType.FRAGMENTATION};
        when(damage.getType()).thenAnswer(call -> type[0]);
        doAnswer(call -> type[0] = call.getArgument(0)).when(damage).setType(any());
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.retypeForThisHit(damage, DamageType.HIGH_EXPLOSIVE, target);
        assertEquals(DamageType.HIGH_EXPLOSIVE, type[0]);

        registeredRestorer().reportDamageApplied(null, target, null);
        assertEquals(DamageType.FRAGMENTATION, type[0]);
    }

    @Test
    void ignoresDamageReportsForOtherTargets() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getBaseDamage()).thenReturn(100f);
        ShipAPI target = mock(ShipAPI.class);
        ShipAPI bystander = mock(ShipAPI.class);

        HitDamageRestorer.scaleDamageForThisHit(damage, 0.5f, target);
        registeredRestorer().reportDamageApplied(null, bystander, null);

        verify(damage, times(0)).setDamage(100f);
    }

    @Test
    void aDamageScaleAndAnEmpScaleOnTheSameHitStackAndRestoreToTheOriginalValues() {
        float[] base = {100f};
        float[] emp = {10f};
        DamageAPI damage = trackedDamage(base, emp);
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.scaleDamageForThisHit(damage, 0.5f, target);
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));
        HitDamageRestorer.scaleEmpForThisHit(damage, 2f, target);
        assertEquals(50f, base[0]);
        assertEquals(20f, emp[0]);

        restorer.reportDamageApplied(null, target, null);
        assertEquals(100f, base[0]);
        assertEquals(10f, emp[0]);
    }

    @Test
    void settingAHitAlreadyScaledStillRestoresTheValuesFromBeforeEither() {
        float[] base = {100f};
        float[] emp = {10f};
        DamageAPI damage = trackedDamage(base, emp);
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.scaleDamageForThisHit(damage, 2f, target);
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));
        HitDamageRestorer.setForThisHit(damage, 30f, 70f, target);
        assertEquals(30f, base[0]);
        assertEquals(70f, emp[0]);

        restorer.reportDamageApplied(null, target, null);
        assertEquals(100f, base[0]);
        assertEquals(10f, emp[0]);
    }

    @Test
    void aNestedHitOnAnotherTargetLeavesThePendingHitAlone() {
        DamageAPI outer = mock(DamageAPI.class);
        when(outer.getBaseDamage()).thenReturn(100f);
        DamageAPI nested = mock(DamageAPI.class);
        when(nested.getBaseDamage()).thenReturn(60f);

        HitDamageRestorer.scaleDamageForThisHit(outer, 0.5f, mock(ShipAPI.class));
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));
        HitDamageRestorer.scaleDamageForThisHit(nested, 0.5f, mock(ShipAPI.class));

        verify(outer, never()).setDamage(100f);
    }

    @Test
    void aHitAnotherModDealsToTheSameTargetMidHitUndoesOnlyItsOwnChanges() {
        float[] outerBase = {100f};
        DamageAPI outer = trackedDamage(outerBase, new float[] {0f});
        float[] nestedBase = {60f};
        DamageAPI nested = trackedDamage(nestedBase, new float[] {0f});
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.scaleDamageForThisHit(outer, 0.5f, target);
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));
        restorer.hitStarted(outer, target);
        restorer.hitStarted(nested, target);
        HitDamageRestorer.scaleDamageForThisHit(nested, 0.5f, target);

        assertEquals(50f, outerBase[0]);
        restorer.reportDamageApplied(null, target, null);
        assertEquals(60f, nestedBase[0]);
        assertEquals(50f, outerBase[0]);

        restorer.reportDamageApplied(null, target, null);
        assertEquals(100f, outerBase[0]);
    }

    @Test
    void aHitWithNoDamageDealtFromInsideAHitOnTheSameTargetLeavesTheOuterHitAlone() {
        float[] base = {100f};
        DamageAPI outer = trackedDamage(base, new float[] {0f});
        when(outer.getType()).thenReturn(DamageType.ENERGY);
        when(outer.getDamage()).thenAnswer(call -> base[0]);
        ShipAPI target = mock(ShipAPI.class);
        HitDamageRestorer.scaleDamageForThisHit(outer, 1.5f, target);
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));
        restorer.hitStarted(outer, target);

        restorer.reportDamageApplied(null, target, landed(DamageType.ENERGY, 0f));
        assertEquals(150f, base[0]);

        restorer.reportDamageApplied(null, target, landed(DamageType.ENERGY, 150f));
        assertEquals(100f, base[0]);
    }

    @Test
    void aChangeWhoseHitNeverLandedIsUndoneWhenTheFrameEnds() {
        float[] base = {100f};
        DamageAPI damage = trackedDamage(base, new float[] {0f});
        ShipAPI target = mock(ShipAPI.class);
        HitDamageRestorer.scaleDamageForThisHit(damage, 0.5f, target);
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        restorer.hitStarted(damage, target);

        restorer.endFrame();

        assertEquals(100f, base[0]);
    }

    private static ApplyDamageResultAPI landed(DamageType type, float damageToHull) {
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getType()).thenReturn(type);
        when(result.getDamageToHull()).thenReturn(damageToHull);
        return result;
    }

    @Test
    void theOriginalHitDamageIgnoresWhatTheRestorerTookOutOfThisHit() {
        float[] base = {100f};
        float[] emp = {0f};
        DamageAPI damage = trackedDamage(base, emp);
        MutableStat modifier = mock(MutableStat.class);
        when(modifier.getModifiedValue()).thenReturn(1.5f);
        when(damage.getModifier()).thenReturn(modifier);
        when(damage.getMultiplier()).thenReturn(2f);

        HitDamageRestorer.setForThisHit(damage, 0f, 100f, mock(ShipAPI.class));
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));

        assertEquals(300f, HitDamageRestorer.originalHitDamage(damage), 0.001f);
    }

    @Test
    void sideDamageLandingOnTheSameTargetMidHitLeavesTheOuterHitChangedUntilItLands() {
        DamageAPI damage = mock(DamageAPI.class);
        DamageType[] type = {DamageType.ENERGY};
        when(damage.getType()).thenAnswer(call -> type[0]);
        doAnswer(call -> type[0] = call.getArgument(0)).when(damage).setType(any());
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.retypeForThisHit(damage, DamageType.KINETIC, target);
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));
        HitDamageRestorer.whileDealingSideDamage(() -> {
            restorer.reportDamageApplied(null, target, null);
            return null;
        });
        assertEquals(DamageType.KINETIC, type[0]);

        restorer.reportDamageApplied(null, target, null);
        assertEquals(DamageType.ENERGY, type[0]);
    }

    @Test
    void sideDamageTrackedOnTheSameTargetIsRestoredByItsOwnReportWithoutTouchingTheOuterHit() {
        DamageAPI outer = mock(DamageAPI.class);
        when(outer.getBaseDamage()).thenReturn(100f);
        DamageAPI side = mock(DamageAPI.class);
        when(side.getBaseDamage()).thenReturn(40f);
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.scaleDamageForThisHit(outer, 0.5f, target);
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));
        HitDamageRestorer.whileDealingSideDamage(() -> {
            HitDamageRestorer.scaleDamageForThisHit(side, 0.5f, target);
            restorer.reportDamageApplied(null, target, null);
            return null;
        });

        verify(side).setDamage(40f);
        verify(outer, never()).setDamage(100f);
        restorer.reportDamageApplied(null, target, null);
        verify(outer).setDamage(100f);
    }
}
