package exiledsector.items;

import exiledsector.loot.BattleLootRates;
import exiledsector.loot.FleetLootRules;

import java.util.List;
import java.util.Random;

public final class LogisticsSchematicDrops {

    public static final String RATE_ID = "logistics_schematic";
    static final RefitSchematic MATCHED_TIER = RefitSchematic.MILITARY;

    public record SourceLoss(float deploymentPoints, float difficultyMultiplier, FleetLootRules rules) {

        public SourceLoss {
            deploymentPoints = Math.max(0f, deploymentPoints);
            rules = rules == null ? FleetLootRules.REGULAR : rules;
        }
    }

    private LogisticsSchematicDrops() {
    }

    public static int rollBattle(List<SourceLoss> sources, float dropMultiplier, Random random) {
        return ItemDrops.wholeFeedstock(expectedBattleDrops(sources, BattleLootRates.rate(RATE_ID), dropMultiplier), random);
    }

    static float expectedBattleDrops(List<SourceLoss> sources, BattleLootRates.Rate rate, float dropMultiplier) {
        if (rate == null || sources.isEmpty() || !(dropMultiplier > 0f)) {
            return 0f;
        }
        float battleDeploymentPoints = 0f;
        for (SourceLoss source : sources) {
            battleDeploymentPoints += source.deploymentPoints();
        }
        float expected = 0f;
        for (SourceLoss source : sources) {
            FleetLootRules rules = source.rules();
            expected += rate.expected(source.deploymentPoints() * rules.deploymentPointsMult(), battleDeploymentPoints)
                    * rate.difficulty(source.difficultyMultiplier()) * rules.share(rate) * rules.multipliers().itemChances().general();
        }
        return expected * dropMultiplier;
    }

    public static boolean rollSiteFind(String siteId, Random random, ItemChances chances) {
        ItemDrops.Rule rule = ItemDrops.activeRule(siteId);
        return rule != null && !rule.chances().isEmpty() && random.nextFloat() < siteFindChance(rule.chances().get(0), chances.general());
    }

    static float siteFindChance(float firstItemChance, float generalChanceMult) {
        return firstItemChance * generalChanceMult * matchedTierShare();
    }

    static float matchedTierShare() {
        float totalWeight = 0f;
        for (RefitSchematic tier : RefitSchematic.values()) {
            totalWeight += tier.dropWeight();
        }
        return totalWeight > 0f ? MATCHED_TIER.dropWeight() / totalWeight : 0f;
    }
}
