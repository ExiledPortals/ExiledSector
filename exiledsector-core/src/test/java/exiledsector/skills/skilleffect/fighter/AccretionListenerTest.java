package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccretionListenerTest {

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

    private void at(float seconds) {
        when(engine.getTotalElapsedTime(false)).thenReturn(seconds);
    }

    private static ShipAPI carrier(float percentPerStack) {
        ShipAPI carrier = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        Map<String, Object> customData = new HashMap<>();
        when(carrier.getCustomData()).thenReturn(customData);
        doAnswer(call -> customData.put(call.getArgument(0), call.getArgument(1))).when(carrier).setCustomData(anyString(), any());
        when(carrier.getMutableStats().getDynamic().getValue(AccretionListener.DAMAGE_PERCENT_PER_STACK_KEY, 0f)).thenReturn(percentPerStack);
        return carrier;
    }

    private static ShipAPI fighter(float hitpoints) {
        ShipAPI fighter = mock(ShipAPI.class);
        when(fighter.getHitpoints()).thenReturn(hitpoints);
        return fighter;
    }

    private static DamageAPI damage() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(mock(MutableStat.class));
        return damage;
    }

    @Test
    void stacksExpireOnTheirOwnAndCapAtTheMaximum() {
        AccretionStacks stacks = new AccretionStacks();
        stacks.add(0f);
        stacks.add(10f);

        assertEquals(2, stacks.activeAt(19f));
        assertEquals(1, stacks.activeAt(AccretionStacks.STACK_SECONDS));
        assertEquals(0, stacks.activeAt(10f + AccretionStacks.STACK_SECONDS));
        for (int i = 0; i < AccretionStacks.MAX_STACKS + 5; i++) {
            stacks.add(40f);
        }
        assertEquals(AccretionStacks.MAX_STACKS, stacks.activeAt(41f));
    }

    @Test
    void eachLostStrikecraftAddsOneStackThatEveryStrikecraftOfTheCarrierUses() {
        ShipAPI carrier = carrier(1f);
        ShipAPI destroyed = fighter(0f);
        AccretionListener lostFighter = new AccretionListener(destroyed, carrier);
        AccretionListener survivor = new AccretionListener(fighter(10f), carrier);
        at(5f);

        lostFighter.reportDamageApplied(null, destroyed, mock(ApplyDamageResultAPI.class));
        lostFighter.reportDamageApplied(null, destroyed, mock(ApplyDamageResultAPI.class));
        DamageAPI hit = damage();
        String modId = survivor.modifyDamageDealt(null, mock(ShipAPI.class), hit, new Vector2f(), false);

        assertSame(AccretionStacks.of(carrier), AccretionStacks.of(carrier));
        assertEquals(1, AccretionStacks.of(carrier).activeAt(5f));
        verify(hit.getModifier()).modifyPercent(modId, 1f);
    }

    @Test
    void noStacksOrNoEffectLeavesTheDamageAlone() {
        ShipAPI carrier = carrier(1f);
        at(5f);

        assertNull(new AccretionListener(fighter(10f), carrier).modifyDamageDealt(null, mock(ShipAPI.class), damage(), new Vector2f(), false));
        ShipAPI destroyed = fighter(0f);
        ShipAPI withoutEffect = carrier(0f);
        new AccretionListener(destroyed, withoutEffect).reportDamageApplied(null, destroyed, mock(ApplyDamageResultAPI.class));
        assertNull(new AccretionListener(fighter(10f), withoutEffect).modifyDamageDealt(null, mock(ShipAPI.class), damage(), new Vector2f(), false));
    }
}
