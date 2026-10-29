package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.StatMode;

import java.util.ArrayList;
import java.util.List;

final class ThermoAcceleratorListener extends ShipCombatListener implements AdvanceableListener {

    static final String FLUX_COST_REDUCTION_PERCENT_KEY = "exiledLib_thermoAcceleratorFluxCostReductionPercent";
    private static final String MOD_ID_PREFIX = "exiledLib_thermoAccelerator_";

    private final List<WeaponAPI> hardpointWeapons;

    ThermoAcceleratorListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
        hardpointWeapons = hardpointWeapons(ownerShip.getAllWeapons());
        float reduction = magnitude(FLUX_COST_REDUCTION_PERCENT_KEY);
        if (reduction > 0f) {
            WeaponStatFamily.FLUX_COST.target(WeaponScope.ALL).apply(ownerShip.getMutableStats(), modId, StatMode.PERCENT, -reduction);
        }
    }

    static List<WeaponAPI> hardpointWeapons(List<WeaponAPI> weapons) {
        List<WeaponAPI> hardpoints = new ArrayList<>();
        if (weapons == null) {
            return hardpoints;
        }
        for (WeaponAPI weapon : weapons) {
            WeaponSlotAPI slot = weapon.getSlot();
            if (slot != null && slot.isHardpoint() && !weapon.isDecorative() && !slot.isSystemSlot()) {
                hardpoints.add(weapon);
            }
        }
        return hardpoints;
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            return;
        }
        for (WeaponAPI weapon : hardpointWeapons) {
            weapon.setForceNoFireOneFrame(true);
        }
    }
}
