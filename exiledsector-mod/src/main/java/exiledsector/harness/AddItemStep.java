package exiledsector.harness;

import com.fs.starfarer.api.Global;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.modifier.ItemRoll;

final class AddItemStep implements HarnessStep {

    static final long ROLL_SEED = 1L;
    static final int RANDOM_MODIFIERS = -1;

    private final String definitionId;
    private final int count;
    private final long seed;
    private final int modifierCount;

    AddItemStep(String definitionId, int count, long seed, int modifierCount) {
        if (definitionId.isBlank()) {
            throw new IllegalArgumentException("addItem needs an item definition id");
        }
        if (count < 1) {
            throw new IllegalArgumentException("addItem needs a count of at least 1, not " + count);
        }
        if (modifierCount < RANDOM_MODIFIERS) {
            throw new IllegalArgumentException("addItem needs a modifier count of at least 0, not " + modifierCount);
        }
        this.definitionId = definitionId;
        this.count = count;
        this.seed = seed;
        this.modifierCount = modifierCount;
    }

    @Override
    public String name() {
        return "addItem " + definitionId + (count > 1 ? " x" + count : "") + (modifierCount == RANDOM_MODIFIERS ? "" : " with " + modifierCount + " modifiers");
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
        ItemRoll roll = modifierCount == RANDOM_MODIFIERS ? ItemRoll.PLAIN : ItemRoll.withModifierCount(modifierCount);
        for (int copy = 0; copy < count; copy++) {
            Global.getSector().getPlayerFleet().getCargo().addSpecial(ItemData.rolled(definition, seed + copy, roll).toSpecialItem(), 1f);
        }
        return StepOutcome.passed("added " + count + " " + definitionId + " to the player fleet's cargo");
    }
}
