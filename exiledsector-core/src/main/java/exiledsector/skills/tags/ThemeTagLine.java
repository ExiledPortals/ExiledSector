package exiledsector.skills.tags;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ThemeTagLine {

    private ThemeTagLine() {
    }

    public static StyledText of(Collection<String> tags) {
        List<StyledText> tagNames = new ArrayList<>();
        for (String tag : tags) {
            if (ThemeTags.isThemeTag(tag)) {
                tagNames.add(StyledText.of(ThemeTags.displayName(tag)));
            }
        }
        if (tagNames.isEmpty()) {
            return null;
        }
        return Translation.msg("themeTags.tooltipLine").arg("tags", Translation.list(tagNames)).styled();
    }
}
