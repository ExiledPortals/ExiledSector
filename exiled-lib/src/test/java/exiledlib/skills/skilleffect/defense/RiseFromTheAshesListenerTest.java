package exiledlib.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiseFromTheAshesListenerTest {

    private static ShipAPI ship(String id, Map<String, Float> magnitudes) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getMutableStats()).thenReturn(stats);
        magnitudes.forEach((key, value) -> when(stats.getDynamic().getValue(key, 0f)).thenReturn(value));
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return ship;
    }

    @Test
    void riseFromTheAshesCancelsOneLethalHitPerShipPerBattle() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        Map<String, Object> customData = new HashMap<>();
        when(engine.getCustomData()).thenReturn(customData);
        ShipAPI ship = ship("sandgrouse", Map.of(RiseFromTheAshesListener.HULL_PERCENT_KEY, 25f,
                RiseFromTheAshesListener.DAMAGE_TAKEN_PERCENT_KEY, -50f));
        when(ship.getFleetMemberId()).thenReturn("member_7");
        when(ship.getMaxHitpoints()).thenReturn(2000f);
        when(ship.getHitpoints()).thenReturn(100f);
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);
            RiseFromTheAshesListener listener = new RiseFromTheAshesListener(ship);

            assertTrue(listener.saveFromLethalHit(ship, 300f));
            verify(ship).setHitpoints(500f);
            verify(ship.getMutableStats().getHullDamageTakenMult()).modifyMult("exiledLib_riseFromTheAshes_sandgrouse", 0.5f);
            assertTrue(listener.rising());
            assertTrue(customData.containsKey("exiledLib_riseFromTheAshesUsed_member_7"));

            assertFalse(new RiseFromTheAshesListener(ship).saveFromLethalHit(ship, 300f));

            listener.advance(RiseFromTheAshesListener.DURATION_SECONDS + 0.1f);
            assertFalse(listener.rising());
            verify(ship.getMutableStats().getHullDamageTakenMult()).unmodify("exiledLib_riseFromTheAshes_sandgrouse");
            verify(ship, never()).setJitterUnder(Mockito.any(), Mockito.any(), anyFloat(), anyInt(), anyFloat(), anyFloat());
        }
    }
}
