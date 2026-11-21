package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

final class ReadyFleetStep implements HarnessStep {

    static final float SPARE_CREW_SHARE = 0.1f;

    private final float suppliesShare;

    ReadyFleetStep(float suppliesShare) {
        if (suppliesShare < 0f || suppliesShare > 1f) {
            throw new IllegalArgumentException("readyFleet needs a supplies share from 0 to 1, not " + suppliesShare);
        }
        this.suppliesShare = suppliesShare;
    }

    @Override
    public String name() {
        return "readyFleet";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign(name());
        if (notInCampaign != null) {
            return notInCampaign;
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        playerFleet.getFleetData().syncIfNeeded();
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            member.getStatus().repairFully();
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        }
        CargoAPI cargo = playerFleet.getCargo();
        int crew = Math.min((int) cargo.getMaxPersonnel(), (int) Math.ceil(playerFleet.getFleetData().getMinCrew() * (1f + SPARE_CREW_SHARE)));
        cargo.removeCrew(cargo.getCrew());
        cargo.addCrew(crew);
        cargo.addFuel(cargo.getMaxFuel() - cargo.getFuel());
        float otherCargo = cargo.getSpaceUsed() - cargo.getSupplies();
        float supplies = Math.max(0f, (float) Math.floor((cargo.getMaxCapacity() - otherCargo) * suppliesShare));
        cargo.removeSupplies(cargo.getSupplies());
        cargo.addSupplies(supplies);
        return StepOutcome.passed("repaired every ship to full CR, " + crew + " crew, " + Math.round(cargo.getFuel()) + " fuel, "
                + Math.round(supplies) + " supplies");
    }
}
