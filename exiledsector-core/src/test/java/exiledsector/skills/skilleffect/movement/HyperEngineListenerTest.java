package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import exiledsector.skills.skilleffect.CombatTestShips;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalMatchers.eq;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HyperEngineListenerTest {

    private static ShipAPI ship(FluxTrackerAPI flux, float topSpeed) {
        ShipAPI ship = CombatTestShips.ship(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS)).id("courier")
                .dynamic(Map.of(HyperEngineListener.SPEED_FLAT_KEY, 20f)).alive().at(new Vector2f()).build();
        when(ship.getFluxTracker()).thenReturn(flux);
        when(ship.getEngineController()).thenReturn(mock(ShipEngineControllerAPI.class));
        when(ship.getMutableStats().getZeroFluxSpeedBoost().getModifiedValue()).thenReturn(50f);
        when(ship.getMutableStats().getMaxSpeed().getModifiedValue()).thenReturn(topSpeed);
        return ship;
    }

    @Test
    void slowerShipsGainMoreZeroFluxSpeed() {
        assertEquals(60f, HyperEngineListener.speedBonus(20f, 50f, 30f), 1e-4f);
        assertEquals(0f, HyperEngineListener.speedBonus(20f, 50f, 300f));
    }

    @Test
    void theBonusAppliesOnlyWhileTheBoostRunsAtLowFlux() {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        ShipAPI ship = ship(flux, 90f);
        HyperEngineListener listener = new HyperEngineListener(ship);
        String modId = "exiledSector_hyperEngine_courier";

        when(flux.isEngineBoostActive()).thenReturn(true);
        when(flux.getFluxLevel()).thenReturn(0.2f);
        listener.advance(0.1f);
        verify(ship.getMutableStats().getZeroFluxSpeedBoost(), never()).modifyFlat(anyString(), anyFloat());

        when(flux.getFluxLevel()).thenReturn(0.03f);
        listener.advance(0.1f);
        verify(ship.getMutableStats().getZeroFluxSpeedBoost()).modifyFlat(org.mockito.ArgumentMatchers.eq(modId), eq(40f, 1e-4f));

        when(flux.isEngineBoostActive()).thenReturn(false);
        listener.advance(0.1f);
        verify(ship.getMutableStats().getZeroFluxSpeedBoost()).unmodify(modId);
    }

    @Test
    void fastShipsGainNothing() {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        ShipAPI ship = ship(flux, 300f);
        when(flux.isEngineBoostActive()).thenReturn(true);
        new HyperEngineListener(ship).advance(0.1f);

        verify(ship.getMutableStats().getZeroFluxSpeedBoost(), never()).modifyFlat(anyString(), anyFloat());
    }

    @Test
    void theTopSpeedIsReadOnlyOnceTheTravelDriveIsOff() {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        ShipAPI ship = ship(flux, 690f);
        ShipSystemAPI travelDrive = mock(ShipSystemAPI.class);
        when(ship.getTravelDrive()).thenReturn(travelDrive);
        when(travelDrive.isOn()).thenReturn(true);
        when(flux.isEngineBoostActive()).thenReturn(true);
        HyperEngineListener listener = new HyperEngineListener(ship);
        listener.advance(0.1f);

        when(travelDrive.isOn()).thenReturn(false);
        when(ship.getMutableStats().getMaxSpeed().getModifiedValue()).thenReturn(90f);
        listener.advance(0.1f);

        verify(ship.getMutableStats().getZeroFluxSpeedBoost()).modifyFlat(org.mockito.ArgumentMatchers.eq("exiledSector_hyperEngine_courier"), eq(40f, 1e-4f));
    }
}
