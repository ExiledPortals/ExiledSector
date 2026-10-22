package exiledsector.skills;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledlib.i18n.NumberText;
import exiledsector.items.CargoRefund;

public record SkillItemCost(String itemId, float quantity) implements CargoRefund {

    public String itemName() {
        return CostItems.displayName(itemId);
    }

    @Override
    public void refundTo(CargoAPI cargo) {
        if (cargo != null) {
            CostItems.add(cargo, itemId, quantity);
        }
    }

    public String formattedQuantity() {
        return formatQuantity(quantity);
    }

    public static String formatQuantity(float value) {
        return NumberText.format(value);
    }
}
