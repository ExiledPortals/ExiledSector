package exiledsector.items;

import exiledsector.i18n.StyledText;
import exiledsector.modifiers.ModifierSlot;

import java.util.List;

public record SlottedLine(ModifierSlot slot, StyledText text, String detailTitle, List<StyledText> details) {

    public SlottedLine(ModifierSlot slot, StyledText text) {
        this(slot, text, null, List.of());
    }

    public boolean hasDetails() {
        return !details.isEmpty();
    }
}
