package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChainHitDamageRestorerTest {

    private MockedStatic<Global> globalMock;
    private CombatListenerManagerAPI listeners;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        listeners = mock(CombatListenerManagerAPI.class);
        when(engine.getListenerManager()).thenReturn(listeners);
        when(listeners.getListeners(ChainHitDamageRestorer.class)).thenReturn(List.of());
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

        ChainHitDamageRestorer.reduceForThisHit(damage, 0.8f, target);
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

        ChainHitDamageRestorer.reduceForThisHit(unreported, 0.5f, target);
        ChainHitDamageRestorer restorer = (ChainHitDamageRestorer) registeredRestorer();
        when(listeners.getListeners(ChainHitDamageRestorer.class)).thenReturn(List.of(restorer));
        ChainHitDamageRestorer.reduceForThisHit(next, 0.5f, target);

        verify(unreported).setDamage(100f);
        verify(next).setDamage(30f);
    }

    @Test
    void ignoresDamageReportsForOtherTargets() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getBaseDamage()).thenReturn(100f);
        ShipAPI target = mock(ShipAPI.class);
        ShipAPI bystander = mock(ShipAPI.class);

        ChainHitDamageRestorer.reduceForThisHit(damage, 0.5f, target);
        registeredRestorer().reportDamageApplied(null, bystander, null);

        verify(damage, times(0)).setDamage(100f);
    }
}
