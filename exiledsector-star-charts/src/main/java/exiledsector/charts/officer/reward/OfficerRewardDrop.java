package exiledsector.charts.officer.reward;

import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.charts.loot.ChartDrops;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.items.AffixLayout;
import exiledsector.items.GrantedModifiers;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class OfficerRewardDrop {

    private OfficerRewardDrop() {
    }

    public static ItemDrops.SiteLoot roll(List<CarriedItem> carried, ChartRun run, Random random) {
        return roll(carried, run.tier(), ChartRunEffects.contextOf(run), random);
    }

    public static ItemDrops.SiteLoot roll(List<CarriedItem> carried, int runTier, ChartGenContext context, Random random) {
        List<ItemData> items = new ArrayList<>();
        Map<String, Integer> materials = new LinkedHashMap<>();
        float craftingMult = craftingMult(context);
        float feedstockMult = feedstockMult(context);
        for (CarriedItem item : carried) {
            if (item.kind().craftingItem()) {
                float mult = item.kind() == RewardKind.FEEDSTOCK ? feedstockMult : craftingMult;
                materials.merge(item.id(), Math.max(1, ItemDrops.wholeFeedstock(item.baseCount() * mult, random)), Integer::sum);
            } else {
                ItemDefinition definition = ItemDefinitions.get(item.id());
                for (int copy = 0; definition != null && copy < item.baseCount(); copy++) {
                    items.add(rolled(item, definition, runTier, random));
                }
            }
        }
        if (items.isEmpty() && materials.isEmpty()) {
            return ItemDrops.SiteLoot.NONE;
        }
        return new ItemDrops.SiteLoot(items, List.of(), materials);
    }

    public static float craftingMult(ChartGenContext context) {
        return dropMult(context, DropType.CRAFTING_ITEM);
    }

    public static float feedstockMult(ChartGenContext context) {
        return dropMult(context, DropType.FEEDSTOCK);
    }

    private static float dropMult(ChartGenContext context, DropType dropType) {
        float percent = context.itemPercent() + context.dropPercent(dropType);
        return Math.max(0f, 1f + percent / ChartGenContext.PERCENT);
    }

    private static ItemData rolled(CarriedItem item, ItemDefinition definition, int runTier, Random random) {
        long seed = random.nextLong();
        return switch (item.kind()) {
            case UNIQUE -> ItemData.rolled(definition, seed);
            case BIOME_HULL_ITEM -> withBiomeModifier(ItemData.rolledAtTier(definition, seed, runTier), definition, item.variant(), random);
            case STAR_CHART -> ItemData.rolledAtTier(chartAt(definition, runTier, random), seed, runTier);
            default -> ItemData.rolledAtTier(definition, seed, runTier);
        };
    }

    static ItemDefinition chartAt(ItemDefinition definition, int runTier, Random random) {
        if (ItemTypes.rollsAtTier(definition, runTier)) {
            return definition;
        }
        ItemDefinition replacement = ChartDrops.pickChartAtTier(random, runTier);
        return replacement == null ? definition : replacement;
    }

    private static ItemData withBiomeModifier(ItemData item, ItemDefinition definition, String biomeId, Random random) {
        String modifierId = BiomeModifiers.modifierFor(biomeId, definition.kind());
        if (modifierId == null || !GrantedModifiers.canGrant(definition, modifierId)) {
            return item;
        }
        return GrantedModifiers.add(item, modifierId, AffixLayout.dropLimits(definition.type()), random);
    }
}
