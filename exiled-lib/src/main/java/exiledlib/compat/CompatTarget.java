package exiledlib.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record CompatTarget(String displayName, List<String> modIds, List<String> testedVersions, Supplier<List<String>> missingFeatures) {

    public boolean isEnabled() {
        return modIds.stream().anyMatch(CompatTarget::isModEnabled);
    }

    public static boolean isModEnabled(String modId) {
        SettingsAPI settings = Global.getSettings();
        ModManagerAPI modManager = settings == null ? null : settings.getModManager();
        return modManager != null && modManager.isModEnabled(modId);
    }

    public static List<String> missingHullMods(List<String> hullModIds) {
        SettingsAPI settings = Global.getSettings();
        List<String> missing = new ArrayList<>();
        for (String hullModId : hullModIds) {
            if (settings == null || settings.getHullModSpec(hullModId) == null) {
                missing.add("the hull mod " + hullModId);
            }
        }
        return missing;
    }

    public static boolean specExists(Supplier<Object> spec) {
        try {
            return spec.get() != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static void raiseSectorFlag(SectorAPI sector, String flag, Logger log, String logLine) {
        sector.getMemoryWithoutUpdate().set(flag, true);
        log.info("[ExiledLib] " + logLine + ".");
    }
}
