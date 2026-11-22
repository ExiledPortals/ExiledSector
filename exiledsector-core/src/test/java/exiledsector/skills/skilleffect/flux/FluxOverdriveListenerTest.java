package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.AdditionalMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FluxOverdriveListenerTest {

    @Test
    void ballisticAndEnergyDamageAndFluxCostRiseWithFluxLevel() {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        ShipAPI ship = FluxListenersTestShips.ship("brawler", Map.of(FluxOverdriveListener.DAMAGE_PERCENT_KEY, 30f,
                FluxOverdriveListener.FLUX_COST_PERCENT_KEY, 30f), flux);
        MutableShipStatsAPI stats = ship.getMutableStats();
        FluxOverdriveListener listener = new FluxOverdriveListener(ship);
        String modId = "exiledSector_fluxOverdrive_brawler";

        when(flux.getFluxLevel()).thenReturn(0.5f);
        listener.advance(0.1f);
        verify(stats.getBallisticWeaponDamageMult()).modifyMult(org.mockito.ArgumentMatchers.eq(modId), eq(1.15f, 1e-5f));
        verify(stats.getEnergyWeaponDamageMult()).modifyMult(org.mockito.ArgumentMatchers.eq(modId), eq(1.15f, 1e-5f));
        verify(stats.getBallisticWeaponFluxCostMod()).modifyMult(org.mockito.ArgumentMatchers.eq(modId), eq(1.15f, 1e-5f));
        verify(stats.getEnergyWeaponFluxCostMod()).modifyMult(org.mockito.ArgumentMatchers.eq(modId), eq(1.15f, 1e-5f));

        when(flux.getFluxLevel()).thenReturn(0f);
        listener.advance(0.1f);
        verify(stats.getBallisticWeaponDamageMult()).unmodify(modId);
        verify(stats.getEnergyWeaponFluxCostMod()).unmodify(modId);
    }
}
