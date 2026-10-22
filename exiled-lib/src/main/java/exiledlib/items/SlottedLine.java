package exiledlib.items;

import exiledlib.i18n.StyledText;
import exiledlib.modifiers.ModifierSlot;

public record SlottedLine(ModifierSlot slot, StyledText text) {
}
