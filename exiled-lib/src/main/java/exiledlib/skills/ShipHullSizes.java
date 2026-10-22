package exiledlib.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;

import java.util.List;

public final class ShipHullSizes {

    public static final List<HullSize> ORDER = List.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP);

    private ShipHullSizes() {
    }

    public static boolean isShipSize(HullSize hullSize) {
        return hullSize != null && ORDER.contains(hullSize);
    }

    public static HullSize byName(String hullSizeName) {
        for (HullSize hullSize : ORDER) {
            if (hullSize.name().equals(hullSizeName)) {
                return hullSize;
            }
        }
        return null;
    }

    public static String displayName(HullSize hullSize) {
        return Translation.data(nameKey(hullSize), hullSize.name());
    }

    public static StyledText styledDisplayName(HullSize hullSize) {
        return Translation.dataStyled(nameKey(hullSize), hullSize.name());
    }

    private static String nameKey(HullSize hullSize) {
        return "hullSize." + hullSize.name();
    }
}
