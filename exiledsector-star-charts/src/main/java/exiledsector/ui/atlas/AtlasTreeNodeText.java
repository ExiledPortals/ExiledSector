package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasEffects;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.tree.AtlasTreeType;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;

import java.util.ArrayList;
import java.util.List;

final class AtlasTreeNodeText {

    private AtlasTreeNodeText() {
    }

    static List<StyledText> body(AtlasTreeType type, StyledText extraLine) {
        List<StyledText> lines = new ArrayList<>();
        for (ChartGenContext.AppliedEffect effect : type.effects()) {
            addLine(lines, effect.effect().description(effect.value()));
        }
        for (AtlasEffects.Line effect : type.atlasEffects()) {
            addLine(lines, effect.effect().description(effect.value()));
        }
        String flavour = type.flavourText();
        if (flavour != null && !flavour.isBlank()) {
            lines.add(StyledText.styled(flavour, Style.MUTED));
        }
        if (extraLine != null) {
            lines.add(extraLine);
        }
        return lines;
    }

    private static void addLine(List<StyledText> lines, StyledText line) {
        if (line != null) {
            lines.add(line);
        }
    }
}
