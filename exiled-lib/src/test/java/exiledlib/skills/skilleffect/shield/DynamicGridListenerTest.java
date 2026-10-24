package exiledlib.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicGridListenerTest {

    private static ShipAPI ship(String id, int owner, Map<String, Float> magnitudes) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats()).thenReturn(stats);
        magnitudes.forEach((key, value) -> when(stats.getDynamic().getValue(key, 0f)).thenReturn(value));
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return ship;
    }

    @Test
    void dynamicGridWaitsForShieldsToStayDownThenDropsTheBonusWhenTheyRise() {
        ShipAPI ship = ship("grid", 0, Map.of(DynamicGridListener.ENERGY_BONUS_PERCENT_KEY, 15f));
        ShieldAPI shield = mock(ShieldAPI.class);
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(ship.getShield()).thenReturn(shield);
        when(ship.getFluxTracker()).thenReturn(flux);
        when(ship.getHullSize()).thenReturn(HullSize.CRUISER);
        DynamicGridListener grid = new DynamicGridListener(ship);
        String modId = "exiledLib_dynamicGrid_grid";

        grid.advance(1.5f);
        assertFalse(grid.bonusActive());
        grid.advance(0.6f);
        assertTrue(grid.bonusActive());
        verify(ship.getMutableStats().getEnergyRoFMult()).modifyPercent(modId, 15f);
        verify(ship.getMutableStats().getEnergyWeaponFluxCostMod()).modifyPercent(modId, -15f);

        when(shield.isOn()).thenReturn(true);
        grid.advance(0.1f);
        assertFalse(grid.bonusActive());
        verify(ship.getMutableStats().getEnergyRoFMult()).unmodify(modId);

        when(shield.isOn()).thenReturn(false);
        when(flux.isVenting()).thenReturn(true);
        grid.advance(5f);
        assertFalse(grid.bonusActive());
    }

    @Test
    void dynamicGridNeverTurnsOnForAShipWithoutShields() {
        ShipAPI ship = ship("bare", 0, Map.of(DynamicGridListener.ENERGY_BONUS_PERCENT_KEY, 15f));
        when(ship.getFluxTracker()).thenReturn(mock(FluxTrackerAPI.class));
        when(ship.getHullSize()).thenReturn(HullSize.FRIGATE);
        DynamicGridListener grid = new DynamicGridListener(ship);

        grid.advance(10f);

        assertFalse(grid.bonusActive());
    }

    @Test
    void dynamicGridKeepsItsPerHullSizeValuesAndFallbacks() {
        assertEquals(0.75f, DynamicGridListener.SHIELDS_DOWN_SECONDS.of(HullSize.FIGHTER));
        assertEquals(0.75f, DynamicGridListener.SHIELDS_DOWN_SECONDS.of(HullSize.FRIGATE));
        assertEquals(1.1f, DynamicGridListener.SHIELDS_DOWN_SECONDS.of(HullSize.DESTROYER));
        assertEquals(2f, DynamicGridListener.SHIELDS_DOWN_SECONDS.of(HullSize.CRUISER));
        assertEquals(3f, DynamicGridListener.SHIELDS_DOWN_SECONDS.of(HullSize.CAPITAL_SHIP));
        assertEquals(3f, DynamicGridListener.SHIELDS_DOWN_SECONDS.of(HullSize.DEFAULT));
    }
}
