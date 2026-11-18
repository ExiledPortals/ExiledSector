package exiledsector.charts.officer;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.RewardContext;
import exiledsector.loot.LootMultipliers;
import org.apache.log4j.Logger;

import java.util.Random;

public final class OfficerScavenging {

    private static final Logger LOG = Logger.getLogger(OfficerScavenging.class);

    private OfficerScavenging() {
    }

    public static ScavengeRoll forSector(SectorAPI sector) {
        return (officer, random) -> rolled(sector, officer, random);
    }

    private static CarriedItem rolled(SectorAPI sector, AtlasOfficer officer, Random random) {
        try {
            int tier = OfficerStartingItems.tierFor(officer.rankId());
            String commandCentreBiomeId = officer.isActive() ? AtlasRuntime.generator(sector).node(officer.commandCentre()).regionId() : null;
            RewardContext context = RewardContext.forSector(sector, tier, officer.factionId(), commandCentreBiomeId,
                    LootMultipliers.atTierCraftingRarity(ChartTiers.craftingRarity(tier)));
            return OfficerRewardPool.startingItem(context, OfficerRanks.startingOdds(officer.rankId()), random);
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Rolling the scavenged item of Atlas officer " + officer.recordId() + " failed", e);
            return null;
        }
    }
}
