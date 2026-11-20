package exiledsector.charts.officer;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.RewardContext;
import exiledsector.charts.officer.reward.TrophyRefitRolls;
import exiledsector.items.drop.ItemChances;
import exiledsector.loot.DropType;
import exiledsector.loot.LootMultipliers;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Random;

public final class OfficerTrophyRefits {

    static final float LOWEST_RANK_RARITY_PERCENT = 25f;
    static final float RARITY_PERCENT_PER_RANK = 10f;
    private static final Logger LOG = Logger.getLogger(OfficerTrophyRefits.class);

    private OfficerTrophyRefits() {
    }

    public static TrophyRefit forSector(SectorAPI sector) {
        return (officer, random) -> refitted(sector, officer, random);
    }

    public static float rarityPercent(String rankId) {
        return LOWEST_RANK_RARITY_PERCENT + RARITY_PERCENT_PER_RANK * OfficerRanks.order(rankId);
    }

    private static List<CarriedItem> refitted(SectorAPI sector, AtlasOfficer officer, Random random) {
        try {
            int tier = OfficerStartingItems.tierFor(officer.rankId());
            float rarityPercent = rarityPercent(officer.rankId());
            LootMultipliers multipliers = LootMultipliers.atTierCraftingRarity(ChartTiers.craftingRarity(tier))
                    .withChances(ItemChances.BASE.plusRarityPercent(DropType.ITEM, rarityPercent));
            RewardContext context = RewardContext.forSector(sector, tier, officer.factionId(), null, multipliers);
            return TrophyRefitRolls.refitted(officer.carried(), OfficerRewardPool.candidates(context, random), rarityPercent, random);
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Refitting the trophies of Atlas officer " + officer.recordId() + " failed", e);
            return officer.carried();
        }
    }
}
