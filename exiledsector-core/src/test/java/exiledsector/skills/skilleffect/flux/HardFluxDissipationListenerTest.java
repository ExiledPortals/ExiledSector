package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HardFluxDissipationListenerTest {

    private static FluxTrackerAPI flux(float hardFlux, float maxFlux) {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(flux.getHardFlux()).thenReturn(hardFlux);
        when(flux.getMaxFlux()).thenReturn(maxFlux);
        return flux;
    }

    @Test
    void everyWholeFivePercentOfHardFluxIsOneStep() {
        assertEquals(0, HardFluxDissipationListener.hardFluxSteps(flux(49f, 1000f)));
        assertEquals(1, HardFluxDissipationListener.hardFluxSteps(flux(50f, 1000f)));
        assertEquals(9, HardFluxDissipationListener.hardFluxSteps(flux(499f, 1000f)));
        assertEquals(20, HardFluxDissipationListener.hardFluxSteps(flux(1000f, 1000f)));
        assertEquals(0, HardFluxDissipationListener.hardFluxSteps(flux(100f, 0f)));
    }

    @Test
    void dissipationRisesWithHardFluxAndFallsBackWhenItDrains() {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        MutableStat dissipation = new MutableStat(100f);
        when(ship.getId()).thenReturn("molten");
        when(ship.isAlive()).thenReturn(true);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(stats.getFluxDissipation()).thenReturn(dissipation);
        when(dynamic.getValue(HardFluxDissipationListener.PERCENT_PER_STEP_KEY, 0f)).thenReturn(1f);
        HardFluxDissipationListener listener = new HardFluxDissipationListener(ship);

        FluxTrackerAPI halfHard = flux(500f, 1000f);
        when(ship.getFluxTracker()).thenReturn(halfHard);
        listener.advance(0.1f);
        assertEquals(110f, dissipation.getModifiedValue(), 0.001f);

        FluxTrackerAPI drained = flux(0f, 1000f);
        when(ship.getFluxTracker()).thenReturn(drained);
        listener.advance(0.1f);
        assertEquals(100f, dissipation.getModifiedValue(), 0.001f);
    }
}
