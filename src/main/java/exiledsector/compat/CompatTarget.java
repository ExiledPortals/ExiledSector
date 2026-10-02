package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record CompatTarget(String name, List<String> modIds, List<String> testedVersions, Supplier<List<String>> missingFeatures) {

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
