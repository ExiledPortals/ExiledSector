package exiledsector.ui.stats;

import exiledlib.i18n.Translation;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.weapon.ScopedWeaponEffect;

public enum StatGroup {

    PROGRESSION,
    DURABILITY,
    SHIELDS,
    PHASE,
    FLUX,
    WEAPONS,
    FIGHTERS,
    MOBILITY,
    SYSTEM,
    FLEET,
    READINESS,
    LOGISTICS,
    MECHANICS;

    public String displayName() {
        return Translation.text("ui.stats.group." + name());
    }

    static StatGroup of(SkillEffect effect) {
        if (effect instanceof ScopedWeaponEffect) {
            return WEAPONS;
        }
        String packageName = effect.getClass().getPackageName();
        return switch (packageName.substring(packageName.lastIndexOf('.') + 1)) {
            case "defense", "hullmodule" -> DURABILITY;
            case "shield" -> SHIELDS;
            case "phase" -> PHASE;
            case "flux" -> FLUX;
            case "weapon", "onhit" -> WEAPONS;
            case "fighter" -> FIGHTERS;
            case "movement" -> MOBILITY;
            case "shipsystem" -> SYSTEM;
            case "aura", "battle" -> FLEET;
            case "readiness" -> READINESS;
            case "logistics" -> LOGISTICS;
            default -> MECHANICS;
        };
    }
}
