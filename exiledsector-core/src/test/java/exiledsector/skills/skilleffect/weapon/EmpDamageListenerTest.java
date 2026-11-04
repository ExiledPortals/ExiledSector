package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.AdditionalMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmpDamageListenerTest {

    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        CombatListenerManagerAPI listeners = mock(CombatListenerManagerAPI.class);
        when(engine.getListenerManager()).thenReturn(listeners);
        when(listeners.getListeners(HitDamageRestorer.class)).thenReturn(List.of());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static EmpDamageListener listener(float percent) {
        ShipAPI owner = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(owner.getMutableStats().getDynamic().getValue(EmpDamageListener.EMP_PERCENT_KEY, 0f)).thenReturn(percent);
        return new EmpDamageListener(owner);
    }

    @Test
    void empOnAShipHitIsScaledByItsOwnMultiplier() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getFluxComponent()).thenReturn(200f);

        listener(15f).modifyDamageDealt(null, mock(ShipAPI.class), damage, new Vector2f(), false);

        verify(damage).setFluxComponent(AdditionalMatchers.eq(230f, 0.01f));
    }

    @Test
    void hitsWithoutEmpAndHitsOnOtherTargetsAreLeftAlone() {
        DamageAPI noEmp = mock(DamageAPI.class);
        DamageAPI onMissile = mock(DamageAPI.class);
        when(onMissile.getFluxComponent()).thenReturn(200f);
        EmpDamageListener listener = listener(15f);

        listener.modifyDamageDealt(null, mock(ShipAPI.class), noEmp, new Vector2f(), false);
        listener.modifyDamageDealt(null, mock(CombatEntityAPI.class), onMissile, new Vector2f(), false);

        verify(noEmp, never()).setFluxComponent(anyFloat());
        verify(onMissile, never()).setFluxComponent(anyFloat());
    }

    @Test
    void theMultiplierNeverGoesBelowZero() {
        assertEquals(1.1f, EmpDamageListener.empMult(10f), 0.0001f);
        assertEquals(0f, EmpDamageListener.empMult(-150f));
    }
}
