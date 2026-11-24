package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;

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
    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = combat.engine();
        CombatListenerManagerAPI listenerManager = mock(CombatListenerManagerAPI.class);
        when(engine.getListenerManager()).thenReturn(listenerManager);
        when(listenerManager.getListeners(any())).thenAnswer(call -> {
            Class<?> listenerType = call.getArgument(0);
            return engineListeners.stream().filter(listenerType::isInstance).toList();
        });
        doAnswer(call -> engineListeners.add(call.getArgument(0))).when(listenerManager).addListener(any());
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

    private static DamageAPI damage(float baseDamage, float modifiedDamage, float emp) {
        DamageAPI damage = mock(DamageAPI.class);
        float[] empComponent = {emp};
        when(damage.getType()).thenReturn(DamageType.KINETIC);
        when(damage.getDamage()).thenReturn(modifiedDamage);
        when(damage.getBaseDamage()).thenReturn(baseDamage);
        when(damage.getFluxComponent()).thenAnswer(call -> empComponent[0]);
        doAnswer(call -> empComponent[0] = call.getArgument(0)).when(damage).setFluxComponent(anyFloat());
        return damage;
    }

    @Test
    void hullHitsOnEnemiesCarryExtraEmpFromTheirBaseDamageForThatHitOnly() {
        ShipAPI enemy = ship(1);
        DamageAPI damage = damage(200f, 400f, 30f);
        StrikecraftEmpListener listener = new StrikecraftEmpListener(ship(0), carrier(10f));

        assertNull(listener.modifyDamageDealt(null, enemy, damage, new Vector2f(), false));
        assertEquals(50f, damage.getFluxComponent(), 0.001f);

        for (Object engineListener : engineListeners) {
            if (engineListener instanceof HitDamageRestorer restorer) {
                restorer.reportDamageApplied(null, enemy, mock(ApplyDamageResultAPI.class));
            }
        }
        assertEquals(30f, damage.getFluxComponent(), 0.001f);
    }

    @Test
    void shieldHitsAndFriendlyTargetsGetNoExtraEmp() {
        DamageAPI damage = damage(200f, 200f, 0f);
        StrikecraftEmpListener listener = new StrikecraftEmpListener(ship(0), carrier(10f));

        listener.modifyDamageDealt(null, ship(1), damage, new Vector2f(), true);
        listener.modifyDamageDealt(null, ship(0), damage, new Vector2f(), false);

        verify(damage, never()).setFluxComponent(anyFloat());
    }
}
