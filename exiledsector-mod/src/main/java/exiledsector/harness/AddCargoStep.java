package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;

final class AddCargoStep implements HarnessStep {

    private final String commodityId;
    private final int amount;

    AddCargoStep(String commodityId, int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("addCargo needs an amount of at least 1, not " + amount);
        }
        this.commodityId = commodityId;
        this.amount = amount;
    }

    @Override
    public String name() {
        return "addCargo " + amount + " " + commodityId;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("addCargo");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        if (!commodityExists(commodityId)) {
            return StepOutcome.failed("no commodity \"" + commodityId + "\"");
        }
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        cargo.addCommodity(commodityId, amount);
        return StepOutcome.passed("the player fleet now carries " + Math.round(cargo.getCommodityQuantity(commodityId)) + " " + commodityId);
    }

    private static boolean commodityExists(String commodityId) {
        for (CommoditySpecAPI spec : Global.getSettings().getAllCommoditySpecs()) {
            if (spec.getId().equals(commodityId)) {
                return true;
            }
        }
        return false;
    }
}
