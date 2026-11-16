package exiledsector.charts.officer.reward;

import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.charts.loot.ChartDrops;
import exiledsector.charts.loot.ChartLootContext;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.run.ChartRun;
import exiledsector.data.RandomRounding;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemRoll;
import exiledsector.items.ItemTypes;
import exiledsector.loot.Loot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class OfficerRewardDrop {

    private OfficerRewardDrop() {
    }

    public static Loot roll(List<CarriedItem> carried, ChartRun run, Random random) {
        return roll(carried, ChartLootContext.of(run), random);
    }

    public static Loot roll(List<CarriedItem> carried, ChartLootContext loot, Random random) {
        List<ItemData> items = new ArrayList<>();
        Map<String, Integer> materials = new LinkedHashMap<>();
        float craftingMult = loot.carriedDropMult(DropType.CRAFTING_ITEM);
        float feedstockMult = loot.carriedDropMult(DropType.FEEDSTOCK);
        for (CarriedItem item : carried) {
            if (item.kind().craftingItem()) {
                float mult = item.kind() == RewardKind.FEEDSTOCK ? feedstockMult : craftingMult;
                materials.merge(item.id(), Math.max(1, RandomRounding.round(item.baseCount() * mult, random)), Integer::sum);
            } else {
                ItemDefinition definition = ItemDefinitions.get(item.id());
                for (int copy = 0; definition != null && copy < item.baseCount(); copy++) {
                    items.add(rolled(item, definition, loot, random));
                }
            }
        }
        if (items.isEmpty() && materials.isEmpty()) {
            return Loot.NONE;
        }
        return new Loot(items, List.of(), materials);
    }

    private static ItemData rolled(CarriedItem item, ItemDefinition definition, ChartLootContext loot, Random random) {
        long seed = random.nextLong();
        return switch (item.kind()) {
            case UNIQUE -> ItemData.rolled(definition, seed);
            case BIOME_HULL_ITEM -> BiomeModifiers.grant(loot.rolledWithDropLimits(definition, seed), item.variant(), random);
            case STAR_CHART -> {
                int chartTier = ChartTiers.clampDropTier(loot.tier());
                yield ItemData.rolled(chartAt(definition, chartTier, random), seed, ItemRoll.atTier(chartTier));
            }
            default -> loot.rolledWithDropLimits(definition, seed);
        };
    }

    static ItemDefinition chartAt(ItemDefinition definition, int runTier, Random random) {
        if (ItemTypes.rollsAtTier(definition, runTier)) {
            return definition;
        }
        ItemDefinition replacement = ChartDrops.pickChartAtTier(random, runTier);
        return replacement == null ? definition : replacement;
    }
}
