package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;

import java.util.Set;
import java.util.function.Predicate;

public final class HullItemCraftRefusals {

    public static final String PREFIX = "ui.infusedCraft.refused.";

    private HullItemCraftRefusals() {
    }

    public static UseVerdict ofItem(Item item) {
        if (item == null) {
            return UseVerdict.refused(PREFIX + "loadFirst");
        }
        ItemDefinition definition = item.definition();
        if (definition == null || definition.type() != ItemType.HULL_ITEM || !ItemTypes.takesCraftingItem(definition.type())) {
            return UseVerdict.refused(PREFIX + "itemType");
        }
        if (item.mirrored()) {
            return UseVerdict.refused(PREFIX + "mirrored");
        }
        if (item.corrupted()) {
            return UseVerdict.refused(PREFIX + "corrupted");
        }
        if (ItemCrafting.isLockedFromCraftingItem(definition)) {
            return UseVerdict.refused(PREFIX + "locked");
        }
        return UseVerdict.ALLOW;
    }

    public static UseVerdict ofInhibitors(Predicate<Set<ModifierSlot>> possibleWithInhibited, Set<ModifierSlot> inhibited, String impossibleKey) {
        if (possibleWithInhibited.test(inhibited)) {
            return UseVerdict.ALLOW;
        }
        return possibleWithInhibited.test(Set.of()) ? UseVerdict.refused(PREFIX + "inhibited") : UseVerdict.refused(impossibleKey);
    }
}
