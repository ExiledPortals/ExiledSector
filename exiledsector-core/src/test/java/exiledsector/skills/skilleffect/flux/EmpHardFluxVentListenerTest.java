package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmpHardFluxVentListenerTest {

    private static FluxTrackerAPI flux(float currentFlux, float hardFlux) {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        float[] levels = {currentFlux, hardFlux};
        when(flux.getCurrFlux()).thenAnswer(call -> levels[0]);
        when(flux.getHardFlux()).thenAnswer(call -> levels[1]);
        doAnswer(call -> levels[0] = call.getArgument(0)).when(flux).setCurrFlux(anyFloat());
        doAnswer(call -> levels[1] = call.getArgument(0)).when(flux).setHardFlux(anyFloat());
        return flux;
    }

    private static ApplyDamageResultAPI empHit(float emp) {
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getEmpDamage()).thenReturn(emp);
        return result;
    }

    @Test
    void empDamageTakenVentsItsShareFromHardFluxOnly() {
        FluxTrackerAPI flux = flux(1000f, 400f);
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getFluxTracker()).thenReturn(flux);
        when(ship.getMutableStats().getDynamic().getValue(EmpHardFluxVentListener.VENT_PERCENT_KEY, 0f)).thenReturn(25f);

        new EmpHardFluxVentListener(ship).reportDamageApplied(null, ship, empHit(800f));

        assertEquals(200f, flux.getHardFlux(), 0.001f);
        assertEquals(800f, flux.getCurrFlux(), 0.001f);
    }

    @Test
    void ventingStopsWhenHardFluxRunsOut() {
        FluxTrackerAPI flux = flux(500f, 50f);

        assertEquals(50f, EmpHardFluxVentListener.ventHardFlux(flux, 200f), 0.001f);
        assertEquals(0f, flux.getHardFlux(), 0.001f);
        assertEquals(450f, flux.getCurrFlux(), 0.001f);
        assertEquals(0f, EmpHardFluxVentListener.ventHardFlux(flux, 200f));
    }

    @Test
    void hitsWithoutEmpLeaveFluxAlone() {
        FluxTrackerAPI flux = flux(500f, 300f);
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getFluxTracker()).thenReturn(flux);

        new EmpHardFluxVentListener(ship).reportDamageApplied(null, ship, empHit(0f));

        verify(flux, never()).setHardFlux(anyFloat());
    }
}
