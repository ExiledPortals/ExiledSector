package exiledsector.charts.anomaly.stormfront;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.reward.OfficerPoolLoot;
import exiledsector.charts.run.ChartRun;
import exiledsector.loot.Loot;

public record ThunderDuel(int extraRewards) {

    public static final ThunderDuel NONE = new ThunderDuel(0);
    static final String SEED_PREFIX = "thunderDuel|";

    public static ThunderDuel at(SectorAPI sector, ChartRun run) {
        StormFrontTerrainPlugin front = sector == null || run == null ? null : StormFront.frontHolding(sector.getPlayerFleet());
        return front != null && run.runId().equals(front.runId()) ? new ThunderDuel(StormTuning.of(run).officerExtraRewards()) : NONE;
    }

    public Loot extraLoot(SectorAPI sector, AtlasOfficer officer, ChartRun run) {
        return OfficerPoolLoot.drawn(sector, officer, run, extraRewards, SEED_PREFIX);
    }
}
