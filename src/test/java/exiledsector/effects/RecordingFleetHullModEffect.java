package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.HullModFleetEffect;

public class RecordingFleetHullModEffect extends RecordingHullModEffect implements HullModFleetEffect {

    @Override
    public void advanceInCampaign(CampaignFleetAPI fleet) {
        CALLS.add("fleetAdvance");
    }

    @Override
    public boolean withAdvanceInCampaign() {
        return false;
    }

    @Override
    public boolean withOnFleetSync() {
        return false;
    }

    @Override
    public void onFleetSync(CampaignFleetAPI fleet) {
        CALLS.add("fleetSync");
    }
}
