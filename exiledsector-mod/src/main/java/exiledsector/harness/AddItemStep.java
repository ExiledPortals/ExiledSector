package exiledsector.harness;

import com.fs.starfarer.api.Global;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;

final class AddItemStep implements HarnessStep {

    static final long ROLL_SEED = 1L;

    private final String definitionId;

    AddItemStep(String definitionId) {
        if (definitionId.isBlank()) {
            throw new IllegalArgumentException("addItem needs an item definition id");
        }
        this.definitionId = definitionId;
    }

    @Override
    public String name() {
        return "addItem " + definitionId;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("addItem");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        ItemDefinition definition = ItemDefinitions.get(definitionId);
        if (definition == null) {
            return StepOutcome.failed("no item definition \"" + definitionId + "\"");
        }
        Global.getSector().getPlayerFleet().getCargo().addSpecial(ItemData.rolled(definition, ROLL_SEED).toSpecialItem(), 1f);
        return StepOutcome.passed("added " + definitionId + " to the player fleet's cargo");
    }
}
