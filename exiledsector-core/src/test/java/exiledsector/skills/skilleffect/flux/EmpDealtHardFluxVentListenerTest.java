package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmpDealtHardFluxVentListenerTest {

    private static ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getOwner()).thenReturn(owner);
        return ship;
    }

    private static ShipAPI dealer(FluxTrackerAPI flux) {
        ShipAPI dealer = ship(0);
        when(dealer.getFluxTracker()).thenReturn(flux);
        when(dealer.getMutableStats().getDynamic().getValue(EmpDealtHardFluxVentListener.VENT_PERCENT_KEY, 0f)).thenReturn(25f);
        return dealer;
    }

    private static FluxTrackerAPI flux() {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(flux.getCurrFlux()).thenReturn(1000f);
        when(flux.getHardFlux()).thenReturn(400f);
        return flux;
    }

    @Test
    void empDealtToAnEnemyVentsItsShareOfTheDealersHardFlux() {
        FluxTrackerAPI flux = flux();

        new EmpDealtHardFluxVentListener(dealer(flux)).onEmpDealt(ship(1), 800f);

        verify(flux).setHardFlux(200f);
        verify(flux).setCurrFlux(800f);
    }

    @Test
    void empDealtToAlliesOrWrecksVentsNothing() {
        FluxTrackerAPI flux = flux();
        EmpDealtHardFluxVentListener listener = new EmpDealtHardFluxVentListener(dealer(flux));
        ShipAPI wreck = ship(1);
        when(wreck.isHulk()).thenReturn(true);

        listener.onEmpDealt(ship(0), 800f);
        listener.onEmpDealt(wreck, 800f);

        verify(flux, never()).setHardFlux(anyFloat());
    }
}
