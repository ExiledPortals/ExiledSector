package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import exiledsector.skills.skilleffect.CombatTestShips;
import org.lwjgl.util.vector.Vector2f;

import java.util.Map;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class FluxListenersTestShips {

    private FluxListenersTestShips() {
    }

    static ShipAPI ship(String id, Map<String, Float> magnitudes, FluxTrackerAPI flux) {
        ShipAPI ship = CombatTestShips.ship(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS)).id(id).dynamic(magnitudes).alive()
                .at(new Vector2f()).build();
        when(ship.getFluxTracker()).thenReturn(flux);
        when(ship.getEngineController()).thenReturn(mock(ShipEngineControllerAPI.class));
        return ship;
    }
}
