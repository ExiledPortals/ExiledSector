package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;

import java.util.Set;
import java.util.function.Predicate;

public record CraftingContext(ItemStore store, Predicate<ItemDefinition> allowedUniques, Set<ModifierSlot> inhibitedSlots) {

    public CraftingContext {
        inhibitedSlots = inhibitedSlots == null ? Set.of() : inhibitedSlots;
    }
}
