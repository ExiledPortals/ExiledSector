package exiledlib.items;

import exiledlib.modifiers.ModifierCategory;
import exiledlib.modifiers.ModifierLine;
import exiledlib.modifiers.ModifierSlot;

public record SlottedRoll(ModifierSlot slot, RolledEffect roll, ModifierLine line) {

    public ModifierCategory category() {
        return slot.category();
    }
}
