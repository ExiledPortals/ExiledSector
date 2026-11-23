package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;

final class ArchaicWeaponRangeListener implements WeaponRangeModifier {

    static final String RANGE_PERCENT_KEY = "exiledSector_archaicWeaponRangePercent";
    static final String ARCHAIC_WEAPON_TAG = "archaic_c";

    @Override
    public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
        if (weapon == null || weapon.getSpec() == null || !weapon.getSpec().hasTag(ARCHAIC_WEAPON_TAG)) {
            return 0f;
        }
        return ship.getMutableStats().getDynamic().getValue(RANGE_PERCENT_KEY, 0f);
    }

    @Override
    public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
        return 1f;
    }

    @Override
    public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
        return 0f;
    }
}
