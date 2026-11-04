package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
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
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(PhaseEntryFluxShedListener.SHED_PERCENT_KEY, 0f)).thenReturn(20f);
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
