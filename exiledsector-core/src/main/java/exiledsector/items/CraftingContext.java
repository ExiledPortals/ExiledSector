package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.tags.TagWeights;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

public record CraftingContext(ItemStore store, Predicate<ItemDefinition> allowedUniques, Set<ModifierSlot> inhibitedSlots, TagWeights favouredTags) {

    public CraftingContext {
        inhibitedSlots = inhibitedSlots == null ? Set.of() : inhibitedSlots;
        favouredTags = favouredTags == null ? TagWeights.NONE : favouredTags;
    }

    public CraftingContext(ItemStore store, Predicate<ItemDefinition> allowedUniques, Set<ModifierSlot> inhibitedSlots) {
        this(store, allowedUniques, inhibitedSlots, TagWeights.NONE);
    }

    public static CraftingContext withInhibitors(ItemStore store, Predicate<ItemDefinition> allowedUniques, Collection<CraftingInhibitor> inhibitors) {
        return new CraftingContext(store, allowedUniques, CraftingInhibitor.inhibitedSlots(inhibitors), CraftingInhibitor.favouredTags(inhibitors));
    }
}
