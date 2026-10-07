package exiledsector.ui;

import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class VanillaText {

    public record Prepared(String text, String[] highlights, Color[] colors) {

        @Override
        public boolean equals(Object other) {
            return other instanceof Prepared prepared && Objects.equals(text, prepared.text)
                    && Arrays.equals(highlights, prepared.highlights) && Arrays.equals(colors, prepared.colors);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * Objects.hashCode(text) + Arrays.hashCode(highlights)) + Arrays.hashCode(colors);
        }

        @Override
        public String toString() {
            return "Prepared[" + text + ", " + Arrays.toString(highlights) + ", " + Arrays.toString(colors) + "]";
        }
    }

    private static final String HIGHLIGHT_BOUNDARY_PUNCTUATION = "/.,;:\"'[]+-=!@$%^&*(){}|\\?<>`~";

    private VanillaText() {
    }

    public static LabelAPI addPara(TooltipMakerAPI tooltip, StyledText text, float pad, Color base) {
        return addPara(tooltip, text, pad, base, SkillTreePanelStyle::standardHighlightColor);
    }

    public static LabelAPI addPara(TooltipMakerAPI tooltip, StyledText text, float pad, Color base, Function<Style, Color> palette) {
        Prepared prepared = prepare(text, palette);
        LabelAPI label = tooltip.addPara("%s", pad, base, base, prepared.text());
        highlight(label, prepared);
        return label;
    }

    public static LabelAPI addPara(TextPanelAPI panel, StyledText text, Color base) {
        Prepared prepared = prepare(text, SkillTreePanelStyle::standardHighlightColor);
        LabelAPI label = panel.addPara("%s", base, base, prepared.text());
        highlight(label, prepared);
        return label;
    }

    public static Prepared prepare(StyledText text, Function<Style, Color> palette) {
        String plain = text.plain();
        StringBuilder preparedText = new StringBuilder(plain.length() + text.spans().size() * 2);
        List<String> highlights = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        int cursor = 0;
        for (StyledText.Span span : text.spans()) {
            preparedText.append(plain, cursor, span.start());
            if (!preparedText.isEmpty() && !isHighlightBoundary(preparedText.charAt(preparedText.length() - 1))) {
                preparedText.append(' ');
            }
            String highlighted = plain.substring(span.start(), span.end());
            preparedText.append(highlighted);
            highlights.add(highlighted);
            colors.add(palette.apply(span.style()));
            cursor = span.end();
            if (cursor < plain.length() && !isHighlightBoundary(plain.charAt(cursor))) {
                preparedText.append(' ');
            }
        }
        preparedText.append(plain.substring(cursor));
        return new Prepared(preparedText.toString(), highlights.toArray(new String[0]), colors.toArray(new Color[0]));
    }

    static boolean isHighlightBoundary(char c) {
        return Character.isWhitespace(c) || HIGHLIGHT_BOUNDARY_PUNCTUATION.indexOf(c) >= 0;
    }

    private static void highlight(LabelAPI label, Prepared prepared) {
        if (label == null || prepared.highlights().length == 0) {
            return;
        }
        label.setHighlight(prepared.highlights());
        label.setHighlightColors(prepared.colors());
    }
}
