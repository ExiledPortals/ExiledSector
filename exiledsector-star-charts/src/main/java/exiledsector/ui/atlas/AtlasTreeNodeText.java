package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasEffects;
import exiledsector.charts.atlas.AtlasModifier;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.tree.AtlasTreeType;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.List;

final class AtlasTreeNodeText {

    static final String LINE_KEY_PREFIX = "atlasTree.line.";

    private AtlasTreeNodeText() {
    }

    static List<StyledText> body(AtlasTreeType type, StyledText extraLine) {
        List<StyledText> lines = new ArrayList<>();
        for (ChartGenContext.AppliedEffect effect : type.effects()) {
            addLine(lines, chartLine(effect.effect(), effect.value()));
        }
        for (AtlasEffects.Line effect : type.atlasEffects()) {
            addLine(lines, atlasLine(effect.effect(), effect.value()));
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

    static StyledText chartLine(ChartModifier effect, float value) {
        String treeKey = LINE_KEY_PREFIX + effect.name();
        return effect.description(Translation.hasMessage(effect.messageKey(treeKey, value)) ? treeKey : effect.key(), value);
    }

    static StyledText atlasLine(AtlasModifier effect, float value) {
        String treeKey = LINE_KEY_PREFIX + effect.name();
        return effect.description(Translation.hasMessage(treeKey) ? treeKey : effect.key(), value);
    }

    private static void addLine(List<StyledText> lines, StyledText line) {
        if (line != null) {
            lines.add(line);
        }
    }
}
