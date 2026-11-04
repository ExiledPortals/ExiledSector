package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StrikecraftEmpListenerTest {

    private final List<Object> engineListeners = new ArrayList<>();
    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        CombatListenerManagerAPI listenerManager = mock(CombatListenerManagerAPI.class);
        when(engine.getListenerManager()).thenReturn(listenerManager);
        when(listenerManager.getListeners(any())).thenAnswer(call -> new ArrayList<>(engineListeners));
        doAnswer(call -> engineListeners.add(call.getArgument(0))).when(listenerManager).addListener(any());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        return ship;
    }

    private static ShipAPI carrier(float percent) {
        ShipAPI carrier = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(carrier.getMutableStats().getDynamic().getValue(StrikecraftEmpListener.EMP_PERCENT_KEY, 0f)).thenReturn(percent);
        return carrier;
    }

    private static DamageAPI damage(float amount, float emp) {
        DamageAPI damage = mock(DamageAPI.class);
        float[] empComponent = {emp};
        when(damage.getDamage()).thenReturn(amount);
        when(damage.getBaseDamage()).thenReturn(amount);
        when(damage.getFluxComponent()).thenAnswer(call -> empComponent[0]);
        doAnswer(call -> empComponent[0] = call.getArgument(0)).when(damage).setFluxComponent(anyFloat());
        return damage;
    }

    @Test
    void hullHitsOnEnemiesCarryExtraEmpForThatHitOnly() {
        ShipAPI enemy = ship(1);
        DamageAPI damage = damage(200f, 30f);
        StrikecraftEmpListener listener = new StrikecraftEmpListener(ship(0), carrier(10f));

        assertNull(listener.modifyDamageDealt(null, enemy, damage, new Vector2f(), false));
        assertEquals(50f, damage.getFluxComponent(), 0.001f);

        for (Object engineListener : engineListeners) {
            ((HitDamageRestorer) engineListener).reportDamageApplied(null, enemy, mock(ApplyDamageResultAPI.class));
        }
        assertEquals(30f, damage.getFluxComponent(), 0.001f);
    }

    @Test
    void shieldHitsAndFriendlyTargetsGetNoExtraEmp() {
        DamageAPI damage = damage(200f, 0f);
        StrikecraftEmpListener listener = new StrikecraftEmpListener(ship(0), carrier(10f));

        listener.modifyDamageDealt(null, ship(1), damage, new Vector2f(), true);
        listener.modifyDamageDealt(null, ship(0), damage, new Vector2f(), false);

        verify(damage, never()).setFluxComponent(anyFloat());
    }
}
