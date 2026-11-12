package exiledsector.charts.officer.reward;

import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.charts.loot.ChartMaterials;
import exiledsector.items.Engram;
import exiledsector.items.Engrams;
import exiledsector.items.GrantedModifiers;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemType;
import exiledsector.items.WeightedPick;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
        if (offer.kind() != RewardKind.CRAFTING_ITEM && offer.kind() != RewardKind.INHIBITOR) {
            return offer.withCount(1);
        }
        CraftingTier tier = OfficerRewardTable.tierOf(offer.id());
        return offer.withCount(tier == null ? 1 : tier.rollCount(random));
    }

    static CarriedItem startingItem(Map<RewardKind, List<RewardCandidate>> candidates, Map<RewardKind, Float> poolShares,
                                    StartingItemOdds odds, Random random) {
        List<CarriedItem> drawn = offers(odds.reweighted(candidates), odds.shares(poolShares), 1, random);
        return drawn.isEmpty() ? null : picked(drawn.get(0), random);
    }

    static List<CarriedItem> offers(Map<RewardKind, List<RewardCandidate>> candidates, Map<RewardKind, Float> shares, int count,
                                    Random random) {
        Map<RewardKind, List<RewardCandidate>> remaining = new EnumMap<>(RewardKind.class);
        candidates.forEach((kind, kindCandidates) -> remaining.put(kind,
                new ArrayList<>(kindCandidates.stream().filter(candidate -> candidate.weight() > 0f).toList())));
        List<CarriedItem> offers = new ArrayList<>();
        while (offers.size() < count) {
            Map<RewardKind, Float> openShares = new EnumMap<>(RewardKind.class);
            remaining.forEach((kind, kindCandidates) -> {
                if (!kindCandidates.isEmpty()) {
                    openShares.put(kind, shares.getOrDefault(kind, 0f));
                }
            });
            RewardKind kind = WeightedPick.pick(openShares, random);
            if (kind == null) {
                break;
            }
            List<RewardCandidate> kindCandidates = remaining.get(kind);
            offers.add(kindCandidates.remove(WeightedPick.index(kindCandidates, RewardCandidate::weight, random.nextFloat())).item());
        }
        return List.copyOf(offers);
    }

    public static Map<RewardKind, List<RewardCandidate>> candidates(RewardContext context, Random random) {
        Map<RewardKind, List<RewardCandidate>> candidates = new EnumMap<>(RewardKind.class);
        List<ItemDefinition> baseTypes = baseTypes(context);
        candidates.put(RewardKind.HULL_ITEM, baseTypes.stream()
                .map(definition -> new RewardCandidate(CarriedItem.of(RewardKind.HULL_ITEM, definition.id()), definition.rarity())).toList());
        candidates.put(RewardKind.BIOME_HULL_ITEM, biomeItems(baseTypes, context, random));
        candidates.put(RewardKind.CRAFTING_ITEM, craftingItems(RewardKind.CRAFTING_ITEM, context));
        candidates.put(RewardKind.INHIBITOR, craftingItems(RewardKind.INHIBITOR, context));
        candidates.put(RewardKind.ENGRAM, engrams(context));
        candidates.put(RewardKind.UNIQUE, uniques(context));
        return candidates;
    }

    private static List<ItemDefinition> baseTypes(RewardContext context) {
        return ItemDefinitions.ofType(ItemType.HULL_ITEM).stream()
                .filter(definition -> !definition.unique() && ItemDrops.isDroppable(definition) && context.itemAllowed().test(definition))
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

    private static List<RewardCandidate> uniques(RewardContext context) {
        List<RewardCandidate> uniques = new ArrayList<>();
        for (ItemType type : UNIQUE_TYPES) {
            for (ItemDefinition definition : ItemDefinitions.ofType(type)) {
                if (definition.unique() && ItemDrops.isDroppable(definition) && context.itemAllowed().test(definition)) {
                    uniques.add(new RewardCandidate(CarriedItem.of(RewardKind.UNIQUE, definition.id()),
                            (float) ItemDrops.factionWeight(definition, context.factionId())));
                }
            }
        }
        return uniques;
    }
}
