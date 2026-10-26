package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HitDamageRestorerTest {

    private MockedStatic<Global> globalMock;
    private CombatListenerManagerAPI listeners;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        listeners = mock(CombatListenerManagerAPI.class);
        when(engine.getListenerManager()).thenReturn(listeners);
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private DamageListener registeredRestorer() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(listeners).addListener(captor.capture());
        return (DamageListener) captor.getValue();
    }

    @Test
    void reducesTheBaseDamageForTheHitAndRestoresItOnceTheHitLandsOnThatTarget() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getBaseDamage()).thenReturn(100f);
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.reduceForThisHit(damage, 0.8f, target);
        verify(damage).setDamage(80f);

        DamageListener restorer = registeredRestorer();
        restorer.reportDamageApplied(null, target, null);
        restorer.reportDamageApplied(null, target, null);

        verify(damage, times(1)).setDamage(100f);
    }

    @Test
    void restoresAHitThatWasNeverReportedBeforeReducingTheNextOne() {
        DamageAPI unreported = mock(DamageAPI.class);
        when(unreported.getBaseDamage()).thenReturn(100f);
        DamageAPI next = mock(DamageAPI.class);
        when(next.getBaseDamage()).thenReturn(60f);
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.reduceForThisHit(unreported, 0.5f, target);
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));
        HitDamageRestorer.reduceForThisHit(next, 0.5f, target);

        verify(unreported).setDamage(100f);
        verify(next).setDamage(30f);
    }

    @Test
    void ignoresDamageReportsForOtherTargets() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getBaseDamage()).thenReturn(100f);
        ShipAPI target = mock(ShipAPI.class);
        ShipAPI bystander = mock(ShipAPI.class);

        HitDamageRestorer.reduceForThisHit(damage, 0.5f, target);
        registeredRestorer().reportDamageApplied(null, bystander, null);

        verify(damage, times(0)).setDamage(100f);
    }

    @Test
    void aChainReductionAndAnEmpConversionOnTheSameHitStackAndRestoreToTheOriginalValues() {
        DamageAPI damage = mock(DamageAPI.class);
        float[] base = {100f};
        float[] emp = {10f};
        when(damage.getBaseDamage()).thenAnswer(call -> base[0]);
        doAnswer(call -> base[0] = call.getArgument(0)).when(damage).setDamage(anyFloat());
        when(damage.getFluxComponent()).thenAnswer(call -> emp[0]);
        doAnswer(call -> emp[0] = call.getArgument(0)).when(damage).setFluxComponent(anyFloat());
        ShipAPI target = mock(ShipAPI.class);

        HitDamageRestorer.reduceForThisHit(damage, 0.5f, target);
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));
        HitDamageRestorer.moveToEmpForThisHit(damage, 0.2f, target);
        assertEquals(40f, base[0]);
        assertEquals(20f, emp[0]);

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

        HitDamageRestorer.reduceForThisHit(outer, 0.5f, mock(ShipAPI.class));
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));
        HitDamageRestorer.reduceForThisHit(nested, 0.5f, mock(ShipAPI.class));

        verify(outer, never()).setDamage(100f);
    }

    @Test
    void theOriginalHitDamageIgnoresWhatTheRestorerTookOutOfThisHit() {
        DamageAPI damage = mock(DamageAPI.class);
        float[] base = {100f};
        when(damage.getBaseDamage()).thenAnswer(call -> base[0]);
        doAnswer(call -> base[0] = call.getArgument(0)).when(damage).setDamage(anyFloat());
        MutableStat modifier = mock(MutableStat.class);
        when(modifier.getModifiedValue()).thenReturn(1.5f);
        when(damage.getModifier()).thenReturn(modifier);
        when(damage.getMultiplier()).thenReturn(2f);

        HitDamageRestorer.moveToEmpForThisHit(damage, 1f, mock(ShipAPI.class));
        HitDamageRestorer restorer = (HitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of(restorer));

        assertEquals(300f, HitDamageRestorer.originalHitDamage(damage), 0.001f);
    }
}
