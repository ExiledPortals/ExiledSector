package exiledsector.ui;

import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class VanillaText {

    public record Prepared(String text, String[] highlights, Color[] colors, Style[] styles) {

        @Override
        public boolean equals(Object other) {
            return other instanceof Prepared prepared && Objects.equals(text, prepared.text)
                    && Arrays.equals(highlights, prepared.highlights) && Arrays.equals(colors, prepared.colors)
                    && Arrays.equals(styles, prepared.styles);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * (31 * Objects.hashCode(text) + Arrays.hashCode(highlights)) + Arrays.hashCode(colors)) + Arrays.hashCode(styles);
        }

        @Override
        public String toString() {
            return "Prepared[" + text + ", " + Arrays.toString(highlights) + ", " + Arrays.toString(colors) + ", " + Arrays.toString(styles) + "]";
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
        LabelAPI label = plainPara(tooltip, prepared.text(), pad, base);
        highlight(label, prepared);
        OverchargeBreath.attach(tooltip, label, prepared);
        return label;
    }

    public static LabelAPI plainPara(TooltipMakerAPI tooltip, String text, float pad, Color color) {
        return tooltip.addPara("%s", pad, color, color, text);
    }

    public static String escapeFormat(String text) {
        return text.replace("%", "%%");
    }

    public static LabelAPI plainPara(TextPanelAPI panel, String text) {
        Color color = Misc.getTextColor();
        return panel.addPara("%s", color, color, text);
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
        List<Style> styles = new ArrayList<>();
        int cursor = 0;
        int previousHighlightEnd = -1;
        for (StyledText.Span span : text.spans()) {
            int start = skipWhitespace(plain, Math.max(cursor, span.start()), span.end());
            int end = trimWhitespace(plain, start, span.end());
            if (start == cursor && preparedText.length() == previousHighlightEnd) {
                start = skipBoundaries(plain, start, end);
            }
            if (start >= end) {
                continue;
            }
            preparedText.append(plain, cursor, start);
            if (!preparedText.isEmpty() && !isHighlightBoundary(preparedText.charAt(preparedText.length() - 1))) {
                preparedText.append(' ');
            }
            String highlighted = plain.substring(start, end);
            preparedText.append(highlighted);
            previousHighlightEnd = preparedText.length();
            highlights.add(highlighted);
            colors.add(palette.apply(span.style()));
            styles.add(span.style());
            cursor = end;
            if (cursor < plain.length() && !isHighlightBoundary(plain.charAt(cursor))) {
                preparedText.append(' ');
            }
        }
        preparedText.append(plain.substring(cursor));
        return new Prepared(preparedText.toString(), highlights.toArray(new String[0]), colors.toArray(new Color[0]), styles.toArray(new Style[0]));
    }

    private static int skipWhitespace(String text, int from, int limit) {
        int position = from;
        while (position < limit && Character.isWhitespace(text.charAt(position))) {
            position++;
        }
        return position;
    }

    private static int trimWhitespace(String text, int from, int end) {
        int position = end;
        while (position > from && Character.isWhitespace(text.charAt(position - 1))) {
            position--;
        }
        return position;
    }

    private static int skipBoundaries(String text, int from, int limit) {
        int position = from;
        while (position < limit && isHighlightBoundary(text.charAt(position))) {
            position++;
        }
        return position;
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
