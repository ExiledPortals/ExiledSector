package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.ArrayList;
import java.util.List;

public final class NightcrossCompat {

    public static final String MOD_ID = "Nightcross";
    public static final List<String> TESTED_VERSIONS = List.of("2.1.5", "2.1.6");
    public static final List<String> REQUIRED_HULL_MOD_IDS = List.of("nightcrosstargeting", "na_gravitydrag");
    static final String GRAVITY_DRAG_MOD_SUFFIX = "_gravitydrag_target_data";
    public static final CompatTarget TARGET = new CompatTarget("Nightcross Armory area", List.of(MOD_ID), TESTED_VERSIONS,
            NightcrossCompat::missingFeatures);

    private NightcrossCompat() {
    }

    public static float gravityDragPercentOn(ShipAPI target) {
        MutableStat.StatMod drag = target.getMutableStats().getTimeMult().getMultMods().get(target.getId() + GRAVITY_DRAG_MOD_SUFFIX);
        return drag == null ? 0f : Math.max(0f, (1f - drag.getValue()) * 100f);
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>(CompatTarget.missingHullMods(REQUIRED_HULL_MOD_IDS));
        for (String hullId : NightcrossUnlocks.PROTOTYPE_HULL_IDS.stream().sorted().toList()) {
            if (!hullSpecExists(hullId)) {
                missing.add("the Ancient Prototype hull " + hullId + ", used to unlock Candle's Grace Replica");
            }
        }
        return missing;
    }

    private static boolean hullSpecExists(String hullId) {
        try {
            return Global.getSettings().getHullSpec(hullId) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static boolean isModEnabled() {
        SettingsAPI settings = Global.getSettings();
        ModManagerAPI modManager = settings == null ? null : settings.getModManager();
        return modManager != null && modManager.isModEnabled(MOD_ID);
    }
}
