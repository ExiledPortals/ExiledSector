package exiledsector.loot;

import exiledsector.items.CraftingDrops;
import exiledsector.items.ItemDrops;
import exiledsector.skills.skilleffect.logistics.FoundItemQuantity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class BattleMaterials {

    private record Loss(float deploymentPoints, float difficultyMultiplier, FleetLootRules rules) {
    }

    private final List<Loss> losses = new ArrayList<>();

    public void record(float deploymentPoints, float difficultyMultiplier, FleetLootRules rules) {
        if (deploymentPoints > 0f) {
            losses.add(new Loss(deploymentPoints, difficultyMultiplier, rules == null ? FleetLootRules.LEVEL_SCALED : rules));
        }
    }

    public float feedstock() {
        float feedstock = 0f;
        for (Loss loss : losses) {
            FleetLootRules rules = loss.rules();
            if (rules.kind() == FleetLootRules.Kind.SPENT) {
                continue;
            }
            float bonuses = rules.kind() == FleetLootRules.Kind.FEEDSTOCK_ONLY ? 1f
                    : loss.difficultyMultiplier() * rules.multipliers().feedstockMult();
            feedstock += loss.deploymentPoints() * ItemDrops.battleFeedstockPerDeploymentPoint() * bonuses;
        }
        return feedstock;
    }

    public Map<String, Integer> rollCraftingItems(float levelScaledShare, Random random) {
        float expectedCount = 0f;
        for (Loss loss : losses) {
            FleetLootRules rules = loss.rules();
            if (rules.kind().rollsBattleCraftingItems()) {
                expectedCount += CraftingDrops.expectedCount(loss.deploymentPoints() * rules.deploymentPointsMult())
                        * rules.itemShare(levelScaledShare) * rules.multipliers().craftingItemMult();
            }
        }
        return CraftingDrops.roll(expectedCount * FoundItemQuantity.craftingItemMult(), CraftingDrops.OUTSIDE_CHARTS, CraftingDrops.NO_BAND_SHIFT,
                random);
    }
}
