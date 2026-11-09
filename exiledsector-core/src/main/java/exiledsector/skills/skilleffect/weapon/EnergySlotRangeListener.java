package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;

final class EnergySlotRangeListener implements WeaponBaseRangeModifier {

    static final String MEDIUM_SLOT_RANGE_FLAT_KEY = "exiledSector_mediumEnergySlotRangeFlat";
    static final String ANY_SLOT_RANGE_FLAT_KEY = "exiledSector_energySlotRangeFlat";

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
        if (slot == null || slot.getWeaponType() != WeaponAPI.WeaponType.ENERGY) {
            return 0f;
        }
        DynamicStatsAPI dynamic = ship.getMutableStats().getDynamic();
        float rangeBonus = dynamic.getValue(ANY_SLOT_RANGE_FLAT_KEY, 0f);
        if (slot.getSlotSize() == WeaponAPI.WeaponSize.MEDIUM) {
            rangeBonus += dynamic.getValue(MEDIUM_SLOT_RANGE_FLAT_KEY, 0f);
        }
        return rangeBonus;
    }
}
