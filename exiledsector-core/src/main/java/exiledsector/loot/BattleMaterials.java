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
            losses.add(new Loss(deploymentPoints, difficultyMultiplier, rules == null ? FleetLootRules.REGULAR : rules));
        }
    }

    public float feedstock() {
        float feedstock = 0f;
        for (Loss loss : losses) {
            FleetLootRules rules = loss.rules();
            if (rules.paysNothing()) {
                continue;
            }
            float bonuses = rules.feedstockOnly() ? 1f : loss.difficultyMultiplier() * rules.multipliers().feedstockMult();
            feedstock += loss.deploymentPoints() * ItemDrops.battleFeedstockPerDeploymentPoint() * bonuses;
        }
        return feedstock;
    }

    public Map<String, Integer> rollMaterials(Random random) {
        float expectedCount = 0f;
        for (Loss loss : losses) {
            FleetLootRules rules = loss.rules();
            if (!rules.sourceRollsMaterials()) {
                expectedCount += CraftingDrops.expectedCount(loss.deploymentPoints() * rules.deploymentPointsMult()) * rules.share()
                        * rules.multipliers().craftingItemMult();
            }
        }
        return CraftingDrops.roll(expectedCount * FoundItemQuantity.craftingItemMult(), CraftingDrops.OUTSIDE_CHARTS, CraftingDrops.NO_BAND_SHIFT,
                random);
    }
}
