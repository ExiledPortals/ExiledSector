package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhasedPeakTimeListenerTest {

    private static ShipAPI ship(float paused) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getMutableStats().getDynamic().getValue(PhasedPeakTimeListener.PAUSED_KEY, 0f)).thenReturn(paused);
        return ship;
    }

    @Test
    void timeDeployedHoldsAtItsValueOnPhaseEntryUntilTheShipLeavesPhase() {
        ShipAPI ship = ship(1f);
        PhasedPeakTimeListener listener = new PhasedPeakTimeListener(ship);
        when(ship.isPhased()).thenReturn(true);
        when(ship.getTimeDeployedForCRReduction()).thenReturn(30f);
        listener.advance(0.1f);

        when(ship.getTimeDeployedForCRReduction()).thenReturn(30.1f);
        listener.advance(0.1f);
        verify(ship).setTimeDeployed(30f);

        when(ship.isPhased()).thenReturn(false);
        when(ship.getTimeDeployedForCRReduction()).thenReturn(30.2f);
        listener.advance(0.1f);
        when(ship.isPhased()).thenReturn(true);
        listener.advance(0.1f);
        when(ship.getTimeDeployedForCRReduction()).thenReturn(30.3f);
        listener.advance(0.1f);
        verify(ship).setTimeDeployed(30.2f);
    }

    @Test
    void withoutTheEffectPhasingLeavesTheTimerRunning() {
        ShipAPI ship = ship(0f);
        PhasedPeakTimeListener listener = new PhasedPeakTimeListener(ship);
        when(ship.isPhased()).thenReturn(true);
        when(ship.getTimeDeployedForCRReduction()).thenReturn(30f, 30.1f);

        listener.advance(0.1f);
        listener.advance(0.1f);

        verify(ship, never()).setTimeDeployed(anyFloat());
    }
}
