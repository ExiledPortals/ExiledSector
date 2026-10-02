package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;

public final class ShipSystemCharges {

    private ShipSystemCharges() {
    }

    public static boolean limited(ShipHullSpecAPI hullSpec) {
        SettingsAPI settings = Global.getSettings();
        if (hullSpec == null || settings == null) return false;

        return usesCharges(settings, hullSpec.getShipSystemId()) || usesCharges(settings, hullSpec.getShipDefenseId());
    }

    private static boolean usesCharges(SettingsAPI settings, String systemId) {
        ShipSystemSpecAPI spec = systemId == null || systemId.isEmpty() ? null : settings.getShipSystemSpec(systemId);
        return spec != null && spec.usesAmmo();
    }
}
