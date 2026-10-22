package exiledsector.items;

import exiledsector.modifiers.ModifierCategory;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;

public record SlottedRoll(ModifierSlot slot, RolledEffect roll, ModifierLine line) {

    public ModifierCategory category() {
        return slot.category();
    }
}
