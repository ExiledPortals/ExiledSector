package exiledsector.charts.anomaly.station;

import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.fleets.SourceBasedFleetManager;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.charts.run.ChartRunBound;

public final class MothershipFleetManager extends SourceBasedFleetManager implements ChartRunBound {

    static final StationFleets.Launch LAUNCH = new StationFleets.Launch(Factions.DERELICT, StationRules.DRONE_MAX_SHIP_SIZE, (random, fleet) -> {
    });

    private final StationLaunches launches;
    private int fleetsLost;

    MothershipFleetManager(CampaignFleetAPI mothership, String runId, int tier, long seed) {
        super(mothership, StationRules.THRESHOLD_LY, StationRules.MIN_FLEETS, StationRules.MOTHERSHIP_MAX_FLEETS, StationRules.MOTHERSHIP_RESPAWN_DAYS);
        this.launches = new StationLaunches(runId, tier, seed);
    }

    @Override
    protected CampaignFleetAPI spawnFleet() {
        return launches.next(source, LAUNCH, fleetsLost);
    }

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        super.reportFleetDespawnedToListener(fleet, reason, param);
        if (reason == FleetDespawnReason.DESTROYED_BY_BATTLE) {
            fleetsLost++;
        }
    }

    @Override
    public boolean belongsToRun(String wantedRunId) {
        return launches.belongsToRun(wantedRunId);
    }

    int fleetsLost() {
        return fleetsLost;
    }

    int maxFleets() {
        return maxFleets;
    }
}
