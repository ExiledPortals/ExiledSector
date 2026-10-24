package exiledlib.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThermoAcceleratorListenerTest {

    private static ShipAPI ship(String id, Map<String, Float> magnitudes) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getMutableStats()).thenReturn(stats);
        magnitudes.forEach((key, value) -> when(stats.getDynamic().getValue(key, 0f)).thenReturn(value));
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return ship;
    }

    private static WeaponAPI weapon(boolean hardpoint, boolean decorative, boolean systemSlot) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        WeaponSlotAPI slot = mock(WeaponSlotAPI.class);
        when(slot.isHardpoint()).thenReturn(hardpoint);
        when(slot.isSystemSlot()).thenReturn(systemSlot);
        when(weapon.getSlot()).thenReturn(slot);
        when(weapon.isDecorative()).thenReturn(decorative);
        return weapon;
    }

    @Test
    void thermoAcceleratorCutsWeaponFluxCostAndKeepsOnlyRealHardpointsFromFiring() {
        WeaponAPI hardpoint = weapon(true, false, false);
        WeaponAPI turret = weapon(false, false, false);
        WeaponAPI decorative = weapon(true, true, false);
        WeaponAPI system = weapon(true, false, true);
        ShipAPI ship = ship("aetos", Map.of(ThermoAcceleratorListener.FLUX_COST_REDUCTION_PERCENT_KEY, 50f));
        when(ship.getAllWeapons()).thenReturn(List.of(hardpoint, turret, decorative, system));

        ThermoAcceleratorListener listener = new ThermoAcceleratorListener(ship);
        listener.advance(0.1f);

        String modId = "exiledLib_thermoAccelerator_aetos";
        verify(ship.getMutableStats().getBallisticWeaponFluxCostMod()).modifyPercent(modId, -50f);
        verify(ship.getMutableStats().getEnergyWeaponFluxCostMod()).modifyPercent(modId, -50f);
        verify(ship.getMutableStats().getMissileWeaponFluxCostMod()).modifyPercent(modId, -50f);
        verify(hardpoint).setForceNoFireOneFrame(true);
        verify(turret, never()).setForceNoFireOneFrame(true);
        verify(decorative, never()).setForceNoFireOneFrame(true);
        verify(system, never()).setForceNoFireOneFrame(true);
    }
}
