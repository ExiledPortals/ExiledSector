package exiledsector.charts.officer;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.RewardContext;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Random;

public final class OfficerStartingItems {

    private static final Logger LOG = Logger.getLogger(OfficerStartingItems.class);

    private OfficerStartingItems() {
    }

    public static StartingItemRoll forSector(SectorAPI sector) {
        return (officer, random) -> rolled(sector, officer, random);
    }

    public static AtlasOfficer dealt(AtlasOfficer officer, StartingItemRoll roll, Random random) {
        CarriedItem startingItem = roll.roll(officer, random);
        return startingItem == null ? officer : officer.withCarried(CarriedItem.stack(officer.carried(), List.of(startingItem)));
    }

    static int tierFor(String rankId) {
        int rankCount = OfficerRanks.all().size();
        if (rankCount <= 1) {
            return ChartTiers.LOWEST_TIER;
        }
        float rankShare = OfficerRanks.order(rankId) / (float) (rankCount - 1);
        return ChartTiers.LOWEST_TIER + Math.round(rankShare * (ChartTiers.HIGHEST_TIER - ChartTiers.LOWEST_TIER));
    }

    private static CarriedItem rolled(SectorAPI sector, AtlasOfficer officer, Random random) {
        try {
            RewardContext context = RewardContext.forSector(sector, tierFor(officer.rankId()), officer.factionId(), null);
            return OfficerRewardPool.startingItem(context, OfficerRanks.startingOdds(officer.rankId()), random);
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Rolling the starting item of Atlas officer " + officer.recordId() + " failed", e);
            return null;
        }
    }
}
