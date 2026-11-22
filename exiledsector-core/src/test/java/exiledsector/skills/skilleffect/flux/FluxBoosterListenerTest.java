package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FluxBoosterListenerTest {

    @Test
    void ventingBoostsSpeedByTheFluxLevelTheVentStartedAtForItsSeconds() {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        ShipAPI ship = FluxListenersTestShips.ship("booster", Map.of(FluxBoosterListener.MANEUVERABILITY_PERCENT_KEY, 100f,
                FluxBoosterListener.SECONDS_KEY, 3f), flux);
        MutableShipStatsAPI stats = ship.getMutableStats();
        FluxBoosterListener listener = new FluxBoosterListener(ship);
        String modId = "exiledSector_fluxBooster_booster";

        when(flux.getFluxLevel()).thenReturn(0.8f);
        listener.advance(0.1f);
        assertFalse(listener.boosting());

        when(flux.isVenting()).thenReturn(true);
        when(flux.getCurrFlux()).thenReturn(500f);
        when(flux.getFluxLevel()).thenReturn(0.5f);
        listener.advance(0.1f);
        assertTrue(listener.boosting());
        verify(stats.getMaxSpeed()).modifyPercent(modId, 80f);
        verify(stats.getMaxTurnRate()).modifyPercent(modId, 80f);
        verify(stats.getAcceleration()).modifyPercent(modId, 160f);
        verify(stats.getTurnAcceleration()).modifyPercent(modId, 160f);

        listener.advance(3f);
        listener.advance(0.1f);
        assertFalse(listener.boosting());
        verify(stats.getMaxSpeed()).unmodify(modId);
    }
}
