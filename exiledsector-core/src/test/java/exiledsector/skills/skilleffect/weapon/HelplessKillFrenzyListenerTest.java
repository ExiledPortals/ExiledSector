package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.KillWatch;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HelplessKillFrenzyListenerTest {

    private static ShipAPI ship(String id, int owner, boolean overloaded) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.isHulk()).thenReturn(false);
        when(ship.isFighter()).thenReturn(false);
        when(ship.getParentStation()).thenReturn(null);
        when(ship.getHitpoints()).thenReturn(1000f);
        when(ship.getFluxTracker().isOverloaded()).thenReturn(overloaded);
        when(ship.getFluxTracker().isVenting()).thenReturn(false);
        when(ship.getEngineController().isFlamedOut()).thenReturn(false);
        when(ship.getListeners(any())).thenReturn(List.of());
        return ship;
    }

    private static ShipAPI raider(float stacksPerKill) {
        ShipAPI raider = ship("raider", 0, false);
        when(raider.getMutableStats().getDynamic().getValue(HelplessKillFrenzyListener.STACKS_KEY, 0f)).thenReturn(stacksPerKill);
        when(raider.getMutableStats().getDynamic().getValue(PirateFrenzy.EXTRA_SECONDS_KEY, 0f)).thenReturn(0f);
        return raider;
    }

    private static <T> T addedListener(ShipAPI ship, Class<T> type) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ship).addListener(captor.capture());
        return type.cast(captor.getValue());
    }

    @Test
    void destroyingAHelplessShipGrantsPirateFrenzyStacks() {
        ShipAPI raider = raider(2f);
        HelplessKillFrenzyListener listener = new HelplessKillFrenzyListener(raider);
        PirateFrenzy frenzy = addedListener(raider, PirateFrenzy.class);
        when(raider.getListeners(KillWatch.KillReaction.class)).thenReturn(List.of(listener));
        ShipAPI victim = ship("victim", 1, true);

        listener.modifyDamageDealt(null, victim, mock(DamageAPI.class), new Vector2f(), false);
        KillWatch watch = addedListener(victim, KillWatch.class);
        when(victim.getHitpoints()).thenReturn(0f);
        watch.reportDamageApplied(raider, victim, null);

        assertEquals(2, frenzy.stacks());
    }

    @Test
    void readyShipsAreNotWatchedAndGrantNoStacks() {
        ShipAPI raider = raider(1f);
        HelplessKillFrenzyListener listener = new HelplessKillFrenzyListener(raider);
        PirateFrenzy frenzy = addedListener(raider, PirateFrenzy.class);
        ShipAPI ready = ship("ready", 1, false);

        listener.modifyDamageDealt(null, ready, mock(DamageAPI.class), new Vector2f(), false);
        listener.onKill(ready);

        verify(ready, never()).addListener(any());
        assertEquals(0, frenzy.stacks());
    }
}
