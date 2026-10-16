package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import exiledsector.i18n.NumberText;

public record SkillItemCost(String itemId, float quantity) {

    public String commodityName() {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(itemId);
        return spec != null ? spec.getName() : itemId;
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
