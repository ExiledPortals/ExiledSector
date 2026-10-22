package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.HullModSpecAPI;

public final class HullModNames {

    private HullModNames() {
    }

    public static String displayName(String hullModId) {
        String name = loadedDisplayName(hullModId);
        return name != null ? name : hullModId;
    }

    public static String loadedDisplayName(String hullModId) {
        HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
        return spec != null ? spec.getDisplayName() : null;
    }
}
