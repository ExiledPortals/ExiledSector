package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetInflater;
import com.fs.starfarer.api.campaign.listeners.FleetInflationListener;

public class NpcFleetInflationListener implements FleetInflationListener {

    @Override
    public void reportFleetInflated(CampaignFleetAPI fleet, FleetInflater inflater) {
        NpcFleetLeveller.ensure(fleet);
    }
}
