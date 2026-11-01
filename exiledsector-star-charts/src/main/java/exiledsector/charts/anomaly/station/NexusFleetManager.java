package exiledsector.charts.anomaly.station;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantStationFleetManager;
import exiledsector.charts.run.ChartRunBound;

public final class NexusFleetManager extends RemnantStationFleetManager implements ChartRunBound {

    static final StationFleets.Launch LAUNCH = new StationFleets.Launch(Factions.REMNANTS, StationRules.NO_SHIP_SIZE_CAP,
            (random, fleet) -> RemnantSeededFleetManager.initRemnantFleetProperties(random, fleet, false));

    private final StationLaunches launches;

    NexusFleetManager(CampaignFleetAPI nexus, String runId, int tier, long seed) {
        super(nexus, StationRules.THRESHOLD_LY, StationRules.MIN_FLEETS,
                StationRules.damagedNexus(tier) ? StationRules.DAMAGED_NEXUS_MAX_FLEETS : StationRules.NEXUS_MAX_FLEETS,
                StationRules.damagedNexus(tier) ? StationRules.DAMAGED_NEXUS_RESPAWN_DAYS : StationRules.NEXUS_RESPAWN_DAYS,
                StationRules.VANILLA_MIN_POINTS, StationRules.VANILLA_MAX_POINTS);
        this.launches = new StationLaunches(runId, tier, seed);
    }

    @Override
    public void advance(float amount) {
        addedListener = true;
        super.advance(amount);
    }

    @Override
    protected CampaignFleetAPI spawnFleet() {
        return launches.next(source, LAUNCH, getTotalLost());
    }

    @Override
    public boolean belongsToRun(String wantedRunId) {
        return launches.belongsToRun(wantedRunId);
    }

    int maxFleets() {
        return maxFleets;
    }

    float respawnDays() {
        return respawnDelay;
    }
}
