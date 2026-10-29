package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSlotAPI;

import java.util.List;
import java.util.function.ToDoubleFunction;

record DamageConversion(String flagKey, WeaponType mountType, WeaponSize mountSize, Mounting mounting, boolean beamsOnly,
                        DamageType fromType, DamageType toType) {

    static final String KEY_PREFIX = "exiledLib_damageConversion_";

    enum Mounting {
        ANY, HARDPOINT, TURRET;

        boolean matches(WeaponSlotAPI mount) {
            return switch (this) {
                case ANY -> true;
                case HARDPOINT -> mount.isHardpoint();
                case TURRET -> mount.isTurret();
            };
        }
    }

    boolean toEmp() {
        return toType == null;
    }

    boolean appliesTo(WeaponAPI weapon, WeaponSlotAPI mount, DamageType sourceType) {
        return mount != null && (mountType == null || mount.getWeaponType() == mountType) && (mountSize == null || mount.getSlotSize() == mountSize)
                && mounting.matches(mount) && (!beamsOnly || weapon.isBeam()) && (fromType == null || fromType == sourceType) && toType != sourceType;
    }

    static DamageConversion first(WeaponAPI weapon, WeaponSlotAPI mount, DamageType sourceType, List<DamageConversion> conversions,
                                  ToDoubleFunction<String> flagValue) {
        for (DamageConversion conversion : conversions) {
            if (flagValue.applyAsDouble(conversion.flagKey()) > 0 && conversion.appliesTo(weapon, mount, sourceType)) {
                return conversion;
            }
        }
        return null;
    }
}
