package exiledsector.charts.officer.reward;

import exiledsector.data.WeightedPick;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public final class TrophyRefitRolls {

    static final float BASE_UNIQUE_SHARE = 1f / 12f;

    private TrophyRefitRolls() {
    }

    public static List<CarriedItem> refitted(List<CarriedItem> carried, Map<RewardKind, List<RewardCandidate>> candidates, float rarityPercent,
                                             Random random) {
        float uniqueChance = Math.min(1f, BASE_UNIQUE_SHARE * Math.max(0f, 1f + rarityPercent / 100f));
        List<CarriedItem> refitted = new ArrayList<>();
        for (CarriedItem item : carried) {
            for (int copy = 0; copy < item.baseCount(); copy++) {
                stack(refitted, rerolled(item.withCount(1), candidates, uniqueChance, random));
            }
        }
        return List.copyOf(refitted);
    }

    static CarriedItem rerolled(CarriedItem item, Map<RewardKind, List<RewardCandidate>> candidates, float uniqueChance, Random random) {
        return switch (item.kind()) {
            case UNIQUE, BIOME_HULL_ITEM -> item;
            case HULL_ITEM -> asUnique(item, candidates.getOrDefault(RewardKind.UNIQUE, List.of()), uniqueChance, random);
            case CRAFTING_ITEM -> sameKind(item, candidates, candidate -> atLeastTier(candidate, OfficerRewardTable.tierOf(item.id())), random);
            default -> sameKind(item, candidates, candidate -> true, random);
        };
    }

    private static CarriedItem asUnique(CarriedItem item, List<RewardCandidate> uniques, float uniqueChance, Random random) {
        ItemDefinition base = ItemDefinitions.get(item.id());
        if (base == null || random.nextFloat() >= uniqueChance) {
            return item;
        }
        List<RewardCandidate> sameSection = uniques.stream().filter(candidate -> {
            ItemDefinition unique = ItemDefinitions.get(candidate.item().id());
            return unique != null && unique.kind() == base.kind();
        }).toList();
        RewardCandidate picked = WeightedPick.pick(sameSection, RewardCandidate::weight, random);
        return picked == null ? item : picked.item();
    }

    private static CarriedItem sameKind(CarriedItem item, Map<RewardKind, List<RewardCandidate>> candidates,
                                        Predicate<CarriedItem> allowed, Random random) {
        List<RewardCandidate> options = candidates.getOrDefault(item.kind(), List.of()).stream().filter(candidate -> allowed.test(candidate.item()))
                .toList();
        RewardCandidate picked = WeightedPick.pick(options, RewardCandidate::weight, random);
        if (picked == null) {
            return item;
        }
        return item.hasFinalCount() ? CarriedItem.withFinalCount(item.kind(), picked.item().id(), 1) : picked.item().withCount(1);
    }

    private static boolean atLeastTier(CarriedItem candidate, CraftingTier lowest) {
        CraftingTier tier = OfficerRewardTable.tierOf(candidate.id());
        return lowest == null || tier != null && tier.compareTo(lowest) >= 0;
    }

    static void stack(List<CarriedItem> stacks, CarriedItem item) {
        for (int index = 0; index < stacks.size(); index++) {
            CarriedItem stacked = stacks.get(index);
            if (stacked.sameItemAs(item)) {
                stacks.set(index, stacked.withCount(stacked.baseCount() + item.baseCount()));
                return;
            }
        }
        stacks.add(item);
    }
}
