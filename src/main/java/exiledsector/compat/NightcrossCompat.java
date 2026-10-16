package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.List;

public final class NightcrossCompat {

    public static final String MOD_ID = "Nightcross";
    public static final List<String> TESTED_VERSIONS = List.of("2.1.5");
    public static final List<String> REQUIRED_HULL_MOD_IDS = List.of("nightcrosstargeting", "na_gravitydrag");
    static final String GRAVITY_DRAG_MOD_SUFFIX = "_gravitydrag_target_data";
    public static final CompatTarget TARGET = new CompatTarget("Nightcross Armory area", List.of(MOD_ID), TESTED_VERSIONS,
            () -> CompatTarget.missingHullMods(REQUIRED_HULL_MOD_IDS));

    private NightcrossCompat() {
    }

    public static float gravityDragPercentOn(ShipAPI target) {
        MutableStat.StatMod drag = target.getMutableStats().getTimeMult().getMultMods().get(target.getId() + GRAVITY_DRAG_MOD_SUFFIX);
        return drag == null ? 0f : Math.max(0f, (1f - drag.getValue()) * 100f);
    }

    public static boolean isModEnabled() {
        SettingsAPI settings = Global.getSettings();
        ModManagerAPI modManager = settings == null ? null : settings.getModManager();
        return modManager != null && modManager.isModEnabled(MOD_ID);
    }
}
