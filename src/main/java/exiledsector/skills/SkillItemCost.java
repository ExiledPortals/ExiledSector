package exiledsector.skills;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.i18n.NumberText;

public record SkillItemCost(String itemId, float quantity) {

    public String commodityName() {
        return CommodityNames.displayName(itemId);
    }

    public void refundTo(CargoAPI cargo) {
        if (cargo != null) {
            cargo.addCommodity(itemId, quantity);
        }
    }

    public String formattedQuantity() {
        return formatQuantity(quantity);
    }

    public static String formatQuantity(float value) {
        return NumberText.format(value);
    }
}
