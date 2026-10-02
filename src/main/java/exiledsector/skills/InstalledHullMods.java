package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipVariantAPI;

public final class InstalledHullMods {

    public static final String TAG_PREFIX = "exiledSector_installed_";

    private InstalledHullMods() {
    }

    public static String tag(String hullModId) {
        return TAG_PREFIX + hullModId;
    }

    public static boolean isInstalledBySkillTree(ShipVariantAPI variant, String hullModId) {
        return variant.hasTag(tag(hullModId));
    }

    public static boolean hasHullModOfItsOwn(ShipVariantAPI variant, String hullModId) {
        return variant.hasHullMod(hullModId) && !isInstalledBySkillTree(variant, hullModId);
    }
}
