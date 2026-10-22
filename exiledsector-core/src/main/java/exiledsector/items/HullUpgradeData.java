package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.i18n.Translation;
import exiledlib.skills.ShipHullSizes;

import java.util.Locale;

public record HullUpgradeData(HullSize hullSize) {

    public static final String ITEM_ID = "exiledSector_hull_upgrade";
    private static final String ICON_FOLDER = "graphics/icons/hull_upgrades/hull_upgrade_";

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

    public static Integer absorbInto(ItemStore store, CargoAPI cargo, CargoStackAPI stack) {
        HullUpgradeData upgrade = of(stack.getSpecialDataIfSpecial());
        if (upgrade == null) {
            return null;
        }
        int stackCount = Math.round(stack.getSize());
        if (stackCount < 1) {
            return 0;
        }
        store.addUpgrades(upgrade.hullSize(), stackCount);
        cargo.removeStack(stack);
        return stackCount;
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
