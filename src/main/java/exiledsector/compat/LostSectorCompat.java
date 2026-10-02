package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

public final class LostSectorCompat {

    public static final String MOD_ID = "lost.sector";
    public static final String AUGMENTED_SYSTEMS_HULLMOD_ID = "nskr_augmented";

    private LostSectorCompat() {
    }

    public static boolean isModEnabled() {
        SettingsAPI settings = Global.getSettings();
        ModManagerAPI mods = settings == null ? null : settings.getModManager();
        return mods != null && mods.isModEnabled(MOD_ID);
    }

    public static boolean hasAugmentedSystems(ShipVariantAPI variant) {
        return variant != null && variant.hasHullMod(AUGMENTED_SYSTEMS_HULLMOD_ID) && isModEnabled();
    }
}
