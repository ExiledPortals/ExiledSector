package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.items.crafting.material.MaterialItems;

final class AddSpecialStep implements HarnessStep {

    private final String specialItemId;
    private final String data;
    private final int count;

    AddSpecialStep(String specialItemId, String data, int count) {
        if (specialItemId.isBlank()) {
            throw new IllegalArgumentException("addSpecial needs a special item id");
        }
        if (count < 1) {
            throw new IllegalArgumentException("addSpecial needs a count of at least 1, not " + count);
        }
        this.specialItemId = specialItemId;
        this.data = data == null || data.isEmpty() ? null : data;
        this.count = count;
    }

    @Override
    public String name() {
        return "addSpecial " + specialItemId + " x" + count;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("addSpecial");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        if (MaterialItems.spec(specialItemId) == null) {
            return StepOutcome.failed("no special item \"" + specialItemId + "\"");
        }
        Global.getSector().getPlayerFleet().getCargo().addSpecial(new SpecialItemData(specialItemId, data), count);
        return StepOutcome.passed("added " + count + " " + specialItemId + " to the player fleet's cargo");
    }
}
