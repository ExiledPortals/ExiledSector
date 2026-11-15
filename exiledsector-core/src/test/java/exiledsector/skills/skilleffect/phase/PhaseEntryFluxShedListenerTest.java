package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatTestShips;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhaseEntryFluxShedListenerTest {

    @Test
    void enteringPhaseTurnsItsShareOfHardFluxSoftOncePerEntry() {
        ShipAPI ship = CombatTestShips.withDynamic(PhaseEntryFluxShedListener.SHED_PERCENT_KEY, 20f);
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(ship.getFluxTracker()).thenReturn(flux);
        when(flux.getHardFlux()).thenReturn(500f);
        PhaseEntryFluxShedListener listener = new PhaseEntryFluxShedListener(ship);

        listener.advance(0.1f);
        verify(flux, never()).setHardFlux(anyFloat());

        when(ship.isPhased()).thenReturn(true);
        listener.advance(0.1f);
        listener.advance(0.1f);
        verify(flux, times(1)).setHardFlux(400f);
        verify(flux, never()).setCurrFlux(anyFloat());
    }

    @Test
    void noHardFluxMeansNothingToConvert() {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);

        PhaseEntryFluxShedListener.softenHardFlux(flux, 20f);

        verify(flux, never()).setHardFlux(anyFloat());
    }
}
