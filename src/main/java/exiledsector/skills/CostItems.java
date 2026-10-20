package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;

import static com.fs.starfarer.api.campaign.CargoAPI.CargoItemType.SPECIAL;

public final class CostItems {

    private CostItems() {
    }

    public static boolean exists(String itemId) {
        if (Global.getSettings() == null) {
            return true;
        }
        return commoditySpec(itemId) != null || specialItemSpec(itemId) != null;
    }

    public static String displayName(String itemId) {
        CommoditySpecAPI commodity = commoditySpec(itemId);
        if (commodity != null) {
            return commodity.getName();
        }
        SpecialItemSpecAPI special = specialItemSpec(itemId);
        return special != null ? special.getName() : itemId;
    }

    public static float held(CargoAPI cargo, String itemId) {
        if (cargo == null) {
            return 0f;
        }
        return isSpecial(itemId) ? cargo.getQuantity(SPECIAL, new SpecialItemData(itemId, null)) : cargo.getCommodityQuantity(itemId);
    }

    public static void remove(CargoAPI cargo, String itemId, float quantity) {
        if (isSpecial(itemId)) {
            cargo.removeItems(SPECIAL, new SpecialItemData(itemId, null), quantity);
        } else {
            cargo.removeCommodity(itemId, quantity);
        }
    }

    public static void add(CargoAPI cargo, String itemId, float quantity) {
        if (isSpecial(itemId)) {
            cargo.addSpecial(new SpecialItemData(itemId, null), quantity);
        } else {
            cargo.addCommodity(itemId, quantity);
        }
    }

    static boolean isSpecial(String itemId) {
        return Global.getSettings() != null && commoditySpec(itemId) == null && specialItemSpec(itemId) != null;
    }

    private static CommoditySpecAPI commoditySpec(String itemId) {
        SettingsAPI settings = Global.getSettings();
        if (settings == null) {
            return null;
        }
        try {
            return settings.getCommoditySpec(itemId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static SpecialItemSpecAPI specialItemSpec(String itemId) {
        SettingsAPI settings = Global.getSettings();
        if (settings == null) {
            return null;
        }
        try {
            return settings.getSpecialItemSpec(itemId);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
