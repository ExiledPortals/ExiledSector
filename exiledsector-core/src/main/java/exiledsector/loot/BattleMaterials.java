package exiledsector.loot;

import exiledsector.items.CraftingInhibitor;
import exiledsector.items.CraftingMaterialFamilies;
import exiledsector.items.ItemDrops;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

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
            float bonuses = rules.feedstockOnly() ? 1f : loss.difficultyMultiplier() * rules.multipliers().feedstockMult();
            feedstock += loss.deploymentPoints() * ItemDrops.battleFeedstockPerDeploymentPoint() * bonuses;
        }
        return feedstock;
    }

    public Map<String, Integer> rollMaterials(Random random) {
        float battleDeploymentPoints = 0f;
        for (Loss loss : losses) {
            battleDeploymentPoints += loss.deploymentPoints();
        }
        Map<String, Integer> materials = new LinkedHashMap<>();
        for (BattleLootRates.Rate rate : materialRates()) {
            float expected = 0f;
            for (Loss loss : losses) {
                if (!loss.rules().sourceRollsMaterials()) {
                    expected += expected(rate, loss.deploymentPoints(), battleDeploymentPoints, loss.difficultyMultiplier(), loss.rules());
                }
            }
            addCount(materials, rate, expected, random);
        }
        return materials;
    }

    public static Map<String, Integer> roll(float lootDeploymentPoints, float battleDeploymentPoints, float difficultyMultiplier,
                                            FleetLootRules rules, Random random) {
        return roll(lootDeploymentPoints, battleDeploymentPoints, difficultyMultiplier, rules, drop -> true, random);
    }

    public static Map<String, Integer> roll(float lootDeploymentPoints, float battleDeploymentPoints, float difficultyMultiplier,
                                            FleetLootRules rules, Predicate<String> rolledDrops, Random random) {
        Map<String, Integer> materials = new LinkedHashMap<>();
        for (BattleLootRates.Rate rate : materialRates()) {
            if (rolledDrops.test(rate.drop())) {
                addCount(materials, rate, expected(rate, lootDeploymentPoints, battleDeploymentPoints, difficultyMultiplier, rules), random);
            }
        }
        return materials;
    }

    private static float expected(BattleLootRates.Rate rate, float deploymentPoints, float battleDeploymentPoints, float difficultyMultiplier,
                                  FleetLootRules rules) {
        return rate.expected(deploymentPoints * rules.deploymentPointsMult(), battleDeploymentPoints)
                * BattleLootRates.difficulty(difficultyMultiplier) * rules.share(rate) * rules.multipliers().currencyMult();
    }

    private static void addCount(Map<String, Integer> materials, BattleLootRates.Rate rate, float expected, Random random) {
        int count = ItemDrops.wholeFeedstock(expected, random);
        if (count > 0) {
            materials.merge(rate.drop(), count, Integer::sum);
        }
    }

    private static List<BattleLootRates.Rate> materialRates() {
        return BattleLootRates.all().stream().filter(rate -> isMaterial(rate.drop())).toList();
    }

    public static boolean isMaterial(String itemId) {
        return CraftingMaterialFamilies.byItemId(itemId) != null || CraftingInhibitor.byItemId(itemId) != null;
    }
}
