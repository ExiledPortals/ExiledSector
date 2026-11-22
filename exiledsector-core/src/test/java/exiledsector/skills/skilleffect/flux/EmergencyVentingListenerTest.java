package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmergencyVentingListenerTest {

    private static ShipAPI ship(FluxTrackerAPI flux, float ventPercent) {
        ShipAPI ship = FluxListenersTestShips.ship("vent", Map.of(EmergencyVentingListener.OVERLOAD_SECONDS_KEY, 2f,
                EmergencyVentingListener.VENT_RATE_PERCENT_KEY, ventPercent), flux);
        when(ship.getMutableStats().getVentRateMult().getModifiedValue()).thenReturn(1f);
        return ship;
    }

    @Test
    void longOverloadsAreCutThenTheShipVentsWithItsBonusUntilTheVentEnds() {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        ShipAPI ship = ship(flux, 40f);
        EmergencyVentingListener listener = new EmergencyVentingListener(ship);
        String modId = "exiledSector_emergencyVenting_vent";

        when(flux.isOverloaded()).thenReturn(true);
        when(flux.isOverloadedOrVenting()).thenReturn(true);
        when(flux.getOverloadTimeRemaining()).thenReturn(8f);
        listener.advance(0.1f);
        verify(flux).stopOverload();
        verify(flux).beginOverloadWithTotalBaseDuration(2f);
        listener.advance(0.1f);
        verify(flux, times(1)).stopOverload();

        when(flux.isOverloaded()).thenReturn(false);
        when(flux.isOverloadedOrVenting()).thenReturn(false);
        listener.advance(0.1f);
        verify(ship).giveCommand(ShipCommand.VENT_FLUX, null, 0);

        when(flux.isVenting()).thenReturn(true);
        listener.advance(0.1f);
        assertTrue(listener.ventBoosted());
        verify(ship.getMutableStats().getVentRateMult()).modifyPercent(modId, 40f);

        when(flux.isVenting()).thenReturn(false);
        listener.advance(0.1f);
        assertFalse(listener.ventBoosted());
        verify(ship.getMutableStats().getVentRateMult()).unmodify(modId);
    }

    @Test
    void shortOverloadsAndShipsThatCannotVentAreLeftAlone() {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        ShipAPI ship = ship(flux, 0f);
        EmergencyVentingListener listener = new EmergencyVentingListener(ship);
        when(flux.isOverloaded()).thenReturn(true);
        when(flux.getOverloadTimeRemaining()).thenReturn(1.5f);
        listener.advance(0.1f);

        when(flux.getOverloadTimeRemaining()).thenReturn(8f);
        when(ship.getMutableStats().getVentRateMult().getModifiedValue()).thenReturn(0f);
        listener.advance(0.1f);

        verify(flux, never()).stopOverload();
        verify(flux, never()).beginOverloadWithTotalBaseDuration(anyFloat());
    }

    @Test
    void aVentThatNeverStartsStopsWaiting() {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        ShipAPI ship = ship(flux, 40f);
        EmergencyVentingListener listener = new EmergencyVentingListener(ship);
        when(flux.isOverloaded()).thenReturn(true);
        when(flux.getOverloadTimeRemaining()).thenReturn(8f);
        listener.advance(0.1f);
        when(flux.isOverloaded()).thenReturn(false);
        listener.advance(0.1f);
        listener.advance(EmergencyVentingListener.VENT_START_TIMEOUT_SECONDS);

        when(flux.isVenting()).thenReturn(true);
        listener.advance(0.1f);

        assertFalse(listener.ventBoosted());
    }
}
