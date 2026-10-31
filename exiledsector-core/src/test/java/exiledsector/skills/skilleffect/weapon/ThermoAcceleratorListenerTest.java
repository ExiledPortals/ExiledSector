package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThermoAcceleratorListenerTest {

    private static ShipAPI ship() {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getMutableStats()).thenReturn(stats);
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
    void thermoAcceleratorCutsWeaponFluxCostAsAStat() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);

        WeaponSkillEffect.THERMO_ACCELERATOR_FLUX_COST_REDUCTION_PERCENT.apply(stats, "mod_id", 50f);

        verify(stats.getBallisticWeaponFluxCostMod()).modifyPercent("mod_id", -50f);
        verify(stats.getEnergyWeaponFluxCostMod()).modifyPercent("mod_id", -50f);
        verify(stats.getMissileWeaponFluxCostMod()).modifyPercent("mod_id", -50f);
    }

    @Test
    void thermoAcceleratorKeepsOnlyRealHardpointsFromFiring() {
        WeaponAPI hardpoint = weapon(true, false, false);
        WeaponAPI turret = weapon(false, false, false);
        WeaponAPI decorative = weapon(true, true, false);
        WeaponAPI system = weapon(true, false, true);
        ShipAPI ship = ship();
        when(ship.getAllWeapons()).thenReturn(List.of(hardpoint, turret, decorative, system));

        new ThermoAcceleratorListener(ship).advance(0.1f);

        verify(hardpoint).setForceNoFireOneFrame(true);
        verify(turret, never()).setForceNoFireOneFrame(true);
        verify(decorative, never()).setForceNoFireOneFrame(true);
        verify(system, never()).setForceNoFireOneFrame(true);
    }
}
