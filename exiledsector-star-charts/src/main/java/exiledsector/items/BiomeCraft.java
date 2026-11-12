package exiledsector.items;

import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.tags.TagWeights;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public enum BiomeCraft {

    ADAPTIVE(100, 1),
    GRAFTING(1000, 2);

    static final int MIN_OTHER_MODIFIERS = 1;
    static final int MAX_OTHER_MODIFIERS = 3;
    private static final String SHARED_REFUSAL_PREFIX = "ui.infusedCraft.refused.";
    private static final String REFUSAL_PREFIX = "chart.biomeCraft.refused.";

    private final int feedstockCost;
    private final int reshapingCost;

    BiomeCraft(int feedstockCost, int reshapingCost) {
        this.feedstockCost = feedstockCost;
        this.reshapingCost = reshapingCost;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int feedstockCost() {
        return feedstockCost;
    }

    public int reshapingCost() {
        return reshapingCost;
    }

    public static List<String> feedstockIds() {
        return Arrays.stream(InfusedFeedstock.values()).map(InfusedFeedstock::itemId).toList();
    }

    public UseVerdict verdict(Item item, Collection<CraftingInhibitor> inhibitors) {
        if (item == null) {
            return UseVerdict.refused(SHARED_REFUSAL_PREFIX + "loadFirst");
        }
        ItemDefinition definition = item.definition();
        if (definition == null || definition.type() != ItemType.HULL_ITEM || !ItemTypes.takesCraftingItem(definition.type())) {
            return UseVerdict.refused(SHARED_REFUSAL_PREFIX + "itemType");
        }
        if (item.mirrored()) {
            return UseVerdict.refused(SHARED_REFUSAL_PREFIX + "mirrored");
        }
        if (item.corrupted()) {
            return UseVerdict.refused(SHARED_REFUSAL_PREFIX + "corrupted");
        }
        if (ItemCrafting.isLockedFromCraftingItem(definition)) {
            return UseVerdict.refused(SHARED_REFUSAL_PREFIX + "locked");
        }
        if (possible(item, CraftingInhibitor.inhibitedSlots(inhibitors))) {
            return UseVerdict.ALLOW;
        }
        return possible(item, Set.of()) ? UseVerdict.refused(SHARED_REFUSAL_PREFIX + "inhibited") : UseVerdict.refused(REFUSAL_PREFIX + id());
    }

    public Set<CraftingInhibitor> inhibitorsSpent(Item item, Collection<CraftingInhibitor> inhibitors) {
        Set<CraftingInhibitor> spent = EnumSet.noneOf(CraftingInhibitor.class);
        if (item == null || item.definition() == null) {
            return spent;
        }
        if (this == ADAPTIVE) {
            spent.addAll(CraftingInhibitor.favouring(inhibitors));
        }
        for (CraftingInhibitor inhibitor : inhibitors) {
            if (inhibitor.inhibitedSlot() != null && item.explicits().stream().anyMatch(rolled -> rolled.slot() == inhibitor.inhibitedSlot() && !rolled.immutable()
                    && wouldChange(item, rolled))) {
                spent.add(inhibitor);
            }
        }
        return spent;
    }

    public boolean canAfford(CraftingMaterials materials) {
        return feedstockIds().stream().allMatch(feedstockId -> materials.count(feedstockId) >= feedstockCost)
                && materials.count(InfusedCraft.reshapingId()) >= reshapingCost;
    }

    public Optional<Set<CraftingInhibitor>> use(Item item, CraftingMaterials materials, Collection<CraftingInhibitor> inhibitors, Random random) {
        if (!canAfford(materials) || inhibitors.stream().anyMatch(inhibitor -> materials.count(inhibitor.itemId()) < 1)
                || !verdict(item, inhibitors).allowed()) {
            return Optional.empty();
        }
        Set<CraftingInhibitor> spent = inhibitorsSpent(item, inhibitors);
        Set<ModifierSlot> inhibited = CraftingInhibitor.inhibitedSlots(inhibitors);
        List<RolledModifier> replacement = switch (this) {
            case ADAPTIVE -> adapt(item, inhibited, CraftingInhibitor.favouredTags(inhibitors), random);
            case GRAFTING -> graft(item, inhibited, random);
        };
        item.replaceExplicits(replacement);
        item.refreezeName();
        feedstockIds().forEach(feedstockId -> materials.take(feedstockId, feedstockCost));
        materials.take(InfusedCraft.reshapingId(), reshapingCost);
        spent.forEach(inhibitor -> materials.take(inhibitor.itemId(), 1));
        return Optional.of(spent);
    }

    private boolean possible(Item item, Set<ModifierSlot> inhibited) {
        return switch (this) {
            case ADAPTIVE -> !adaptiveGrants(item, inhibited).isEmpty();
            case GRAFTING -> !graftable(item, inhibited).isEmpty();
        };
    }

    boolean wouldChange(Item item, RolledModifier rolled) {
        return switch (this) {
            case ADAPTIVE -> true;
            case GRAFTING -> !graftsAfterRemoving(item, rolled).isEmpty();
        };
    }

    static List<String> adaptiveGrants(Item item, Set<ModifierSlot> inhibited) {
        ItemDefinition definition = item.definition();
        if (definition == null) {
            return List.of();
        }
        List<RolledModifier> kept = Engram.keptModifiers(item, inhibited);
        return BiomeModifiers.modifiersFor(definition.kind()).stream()
                .filter(modifierId -> GrantedModifiers.canGrant(definition, modifierId))
                .filter(modifierId -> BiomeModifiers.hasRoomFor(kept, modifierId))
                .filter(modifierId -> !Engram.keptClash(item, ItemModifiers.get(modifierId), inhibited))
                .toList();
    }

    static List<RolledModifier> adapt(Item item, Set<ModifierSlot> inhibited, Random random) {
        return adapt(item, inhibited, TagWeights.NONE, random);
    }

    static List<RolledModifier> adapt(Item item, Set<ModifierSlot> inhibited, TagWeights favouredTags, Random random) {
        ItemDefinition definition = item.definition();
        List<String> grants = adaptiveGrants(item, inhibited);
        Modifier granted = ItemModifiers.get(grants.get(random.nextInt(grants.size())));
        List<RolledModifier> modifiers = new ArrayList<>(Engram.keptModifiers(item, inhibited));
        if (modifiers.stream().noneMatch(rolled -> rolled.modifierId().equals(granted.id()))) {
            AffixLayout.insert(modifiers, RolledModifier.roll(granted, random));
        }
        int otherCount = MIN_OTHER_MODIFIERS + random.nextInt(MAX_OTHER_MODIFIERS - MIN_OTHER_MODIFIERS + 1);
        int targetCount = Math.min(AffixLayout.limits(definition.type()).maxModifiers(), 1 + otherCount);
        while (modifiers.size() < targetCount) {
            List<List<PoolEntry>> openSides = AffixLayout.openSides(definition, modifiers);
            if (openSides.isEmpty()) {
                break;
            }
            List<PoolEntry> side = openSides.get(random.nextInt(openSides.size()));
            PoolEntry entry = side.get(WeightedPick.index(side, candidate -> candidate.biasedWeight(favouredTags), random.nextFloat()));
            AffixLayout.insert(modifiers, RolledModifier.roll(entry.modifier(), random));
        }
        return modifiers;
    }

    static List<RolledModifier> graftable(Item item, Set<ModifierSlot> inhibited) {
        return item.explicits().stream()
                .filter(rolled -> !rolled.immutable() && !inhibited.contains(rolled.slot()))
                .filter(rolled -> !graftsAfterRemoving(item, rolled).isEmpty())
                .toList();
    }

    static List<String> graftsAfterRemoving(Item item, RolledModifier removed) {
        ItemDefinition definition = item == null ? null : item.definition();
        if (definition == null || removed == null || removed.immutable()) {
            return List.of();
        }
        List<RolledModifier> remaining = remainingAfter(item, removed);
        AffixLayout.Limits limits = AffixLayout.limits(definition.type());
        Set<String> remainingIds = remaining.stream().map(RolledModifier::modifierId).collect(Collectors.toSet());
        Set<String> remainingEffects = remaining.stream().flatMap(rolled -> rolled.lines().stream()).map(RolledLine::effectName)
                .collect(Collectors.toSet());
        return BiomeModifiers.modifiersFor(definition.kind()).stream()
                .filter(modifierId -> !modifierId.equals(removed.modifierId()) && !remainingIds.contains(modifierId))
                .filter(modifierId -> GrantedModifiers.canGrant(definition, modifierId))
                .filter(modifierId -> BiomeModifiers.hasRoomFor(remaining, modifierId))
                .filter(modifierId -> {
                    Modifier granted = ItemModifiers.get(modifierId);
                    long sameSide = remaining.stream().filter(rolled -> rolled.slot() == granted.slot()).count();
                    return sameSide < limits.maxPerSide() && remaining.size() < limits.maxModifiers()
                            && granted.lines().stream().map(ModifierLine::effectName).noneMatch(remainingEffects::contains)
                            && !ModifierExclusivity.clashes(modifierId, remainingIds);
                })
                .toList();
    }

    static List<RolledModifier> graft(Item item, Set<ModifierSlot> inhibited, Random random) {
        List<RolledModifier> removable = graftable(item, inhibited);
        RolledModifier removed = removable.get(random.nextInt(removable.size()));
        List<String> grafts = graftsAfterRemoving(item, removed);
        Modifier granted = ItemModifiers.get(grafts.get(random.nextInt(grafts.size())));
        List<RolledModifier> modifiers = remainingAfter(item, removed);
        AffixLayout.insert(modifiers, RolledModifier.roll(granted, random));
        return modifiers;
    }

    private static List<RolledModifier> remainingAfter(Item item, RolledModifier removed) {
        List<RolledModifier> remaining = new ArrayList<>(item.explicits());
        remaining.remove(removed);
        return remaining;
    }
}
