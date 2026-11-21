package exiledsector.harness;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;

import java.util.ArrayList;
import java.util.List;

final class AddShipStep implements HarnessStep {

    private final String variantId;
    private final int count;

    AddShipStep(String variantId, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("addShip needs a count of at least 1, not " + count);
        }
        this.variantId = variantId;
        this.count = count;
    }

    @Override
    public String name() {
        return "addShip " + count + " " + variantId;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        if (Global.getCurrentState() != GameState.CAMPAIGN) {
            return StepOutcome.failed("addShip only works in the campaign");
        }
        if (!Global.getSettings().doesVariantExist(variantId)) {
            return StepOutcome.failed("no ship variant \"" + variantId + "\"");
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        List<String> added = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
            member.setShipName(playerFleet.getFaction().pickRandomShipName());
            playerFleet.getFleetData().addFleetMember(member);
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
            added.add(member.getShipName() + " (" + member.getHullId() + ")");
        }
        playerFleet.getFleetData().setSyncNeeded();
        return StepOutcome.passed("added " + String.join(", ", added) + " to the player fleet");
    }
}
