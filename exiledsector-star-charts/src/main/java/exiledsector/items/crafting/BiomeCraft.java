package exiledsector.items.crafting;

import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.items.Item;
import exiledsector.items.ItemDefinition;
import exiledsector.items.UseVerdict;
import exiledsector.items.crafting.material.CraftingInhibitor;
import exiledsector.items.crafting.material.Engram;
import exiledsector.items.crafting.material.InfusedFeedstock;
import exiledsector.items.modifier.AffixLayout;
import exiledsector.items.modifier.GrantedModifiers;
import exiledsector.items.modifier.ItemModifiers;
import exiledsector.items.modifier.ModifierExclusivity;
import exiledsector.items.modifier.PoolEntry;
import exiledsector.items.modifier.RolledLine;
import exiledsector.items.modifier.RolledModifier;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.tags.TagWeights;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public enum BiomeCraft implements HullItemCraft {

    ADAPTIVE(100, 1),
    GRAFTING(1000, 2);

    static final int MIN_OTHER_MODIFIERS = 1;
    static final int MAX_OTHER_MODIFIERS = 3;
    private static final String REFUSAL_PREFIX = "chart.biomeCraft.refused.";

    private final int feedstockCost;
    private final int reshapingCost;

    BiomeCraft(int feedstockCost, int reshapingCost) {
        this.feedstockCost = feedstockCost;
        this.reshapingCost = reshapingCost;
    }

    @Override
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public CraftCost cost() {
        return new CraftCost(feedstockIds(), feedstockCost, reshapingCost);
    }

    public static List<String> feedstockIds() {
        return Arrays.stream(InfusedFeedstock.values()).map(InfusedFeedstock::itemId).toList();
    }

    @Override
    public UseVerdict verdict(Item item, Collection<CraftingInhibitor> inhibitors) {
        UseVerdict itemRefusal = HullItemCraftRefusals.ofItem(item);
        if (!itemRefusal.allowed()) {
            return itemRefusal;
        }
        return HullItemCraftRefusals.ofInhibitors(inhibited -> possible(item, inhibited), CraftingInhibitor.inhibitedSlots(inhibitors), REFUSAL_PREFIX + id());
    }

    @Override
    public Set<CraftingInhibitor> inhibitorsSpent(Item item, Collection<CraftingInhibitor> inhibitors) {
        return CraftingInhibitor.spentOn(item, inhibitors, this == ADAPTIVE, rolled -> wouldChange(item, rolled));
    }

    @Override
    public boolean changeModifiers(Item item, Collection<CraftingInhibitor> inhibitors, Random random) {
        Set<ModifierSlot> inhibited = CraftingInhibitor.inhibitedSlots(inhibitors);
        List<RolledModifier> replacement = switch (this) {
            case ADAPTIVE -> adapt(item, inhibited, CraftingInhibitor.tagWeights(inhibitors), random);
            case GRAFTING -> graft(item, inhibited, random);
        };
        item.replaceExplicits(replacement);
        item.refreezeName();
        return true;
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

    static List<RolledModifier> adapt(Item item, Set<ModifierSlot> inhibited, TagWeights tagWeights, Random random) {
        ItemDefinition definition = item.definition();
        List<String> grants = adaptiveGrants(item, inhibited);
        if (definition == null || grants.isEmpty()) {
            return item.explicits();
        }
        Modifier granted = ItemModifiers.get(grants.get(random.nextInt(grants.size())));
        List<RolledModifier> modifiers = new ArrayList<>(Engram.keptModifiers(item, inhibited));
        if (granted != null && modifiers.stream().noneMatch(rolled -> rolled.modifierId().equals(granted.id()))) {
            AffixLayout.insert(modifiers, RolledModifier.roll(granted, random));
        }
        int otherCount = MIN_OTHER_MODIFIERS + random.nextInt(MAX_OTHER_MODIFIERS - MIN_OTHER_MODIFIERS + 1);
        int targetCount = Math.min(AffixLayout.limits(definition.type()).maxModifiers(), 1 + otherCount);
        while (modifiers.size() < targetCount) {
            List<List<PoolEntry>> openSides = AffixLayout.openSides(definition, modifiers);
            if (openSides.isEmpty()) {
                break;
            }
            AffixLayout.insert(modifiers, AffixLayout.rollFrom(openSides, random, tagWeights));
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
