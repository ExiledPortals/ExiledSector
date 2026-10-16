package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record CompatTarget(String displayName, List<String> modIds, List<String> testedVersions, Supplier<List<String>> missingFeatures) {

    public boolean isEnabled() {
        SettingsAPI settings = Global.getSettings();
        ModManagerAPI modManager = settings == null ? null : settings.getModManager();
        return modManager != null && modIds.stream().anyMatch(modManager::isModEnabled);
    }

    static List<String> missingHullMods(List<String> hullModIds) {
        SettingsAPI settings = Global.getSettings();
        List<String> missing = new ArrayList<>();
        for (String hullModId : hullModIds) {
            if (settings == null || settings.getHullModSpec(hullModId) == null) {
                missing.add("the hull mod " + hullModId);
            }
        }
        return missing;
    }
}
