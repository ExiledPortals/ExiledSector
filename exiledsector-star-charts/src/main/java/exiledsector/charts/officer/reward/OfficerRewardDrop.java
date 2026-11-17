package exiledsector.charts.officer.reward;

import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.charts.loot.ChartDrops;
import exiledsector.charts.loot.ChartLootContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.data.RandomRounding;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemRoll;
import exiledsector.items.ItemTypes;
import exiledsector.loot.DropType;
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
        for (CarriedItem item : carried) {
            if (item.kind().material()) {
                int count = item.hasFinalCount() ? item.baseCount() : droppedCount(item, loot.quantityMult(materialDropType(item)), random);
                materials.merge(item.id(), count, Integer::sum);
                continue;
            }
            ItemDefinition definition = ItemDefinitions.get(item.id());
            if (definition == null) {
                continue;
            }
            int count = keepsBaseCount(item.kind(), definition) ? item.baseCount()
                    : droppedCount(item, itemQuantityMult(item.kind(), definition, loot), random);
            for (int copy = 0; copy < count; copy++) {
                items.add(rolled(item, definition, loot, random));
            }
        }
        if (items.isEmpty() && materials.isEmpty()) {
            return Loot.NONE;
        }
        return new Loot(items, List.of(), materials);
    }

    private static int droppedCount(CarriedItem item, float quantityMult, Random random) {
        return Math.max(1, RandomRounding.round(item.baseCount() * quantityMult, random));
    }

    private static boolean keepsBaseCount(RewardKind kind, ItemDefinition definition) {
        return kind == RewardKind.UNIQUE && definition.kind() != ItemKind.SUBROUTINE;
    }

    private static DropType materialDropType(CarriedItem item) {
        return switch (item.kind()) {
            case FEEDSTOCK -> DropType.FEEDSTOCK;
            case ENGRAM -> DropType.ENGRAM;
            default -> DropType.forCraftingItem(item.id());
        };
    }

    private static float itemQuantityMult(RewardKind kind, ItemDefinition definition, ChartLootContext loot) {
        DropType type = kind == RewardKind.STAR_CHART ? DropType.STAR_CHART : DropType.forItemKind(definition.kind());
        return type == null ? 1f : loot.quantityMult(type);
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
