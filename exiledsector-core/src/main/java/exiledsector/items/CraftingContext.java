package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;

import java.util.Set;
import java.util.function.Predicate;

public record CraftingContext(ItemStore store, Predicate<ItemDefinition> allowedUniques, Set<ModifierSlot> guardedSlots) {

    public CraftingContext {
        guardedSlots = guardedSlots == null ? Set.of() : guardedSlots;
    }
}
