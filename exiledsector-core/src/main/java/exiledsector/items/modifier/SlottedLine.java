package exiledsector.items.modifier;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.modifiers.ModifierSlot;

import java.util.List;

public record SlottedLine(ModifierSlot slot, StyledText text, boolean caption, StyledText captionTags, String detailTitle,
                          List<StyledText> details) {

    public SlottedLine(ModifierSlot slot, StyledText text) {
        this(slot, text, null, List.of());
    }

    public SlottedLine(ModifierSlot slot, StyledText text, String detailTitle, List<StyledText> details) {
        this(slot, text, false, null, detailTitle, details);
    }

    public static SlottedLine caption(ModifierSlot slot, StyledText labels, StyledText tags) {
        return new SlottedLine(slot, labels, true, tags, null, List.of());
    }

    public boolean hasDetails() {
        return !details.isEmpty();
    }

    public StyledText inlineText() {
        if (captionTags == null) {
            return text;
        }
        if (text.isEmpty()) {
            return captionTags;
        }
        return Translation.msg("item.tooltip.captionLine").arg("labels", text).arg("tags", captionTags).styled();
    }
}
