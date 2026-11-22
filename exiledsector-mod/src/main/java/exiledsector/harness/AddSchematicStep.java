package exiledsector.harness;

import com.fs.starfarer.api.Global;
import exiledsector.items.schematic.RefitSchematic;

import java.util.Arrays;
import java.util.Locale;

final class AddSchematicStep implements HarnessStep {

    private final RefitSchematic tier;
    private final int count;

    AddSchematicStep(String tierName, int count) {
        this.tier = Arrays.stream(RefitSchematic.values()).filter(candidate -> candidate.name().equals(tierName.toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("addSchematic needs a tier of " + Arrays.toString(RefitSchematic.values()) + ", not \"" + tierName + "\""));
        if (count < 1) {
            throw new IllegalArgumentException("addSchematic needs a count of at least 1, not " + count);
        }
        this.count = count;
    }

    @Override
    public String name() {
        return "addSchematic " + tier + " x" + count;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("addSchematic");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        Global.getSector().getPlayerFleet().getCargo().addSpecial(tier.toSpecialItem(), count);
        return StepOutcome.passed("added " + count + " " + tier + " refit schematic(s) to the player fleet's cargo");
    }
}
