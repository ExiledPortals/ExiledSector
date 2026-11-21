package exiledsector.charts.officer.reward;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.Seeds;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.run.ChartRun;
import exiledsector.loot.Loot;

import java.util.List;
import java.util.Random;

public final class OfficerPoolLoot {

    private OfficerPoolLoot() {
    }

    public static Loot drawn(SectorAPI sector, AtlasOfficer officer, ChartRun run, int count, String seedPrefix) {
        if (count <= 0 || officer == null || run == null) {
            return Loot.NONE;
        }
        Random random = Seeds.random(seedPrefix + run.runId() + "|" + officer.recordId());
        List<CarriedItem> drawn = OfficerRewardPool.offers(RewardContext.forRun(sector, run, officer.factionId()), count, random).stream()
                .map(offer -> OfficerRewardPool.picked(offer, random))
                .toList();
        return drawn.isEmpty() ? Loot.NONE : OfficerRewardDrop.roll(drawn, run, random);
    }
}
