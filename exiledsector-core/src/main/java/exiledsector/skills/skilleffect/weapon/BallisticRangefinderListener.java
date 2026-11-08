package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.impl.hullmods.BallisticRangefinder;

final class BallisticRangefinderListener implements WeaponBaseRangeModifier {

    static final String BONUS_PERCENT_KEY = "exiledSector_ballisticRangefinderBonusPercent";

    private final float smallWeaponBonus;
    private final float mediumWeaponBonus;
    private final float maxBaseRange;

    BallisticRangefinderListener(ShipAPI ship) {
        WeaponSize largestSlot = BallisticRangefinder.getLargestBallisticSlot(ship);
        if (largestSlot == WeaponSize.LARGE) {
            smallWeaponBonus = BallisticRangefinder.BONUS_SMALL_3;
            mediumWeaponBonus = BallisticRangefinder.BONUS_MEDIUM_3;
            maxBaseRange = BallisticRangefinder.BONUS_MAX_3;
        } else if (largestSlot == WeaponSize.MEDIUM) {
            smallWeaponBonus = BallisticRangefinder.BONUS_SMALL_2;
            mediumWeaponBonus = 0f;
            maxBaseRange = BallisticRangefinder.BONUS_MAX_2;
        } else if (largestSlot == WeaponSize.SMALL) {
            smallWeaponBonus = BallisticRangefinder.BONUS_SMALL_1;
            mediumWeaponBonus = 0f;
            maxBaseRange = BallisticRangefinder.BONUS_MAX_1;
        } else {
            smallWeaponBonus = 0f;
            mediumWeaponBonus = 0f;
            maxBaseRange = 0f;
        }
    }

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
        if (maxBaseRange <= 0f || weapon.getSpec() == null || weapon.hasAIHint(AIHints.PD)) {
            return 0f;
        }
        WeaponType mountType = weapon.getSpec().getMountType();
        if (mountType != WeaponType.BALLISTIC && mountType != WeaponType.HYBRID) {
            return 0f;
        }
        float share = ship.getMutableStats().getDynamic().getValue(BONUS_PERCENT_KEY, 0f) / 100f;
        float bonus = sizeBonus(weapon.getSize()) * share;
        if (mountType == WeaponType.HYBRID) {
            bonus = Math.max(bonus * BallisticRangefinder.HYBRID_MULT, BallisticRangefinder.HYBRID_BONUS_MIN * share);
        }
        float baseRange = weapon.getSpec().getMaxRange();
        return Math.max(0f, Math.min(bonus, maxBaseRange - baseRange));
    }

    private float sizeBonus(WeaponSize size) {
        if (size == WeaponSize.SMALL) {
            return smallWeaponBonus;
        }
        return size == WeaponSize.MEDIUM ? mediumWeaponBonus : 0f;
    }
}
