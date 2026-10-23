package exiledlib.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import exiledlib.skills.skilleffect.ScaledBonus;
import exiledlib.skills.skilleffect.ShipCombatListener;

import java.util.ArrayList;
import java.util.List;

import static exiledlib.skills.skilleffect.ScaledBonus.percent;

final class ThermoAcceleratorListener extends ShipCombatListener implements AdvanceableListener {

    static final String RATE_OF_FIRE_PERCENT_KEY = "exiledSector_thermoAcceleratorRateOfFirePercent";
    private static final String MOD_ID_PREFIX = "exiledSector_thermoAccelerator_";
    private static final ScaledBonus TURRET_RATE_OF_FIRE = new ScaledBonus(
            percent(WeaponStatFamily.FIRE_RATE.target(WeaponScope.ALL), RATE_OF_FIRE_PERCENT_KEY));

    private final List<WeaponAPI> hardpointWeapons;

    ThermoAcceleratorListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
        hardpointWeapons = hardpointWeapons(ownerShip.getAllWeapons());
        TURRET_RATE_OF_FIRE.apply(ownerShip.getMutableStats(), modId, 1f);
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
