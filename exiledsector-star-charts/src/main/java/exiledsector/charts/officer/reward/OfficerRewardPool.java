package exiledsector.charts.officer.reward;

import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.charts.loot.ChartMaterials;
import exiledsector.items.Engram;
import exiledsector.items.Engrams;
import exiledsector.items.GrantedModifiers;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemType;
import exiledsector.items.ItemTypes;
import exiledsector.items.WeightedPick;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public final class OfficerRewardPool {

    public static final int OFFER_COUNT = 3;
    private static final List<ItemType> UNIQUE_TYPES = List.of(ItemType.HULL_ITEM, ItemType.SUBROUTINE);

    private OfficerRewardPool() {
    }

    public static List<CarriedItem> offers(RewardContext context, Random random) {
        return offers(candidates(context, random), OfficerRewardTable.shares(), OFFER_COUNT, random);
    }

    public static CarriedItem startingItem(RewardContext context, StartingItemOdds odds, Random random) {
        return startingItem(candidates(context, random), OfficerRewardTable.shares(), odds, random);
    }

    public static CarriedItem picked(CarriedItem offer, Random random) {
        boolean counted = OfficerRewardTable.categoryOf(offer.id()) == offer.kind();
        return offer.withCount(counted ? OfficerRewardTable.rollCount(offer.id(), random) : 1);
    }

    static CarriedItem startingItem(Map<RewardKind, List<RewardCandidate>> candidates, Map<RewardKind, Float> poolShares,
                                    StartingItemOdds odds, Random random) {
        List<CarriedItem> drawn = offers(odds.reweighted(candidates), odds.shares(poolShares), 1, random);
        return drawn.isEmpty() ? null : picked(drawn.get(0), random);
    }

    static List<CarriedItem> offers(Map<RewardKind, List<RewardCandidate>> candidates, Map<RewardKind, Float> shares, int count,
                                    Random random) {
        Map<RewardGroup, List<RewardCandidate>> grouped = grouped(candidates);
        Map<RewardGroup, Float> openShares = groupShares(grouped, shares);
        List<CarriedItem> offers = new ArrayList<>();
        while (offers.size() < count) {
            RewardGroup group = WeightedPick.pick(openShares, random);
            if (group == null) {
                break;
            }
            openShares.remove(group);
            offers.add(WeightedPick.pick(grouped.get(group), RewardCandidate::weight, random).item());
        }
        return List.copyOf(offers);
    }

    static Map<RewardGroup, List<RewardCandidate>> grouped(Map<RewardKind, List<RewardCandidate>> candidates) {
        Map<RewardGroup, List<RewardCandidate>> grouped = new EnumMap<>(RewardGroup.class);
        candidates.values().forEach(kindCandidates -> {
            for (RewardCandidate candidate : kindCandidates) {
                RewardGroup group = candidate.weight() > 0f ? RewardGroup.of(candidate.item()) : null;
                if (group != null) {
                    grouped.computeIfAbsent(group, unused -> new ArrayList<>()).add(candidate);
                }
            }
        });
        return grouped;
    }

    static Map<RewardGroup, Float> groupShares(Map<RewardGroup, List<RewardCandidate>> grouped, Map<RewardKind, Float> shares) {
        Map<RewardKind, Float> kindWeights = new EnumMap<>(RewardKind.class);
        grouped.forEach((group, groupCandidates) -> kindWeights.merge(group.kind(), totalWeight(groupCandidates), Float::sum));
        Map<RewardGroup, Float> groupShares = new EnumMap<>(RewardGroup.class);
        grouped.forEach((group, groupCandidates) -> {
            float share = Math.max(0f, shares.getOrDefault(group.kind(), 0f)) * totalWeight(groupCandidates) / kindWeights.get(group.kind());
            if (share > 0f) {
                groupShares.put(group, share);
            }
        });
        return groupShares;
    }

    private static float totalWeight(List<RewardCandidate> candidates) {
        float total = 0f;
        for (RewardCandidate candidate : candidates) {
            total += candidate.weight();
        }
        return total;
    }

    public static Map<RewardKind, List<RewardCandidate>> candidates(RewardContext context, Random random) {
        Map<RewardKind, List<RewardCandidate>> candidates = new EnumMap<>(RewardKind.class);
        List<ItemDefinition> baseTypes = definitions(ItemType.HULL_ITEM, false, context.itemAllowed());
        candidates.put(RewardKind.HULL_ITEM, baseTypes.stream()
                .map(definition -> new RewardCandidate(CarriedItem.of(RewardKind.HULL_ITEM, definition.id()), definition.rarity())).toList());
        candidates.put(RewardKind.BIOME_HULL_ITEM, biomeItems(baseTypes, context, random));
        candidates.put(RewardKind.CRAFTING_ITEM, craftingItems(RewardKind.CRAFTING_ITEM, context));
        candidates.put(RewardKind.INHIBITOR, craftingItems(RewardKind.INHIBITOR, context));
        candidates.put(RewardKind.ENGRAM, engrams(context));
        candidates.put(RewardKind.FEEDSTOCK, feedstock(context));
        candidates.put(RewardKind.STAR_CHART, starCharts(context));
        candidates.put(RewardKind.UNIQUE, uniques(context));
        return candidates;
    }

    public static List<CarriedItem> possible(RewardGroup group, Predicate<ItemDefinition> itemAllowed, Predicate<String> craftingItemAllowed) {
        return switch (group.kind()) {
            case CRAFTING_ITEM, INHIBITOR, FEEDSTOCK -> OfficerRewardTable.itemsIn(group.kind()).stream()
                    .filter(craftingItemAllowed)
                    .map(itemId -> CarriedItem.of(group.kind(), itemId))
                    .filter(item -> RewardGroup.of(item) == group)
                    .toList();
            case ENGRAM -> Engrams.all().stream().map(Engram::itemId)
                    .filter(itemId -> CarriedItem.isSafeId(itemId) && craftingItemAllowed.test(itemId))
                    .map(itemId -> CarriedItem.of(RewardKind.ENGRAM, itemId)).toList();
            case STAR_CHART -> ItemDefinitions.ofType(ItemType.STAR_CHART).stream()
                    .filter(definition -> ItemDrops.isDroppable(definition) && itemAllowed.test(definition) && rollsAtAnyChartTier(definition))
                    .map(definition -> CarriedItem.of(RewardKind.STAR_CHART, definition.id())).toList();
            case HULL_ITEM, BIOME_HULL_ITEM -> definitions(ItemType.HULL_ITEM, false, itemAllowed).stream()
                    .map(definition -> CarriedItem.of(group.kind(), definition.id())).toList();
            case UNIQUE -> UNIQUE_TYPES.stream().flatMap(type -> definitions(type, true, itemAllowed).stream())
                    .map(definition -> CarriedItem.of(RewardKind.UNIQUE, definition.id())).toList();
        };
    }

    public static int chartTier(int runTier) {
        return Math.min(runTier, ChartTiers.HIGHEST_TIER);
    }

    private static boolean rollsAtAnyChartTier(ItemDefinition definition) {
        return IntStream.rangeClosed(ChartTiers.LOWEST_TIER, ChartTiers.HIGHEST_TIER).anyMatch(tier -> ItemTypes.rollsAtTier(definition, tier));
    }

    private static List<ItemDefinition> definitions(ItemType type, boolean unique, Predicate<ItemDefinition> allowed) {
        return ItemDefinitions.ofType(type).stream()
                .filter(definition -> definition.unique() == unique && ItemDrops.isDroppable(definition) && allowed.test(definition))
                .toList();
    }

    private static List<RewardCandidate> biomeItems(List<ItemDefinition> baseTypes, RewardContext context, Random random) {
        List<RewardCandidate> biomeItems = new ArrayList<>();
        for (ItemDefinition definition : baseTypes) {
            String biomeId = biomeFor(definition, context, random);
            if (biomeId != null) {
                biomeItems.add(new RewardCandidate(new CarriedItem(RewardKind.BIOME_HULL_ITEM, definition.id(), biomeId, 1), definition.rarity()));
            }
        }
        return biomeItems;
    }

    static String biomeFor(ItemDefinition definition, RewardContext context, Random random) {
        String centreBiomeId = context.commandCentreBiomeId();
        String centreModifier = BiomeModifiers.modifierFor(centreBiomeId, definition.kind());
        if (centreModifier != null && GrantedModifiers.canGrant(definition, centreModifier) && CarriedItem.isSafeId(centreBiomeId)) {
            return centreBiomeId;
        }
        List<String> openBiomes = BiomeModifiers.modifiersFor(definition.kind()).stream()
                .filter(modifierId -> GrantedModifiers.canGrant(definition, modifierId))
                .map(BiomeModifiers::biomeOfModifier)
                .filter(biomeId -> CarriedItem.isSafeId(biomeId) && context.biomeOpen().test(biomeId))
                .distinct()
                .toList();
        return openBiomes.isEmpty() ? null : openBiomes.get(random.nextInt(openBiomes.size()));
    }

    private static List<RewardCandidate> craftingItems(RewardKind category, RewardContext context) {
        List<RewardCandidate> craftingItems = new ArrayList<>();
        for (String itemId : OfficerRewardTable.itemsIn(category)) {
            float amount = ChartMaterials.amount(itemId, context.tier());
            if (amount > 0f && context.craftingItemAllowed().test(itemId)) {
                craftingItems.add(new RewardCandidate(CarriedItem.of(category, itemId), amount));
            }
        }
        return craftingItems;
    }

    private static List<RewardCandidate> engrams(RewardContext context) {
        List<RewardCandidate> engrams = new ArrayList<>();
        for (Engram engram : Engrams.all()) {
            if (engram.dropsAt(context.tier()) && CarriedItem.isSafeId(engram.itemId()) && context.craftingItemAllowed().test(engram.itemId())) {
                engrams.add(new RewardCandidate(CarriedItem.of(RewardKind.ENGRAM, engram.itemId()), engram.dropWeight()));
            }
        }
        return engrams;
    }

    private static List<RewardCandidate> feedstock(RewardContext context) {
        List<RewardCandidate> feedstock = new ArrayList<>();
        for (String itemId : OfficerRewardTable.itemsIn(RewardKind.FEEDSTOCK)) {
            if (context.craftingItemAllowed().test(itemId)) {
                feedstock.add(new RewardCandidate(CarriedItem.of(RewardKind.FEEDSTOCK, itemId), OfficerRewardTable.weightOf(itemId)));
            }
        }
        return feedstock;
    }

    private static List<RewardCandidate> starCharts(RewardContext context) {
        return ItemDefinitions.ofType(ItemType.STAR_CHART).stream()
                .filter(definition -> ItemDrops.isDroppable(definition) && ItemTypes.rollsAtTier(definition, chartTier(context.tier()))
                        && context.itemAllowed().test(definition))
                .map(definition -> new RewardCandidate(CarriedItem.of(RewardKind.STAR_CHART, definition.id()), definition.rarity()))
                .toList();
    }

    private static List<RewardCandidate> uniques(RewardContext context) {
        List<RewardCandidate> uniques = new ArrayList<>();
        for (ItemType type : UNIQUE_TYPES) {
            for (ItemDefinition definition : definitions(type, true, context.itemAllowed())) {
                uniques.add(new RewardCandidate(CarriedItem.of(RewardKind.UNIQUE, definition.id()),
                        (float) ItemDrops.factionWeight(definition, context.factionId())));
            }
        }
        return uniques;
    }
}
