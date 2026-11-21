package exiledsector.charts.anomaly.landmark;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.charts.anomaly.LootShaper;

public final class RemnantNexusStation extends LaunchingStation implements LootShaper {

    public static final String ANOMALY_ID = "remnant_nexus_station";
    public static final RemnantNexusStation INSTANCE = new RemnantNexusStation();

    private RemnantNexusStation() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    String factionId() {
        return Factions.REMNANTS;
    }

    @Override
    String variantFor(int tier) {
        return StationRules.damagedNexus(tier) ? StationRules.NEXUS_DAMAGED_VARIANT : StationRules.NEXUS_VARIANT;
    }

    @Override
    String nameKey(int tier) {
        return StationRules.damagedNexus(tier) ? "chart.anomaly.remnant_nexus_station.damagedName" : "chart.anomaly.remnant_nexus_station.name";
    }

    @Override
    boolean damaged(int tier) {
        return StationRules.damagedNexus(tier);
    }

    @Override
    public boolean overchargesHullItems(CampaignFleetAPI fleet) {
        return fleet != null && fleet.hasTag(stationTag());
    }

    @Override
    EveryFrameScript manager(CampaignFleetAPI station, String runId, int tier, long seed) {
        return new NexusFleetManager(station, runId, tier, seed);
    }
}
