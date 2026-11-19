package exiledsector.charts.anomaly.stormfront;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.Seeds;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.OfficerRewardDrop;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.RewardContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.loot.Loot;

import java.util.List;
import java.util.Random;

public record ThunderDuel(int extraRewards) {

    public static final ThunderDuel NONE = new ThunderDuel(0);
    static final String SEED_PREFIX = "thunderDuel|";

    public static ThunderDuel at(SectorAPI sector, ChartRun run) {
        StormFrontTerrainPlugin front = sector == null || run == null ? null : StormFront.frontHolding(sector.getPlayerFleet());
        return front != null && run.runId().equals(front.runId()) ? new ThunderDuel(StormTuning.of(run).officerExtraRewards()) : NONE;
    }

    public Loot extraLoot(SectorAPI sector, AtlasOfficer officer, ChartRun run) {
        if (extraRewards <= 0 || officer == null || run == null) {
            return Loot.NONE;
        }
        Random random = Seeds.random(SEED_PREFIX + run.runId() + "|" + officer.recordId());
        List<CarriedItem> drawn = OfficerRewardPool.offers(RewardContext.forRun(sector, run, officer.factionId()), extraRewards, random).stream()
                .map(offer -> OfficerRewardPool.picked(offer, random))
                .toList();
        return drawn.isEmpty() ? Loot.NONE : OfficerRewardDrop.roll(drawn, run, random);
    }
}
