package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatTestShips;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnstableShieldsListenerTest {

    private static ShipAPI ship(ShieldAPI shield, Map<String, Float> magnitudes) {
        ShipAPI ship = CombatTestShips.ship(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS)).id("aegis").dynamic(magnitudes).alive()
                .at(new Vector2f()).build();
        when(ship.getShield()).thenReturn(shield);
        when(shield.getInnerColor()).thenReturn(new Color(0, 100, 255, 100));
        return ship;
    }

    @Test
    void chargedShieldsTakeLessDamageAndFullyUnstableOnesTakeMore() {
        assertEquals(0.7f, UnstableShieldsListener.damageTakenMult(-30f, 0f), 1e-5f);
        assertEquals(1.3f, UnstableShieldsListener.damageTakenMult(-30f, 1f), 1e-5f);
        assertEquals(0.6f, UnstableShieldsListener.damageTakenMult(-40f, 0f), 1e-5f);
        assertEquals(1.2f, UnstableShieldsListener.damageTakenMult(-40f, 1f), 1e-5f);
    }

    @Test
    void instabilityBuildsAfterTheHoldAndFadesAfterTheDelay() {
        ShieldAPI shield = mock(ShieldAPI.class);
        ShipAPI ship = ship(shield, Map.of(UnstableShieldsListener.DAMAGE_TAKEN_PERCENT_KEY, -30f));
        UnstableShieldsListener listener = new UnstableShieldsListener(ship);

        when(shield.isOn()).thenReturn(true);
        listener.advance(3f);
        assertEquals(0f, listener.instability());
        listener.advance(4.5f);
        assertEquals(0.5f, listener.instability(), 1e-5f);

        when(shield.isOn()).thenReturn(false);
        when(shield.isOff()).thenReturn(true);
        listener.advance(3f);
        assertEquals(0.5f, listener.instability(), 1e-5f);
        listener.advance(1.5f);
        assertEquals(0.25f, listener.instability(), 1e-5f);
        verify(ship.getMutableStats().getShieldDamageTakenMult()).modifyMult(org.mockito.ArgumentMatchers.eq("exiledSector_unstableShields_aegis"), eq(0.85f, 1e-5f));
    }

    @Test
    void steadyShieldsHoldLongerAndRecoverSooner() {
        ShieldAPI shield = mock(ShieldAPI.class);
        ShipAPI ship = ship(shield, Map.of(UnstableShieldsListener.DAMAGE_TAKEN_PERCENT_KEY, -40f, UnstableShieldsListener.STEADY_FLAG_KEY, 1f));
        UnstableShieldsListener listener = new UnstableShieldsListener(ship);

        when(shield.isOn()).thenReturn(true);
        listener.advance(4f);
        assertEquals(0f, listener.instability());
        listener.advance(6f);
        assertEquals(0.5f, listener.instability(), 1e-5f);

        when(shield.isOn()).thenReturn(false);
        when(shield.isOff()).thenReturn(true);
        listener.advance(2f);
        listener.advance(1f);
        assertEquals(0.25f, listener.instability(), 1e-5f);
    }
}
