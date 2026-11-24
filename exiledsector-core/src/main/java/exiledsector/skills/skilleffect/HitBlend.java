package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledsector.skills.skilleffect.DamageTypeMults.Mults;

import java.util.function.ToDoubleFunction;

public final class HitBlend {

    private HitBlend() {
    }

    public static Mults of(TypeShares shares, MutableShipStatsAPI targetStats, ToDoubleFunction<DamageType> typeScale) {
        float shield = shares.weighted(type -> typeScale.applyAsDouble(type) * DamageTypeMults.defaults(type).shield() * shieldDamageTaken(type, targetStats));
        float armor = shares.weighted(type -> typeScale.applyAsDouble(type) * DamageTypeMults.defaults(type).armor() * damageTaken(type, targetStats)
                * kineticArmorTaken(type, targetStats));
        float hull = shares.weighted(type -> typeScale.applyAsDouble(type) * DamageTypeMults.defaults(type).hull() * damageTaken(type, targetStats));
        return new Mults(shield, armor, hull);
    }

    static float damageTaken(DamageType type, MutableShipStatsAPI stats) {
        return switch (type) {
            case KINETIC -> stats.getKineticDamageTakenMult().getModifiedValue();
            case HIGH_EXPLOSIVE -> stats.getHighExplosiveDamageTakenMult().getModifiedValue();
            case FRAGMENTATION -> stats.getFragmentationDamageTakenMult().getModifiedValue();
            case ENERGY -> stats.getEnergyDamageTakenMult().getModifiedValue();
            case OTHER -> 1f;
        };
    }

    static float shieldDamageTaken(DamageType type, MutableShipStatsAPI stats) {
        return switch (type) {
            case KINETIC -> stats.getKineticShieldDamageTakenMult().getModifiedValue();
            case HIGH_EXPLOSIVE -> stats.getHighExplosiveShieldDamageTakenMult().getModifiedValue();
            case FRAGMENTATION -> stats.getFragmentationShieldDamageTakenMult().getModifiedValue();
            case ENERGY -> stats.getEnergyShieldDamageTakenMult().getModifiedValue();
            case OTHER -> 1f;
        };
    }

    private static float kineticArmorTaken(DamageType type, MutableShipStatsAPI stats) {
        return type == DamageType.KINETIC ? stats.getKineticArmorDamageTakenMult().getModifiedValue() : 1f;
    }
}
