package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasEffects;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasModifier;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.tree.AtlasTreeType;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.modifier.Luck;
import exiledsector.skills.SkillTier;

import java.util.ArrayList;
import java.util.List;

public final class AtlasTreeNodeText {

    static final String LINE_KEY_PREFIX = "atlasTree.line.";

    private AtlasTreeNodeText() {
    }

    public static List<StyledText> tooltipBody(AtlasTreeType type) {
        return tooltipBody(type, Luck.BASE_ROLLS);
    }

    public static List<StyledText> tooltipBody(AtlasTreeType type, int luckyRolls) {
        return body(type, type.tier() == SkillTier.ROOT
                ? Translation.msg("atlasTree.pointsSource").arg("max", AtlasState.MAX_ATLAS_SKILL_POINTS).styled() : null, luckyRolls);
    }

    static List<StyledText> body(AtlasTreeType type, StyledText extraLine) {
        return body(type, extraLine, Luck.BASE_ROLLS);
    }

    static List<StyledText> body(AtlasTreeType type, StyledText extraLine, int luckyRolls) {
        List<StyledText> lines = new ArrayList<>();
        for (ChartGenContext.AppliedEffect effect : type.effects()) {
            addChartLine(lines, effect.effect(), effect.value(), luckyRolls);
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

    static void addChartLine(List<StyledText> lines, ChartModifier effect, float value, int luckyRolls) {
        StyledText line = chartLine(effect, value);
        if (line == null || line.isEmpty()) {
            return;
        }
        lines.add(line);
        if (effect.makesLucky()) {
            lines.add(luckyReminder(luckyRolls));
        }
    }

    public static StyledText luckyReminder(int luckyRolls) {
        return luckyRolls <= Luck.BASE_ROLLS ? Translation.msg("chart.lucky.reminder.twice").styled()
                : Translation.msg("chart.lucky.reminder").arg("value", luckyRolls).styled();
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
