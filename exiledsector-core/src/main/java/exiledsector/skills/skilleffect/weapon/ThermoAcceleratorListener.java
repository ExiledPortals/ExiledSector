package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import exiledsector.skills.skilleffect.ShipCombatListener;

import java.util.ArrayList;
import java.util.List;

final class ThermoAcceleratorListener extends ShipCombatListener implements AdvanceableListener {

    private final List<WeaponAPI> hardpointWeapons;

    ThermoAcceleratorListener(ShipAPI ownerShip) {
        super(ownerShip);
        hardpointWeapons = hardpointWeapons(ownerShip.getAllWeapons());
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
