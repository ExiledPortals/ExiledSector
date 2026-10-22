package exiledsector.items;

import exiledlib.i18n.StyledText;
import exiledsector.modifiers.ModifierSlot;

public record SlottedLine(ModifierSlot slot, StyledText text) {
}
