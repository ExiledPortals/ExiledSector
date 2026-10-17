package exiledsector.hullitems;

import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.Translation;
import exiledsector.skills.ShipHullSizes;

import java.util.Locale;

public record HullUpgradeData(HullSize hullSize) {

    public static final String ITEM_ID = "exiledSector_hull_upgrade";
    private static final String ICON_FOLDER = "graphics/icons/frameworks/framework_";

    public static HullUpgradeData parse(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        HullSize hullSize = ShipHullSizes.byName(encoded.trim());
        return hullSize == null ? null : new HullUpgradeData(hullSize);
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
        return Translation.msg("upgrade.name").arg("size", ShipHullSizes.displayName(hullSize)).text();
    }
}
