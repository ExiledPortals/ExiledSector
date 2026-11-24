package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.DamageType;

import java.util.EnumMap;
import java.util.Map;

public final class DamageTypeMults {

    public record Mults(float shield, float armor, float hull) {
    }

    private static final Map<DamageType, Mults> DEFAULTS = snapshot();

    private DamageTypeMults() {
    }

    public static Mults defaults(DamageType type) {
        return DEFAULTS.get(type);
    }

    public static Mults current(DamageType type) {
        return new Mults(type.getShieldMult(), type.getArmorMult(), type.getHullMult());
    }

    public static void set(DamageType type, Mults mults) {
        type.setShieldMult(mults.shield());
        type.setArmorMult(mults.armor());
        type.setHullMult(mults.hull());
    }

    public static boolean atDefaults(DamageType type) {
        return current(type).equals(DEFAULTS.get(type));
    }

    public static void restoreDefaults() {
        for (DamageType type : DamageType.values()) {
            if (!atDefaults(type)) {
                set(type, DEFAULTS.get(type));
            }
        }
    }

    private static Map<DamageType, Mults> snapshot() {
        Map<DamageType, Mults> defaults = new EnumMap<>(DamageType.class);
        for (DamageType type : DamageType.values()) {
            defaults.put(type, current(type));
        }
        return defaults;
    }
}
