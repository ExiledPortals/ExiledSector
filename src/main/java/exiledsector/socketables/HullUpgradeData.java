package exiledsector.socketables;

import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.Translation;

import java.util.List;
import java.util.Locale;

public record HullUpgradeData(HullSize hullSize) {

    public static final String ITEM_ID = "exiledSector_hull_framework";
    public static final List<HullSize> HULL_SIZES = List.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP);
    private static final String ICON_FOLDER = "graphics/icons/frameworks/framework_";
    private static final String LEGACY_FIELD_SEPARATOR = "/";

    public static HullUpgradeData parse(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        String sizeName = encoded.split(LEGACY_FIELD_SEPARATOR, -1)[0].trim();
        for (HullSize hullSize : HULL_SIZES) {
            if (hullSize.name().equals(sizeName)) {
                return new HullUpgradeData(hullSize);
            }
        }
        return null;
    }

    public static HullUpgradeData of(SpecialItemData special) {
        return special != null && ITEM_ID.equals(special.getId()) ? parse(special.getData()) : null;
    }

    public String encode() {
        return hullSize.name();
    }

    public SpecialItemData toSpecialItem() {
        return new SpecialItemData(ITEM_ID, encode());
    }

    public String iconPath() {
        String sizeName = hullSize == HullSize.CAPITAL_SHIP ? "capital" : hullSize.name().toLowerCase(Locale.ROOT);
        return ICON_FOLDER + sizeName + "_unique.png";
    }

    public String name() {
        return Translation.msg("upgrade.name").arg("size", Translation.text("hullSize." + hullSize.name())).text();
    }
}
