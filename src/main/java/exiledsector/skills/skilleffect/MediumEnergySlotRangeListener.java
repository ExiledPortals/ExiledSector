package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.loading.WeaponSlotAPI;

final class MediumEnergySlotRangeListener implements WeaponBaseRangeModifier {

    static final String RANGE_FLAT_KEY = "exiledSector_mediumEnergySlotRangeFlat";

    @Override
    public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
        return 0f;
    }

    @Override
    public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
        return 1f;
    }

    @Override
    public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
        WeaponSlotAPI slot = weapon.getSlot();
        if (slot == null || slot.getWeaponType() != WeaponAPI.WeaponType.ENERGY || slot.getSlotSize() != WeaponAPI.WeaponSize.MEDIUM) {
            return 0f;
        }
        return ship.getMutableStats().getDynamic().getValue(RANGE_FLAT_KEY, 0f);
    }
}
