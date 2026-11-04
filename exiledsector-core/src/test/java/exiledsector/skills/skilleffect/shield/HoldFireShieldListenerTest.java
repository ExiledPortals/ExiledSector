package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalMatchers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HoldFireShieldListenerTest {

    private static final String MOD_ID = "exiledSector_holdFireShield_fortress";

    private static ShipAPI ship(WeaponAPI weapon) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn("fortress");
        when(ship.isAlive()).thenReturn(true);
        when(ship.getAllWeapons()).thenReturn(List.of(weapon));
        when(ship.getMutableStats().getDynamic().getValue(HoldFireShieldListener.REDUCTION_PERCENT_KEY, 0f)).thenReturn(10f);
        return ship;
    }

    private static void advanceSeconds(HoldFireShieldListener listener, float seconds) {
        for (float elapsed = 0f; elapsed < seconds - 0.001f; elapsed += HoldFireShieldListener.CHECK_INTERVAL_SECONDS) {
            listener.advance(HoldFireShieldListener.CHECK_INTERVAL_SECONDS);
        }
    }

    @Test
    void theLevelClimbsAndFallsOverTheTaperBothWays() {
        assertEquals(0.5f, HoldFireShieldListener.nextHoldFireLevel(0f, true, 1.5f), 0.0001f);
        assertEquals(1f, HoldFireShieldListener.nextHoldFireLevel(0.9f, true, 1f));
        assertEquals(0.5f, HoldFireShieldListener.nextHoldFireLevel(1f, false, 1.5f), 0.0001f);
        assertEquals(0f, HoldFireShieldListener.nextHoldFireLevel(0.2f, false, 1f));
    }

    @Test
    void holdingFireReachesTheFullReductionAndFiringTakesItAway() {
        WeaponAPI weapon = mock(WeaponAPI.class);
        ShipAPI ship = ship(weapon);
        HoldFireShieldListener listener = new HoldFireShieldListener(ship);

        advanceSeconds(listener, HoldFireShieldListener.TAPER_SECONDS);
        assertEquals(1f, listener.holdFireLevel(), 0.001f);
        verify(ship.getMutableStats().getShieldDamageTakenMult(), atLeastOnce()).modifyPercent(eq(MOD_ID), AdditionalMatchers.eq(-10f, 0.01f));

        when(weapon.isFiring()).thenReturn(true);
        advanceSeconds(listener, HoldFireShieldListener.TAPER_SECONDS / 2f);
        assertEquals(0.5f, listener.holdFireLevel(), 0.001f);
        advanceSeconds(listener, HoldFireShieldListener.TAPER_SECONDS);
        assertEquals(0f, listener.holdFireLevel());
        verify(ship.getMutableStats().getShieldDamageTakenMult()).unmodify(MOD_ID);
    }

    @Test
    void decorativeWeaponsNeverCountAsFiring() {
        WeaponAPI decoration = mock(WeaponAPI.class);
        when(decoration.isDecorative()).thenReturn(true);
        when(decoration.isFiring()).thenReturn(true);
        HoldFireShieldListener listener = new HoldFireShieldListener(ship(decoration));

        advanceSeconds(listener, 1.5f);

        assertEquals(0.5f, listener.holdFireLevel(), 0.001f);
    }
}
