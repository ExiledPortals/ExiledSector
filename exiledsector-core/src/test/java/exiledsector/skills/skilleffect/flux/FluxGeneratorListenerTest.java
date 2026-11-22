package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FluxGeneratorListenerTest {

    private static ShipAPI generatorShip(FluxTrackerAPI flux) {
        when(flux.getMaxFlux()).thenReturn(1000f);
        ShipAPI ship = FluxListenersTestShips.ship("dynamo", Map.of(FluxGeneratorListener.FLUX_LEVEL_PERCENT_KEY, 50f), flux);
        when(ship.getMutableStats().getFluxDissipation().getModifiedValue()).thenReturn(100f);
        return ship;
    }

    @Test
    void fluxIsGeneratedAtTwiceDissipationUpToTheCap() {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        FluxGeneratorListener listener = new FluxGeneratorListener(generatorShip(flux));

        when(flux.getCurrFlux()).thenReturn(100f);
        listener.advance(1f);
        verify(flux).setCurrFlux(300f);

        when(flux.getCurrFlux()).thenReturn(450f);
        listener.advance(1f);
        verify(flux).setCurrFlux(500f);
    }

    @Test
    void nothingIsGeneratedWhileVentingOrAtTheCap() {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        FluxGeneratorListener listener = new FluxGeneratorListener(generatorShip(flux));

        when(flux.getCurrFlux()).thenReturn(100f);
        when(flux.isVenting()).thenReturn(true);
        listener.advance(1f);
        when(flux.isVenting()).thenReturn(false);
        when(flux.getCurrFlux()).thenReturn(500f);
        listener.advance(1f);

        verify(flux, never()).setCurrFlux(anyFloat());
    }
}
