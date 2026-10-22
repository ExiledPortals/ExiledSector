package exiledsector.ui.util;

import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.TextWrapper;
import exiledsector.ui.SkillTreePanelStyle;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class TextLabel {

    static final int HIGHLIGHT_ALPHA_STEPS = 16;
    private static final int OPAQUE = 255;
    // TODO: drop this if LazyLib stops applying DrawableString colour changes one character early
    private static final int LAZYFONT_COLOR_INDEX_OFFSET = 1;
    private static final String PARAGRAPH_SEPARATOR = "\n\n";
    private static final String HIGHLIGHT_TAIL = " ";
    private static final Function<Style, Color> STANDARD_HIGHLIGHTS = SkillTreePanelStyle::standardHighlightColor;

    private final Supplier<LazyFont> fontSource;
    private final float fontSize;
    private final LazyFont.TextAnchor anchor;
    private Color baseColor;
    private int alpha = OPAQUE;
    private String plainText;
    private List<StyledText.Span> highlightSpans = List.of();
    private Function<Style, Color> highlightColors = STANDARD_HIGHLIGHTS;
    private Object contentSignature;
    private boolean measured;
    private float measuredWidth;
    private float measuredHeight;

    private LazyFont.DrawableString drawable;
    private boolean contentDirty = true;
    private Color drawnColor;
    private int drawnAlpha = -1;

    public TextLabel(float fontSize, Color color) {
        this(fontSize, color, LazyFont.TextAnchor.TOP_LEFT);
    }

    public TextLabel(float fontSize, Color color, LazyFont.TextAnchor anchor) {
        this(SkillTreePanelStyle::font, fontSize, color, anchor);
    }

    TextLabel(Supplier<LazyFont> fontSource, float fontSize, Color color, LazyFont.TextAnchor anchor) {
        this.fontSource = fontSource;
        this.fontSize = fontSize;
        this.baseColor = color;
        this.anchor = anchor;
    }

    public TextLabel set(String value) {
        if (highlightSpans.isEmpty() && Objects.equals(value, plainText)) {
            return this;
        }
        plainText = value;
        highlightSpans = List.of();
        contentChanged();
        return this;
    }

    public TextLabel setStyled(StyledText value, Function<Style, Color> colors) {
        if (value.spans().isEmpty()) {
            return set(value.plain());
        }
        if (value.plain().equals(plainText) && value.spans().equals(highlightSpans) && colors == highlightColors) {
            return this;
        }
        plainText = value.plain();
        highlightSpans = value.spans();
        highlightColors = colors;
        contentChanged();
        return this;
    }

    public TextLabel setWrapped(String rawText, float maxWidth, float maxHeight) {
        LazyFont font = fontSource.get();
        return set(font == null ? rawText : wrap(font, StyledText.of(rawText), maxWidth, maxHeight).plain());
    }

    public TextLabel setParagraphs(List<StyledText> paragraphs, float maxWidth, float maxHeight, Function<Style, Color> colors) {
        LazyFont font = fontSource.get();
        StyledText joined = StyledText.EMPTY;
        for (StyledText paragraph : paragraphs) {
            if (!joined.isEmpty()) {
                joined = joined.append(PARAGRAPH_SEPARATOR);
            }
            joined = joined.append(font == null ? paragraph : wrap(font, paragraph, maxWidth, maxHeight));
        }
        return setStyled(joined, colors);
    }

    public TextLabel refresh(Object signature, Consumer<TextLabel> writer) {
        if (!Objects.equals(signature, contentSignature)) {
            writer.accept(this);
            contentSignature = signature;
        }
        return this;
    }

    public TextLabel setColor(Color value) {
        baseColor = value;
        return this;
    }

    public TextLabel setAlpha(float value) {
        alpha = toByteAlpha(value);
        return this;
    }

    public boolean isEmpty() {
        return plainText == null || plainText.isEmpty();
    }

    public boolean draw(float x, float y) {
        LazyFont.DrawableString current = current();
        if (current == null) {
            return false;
        }
        current.draw(x, y);
        return true;
    }

    public float width() {
        measure();
        return measuredWidth;
    }

    public float height() {
        measure();
        return measuredHeight;
    }

    public void dispose() {
        if (drawable != null) {
            drawable.dispose();
            drawable = null;
        }
        contentDirty = true;
    }

    static int toByteAlpha(float alphaMult) {
        return Math.round(Math.max(0f, Math.min(1f, alphaMult)) * OPAQUE);
    }

    static int quantisedAlpha(int byteAlpha) {
        int step = Math.round(byteAlpha * (float) HIGHLIGHT_ALPHA_STEPS / OPAQUE);
        return Math.round(step * (float) OPAQUE / HIGHLIGHT_ALPHA_STEPS);
    }

    static Color tinted(Color color, int byteAlpha) {
        return byteAlpha == OPAQUE ? color : new Color(color.getRed(), color.getGreen(), color.getBlue(), byteAlpha * color.getAlpha() / OPAQUE);
    }

    static int colourChangeIndex(String text, int boundary) {
        int drawnBoundary = boundary;
        while (drawnBoundary < text.length() && text.charAt(drawnBoundary) == '\n') {
            drawnBoundary++;
        }
        return Math.min(text.length(), drawnBoundary + LAZYFONT_COLOR_INDEX_OFFSET);
    }

    private void contentChanged() {
        contentDirty = true;
        measured = false;
    }

    private StyledText wrap(LazyFont font, StyledText text, float maxWidth, float maxHeight) {
        return TextWrapper.wrap(text, line -> font.calcWidth(line, fontSize), fontSize, maxWidth, maxHeight);
    }

    private void measure() {
        if (measured) {
            return;
        }
        LazyFont font = fontSource.get();
        if (plainText == null || font == null) {
            measuredWidth = 0f;
            measuredHeight = 0f;
            return;
        }
        String[] lines = plainText.split("\n", -1);
        float widest = 0f;
        for (String line : lines) {
            widest = Math.max(widest, font.calcWidth(line, fontSize));
        }
        measuredWidth = widest;
        measuredHeight = lines.length * fontSize * SkillTreePanelStyle.FONT_LINE_HEIGHT_FACTOR;
        measured = true;
    }

    private LazyFont.DrawableString current() {
        if (plainText == null) {
            return null;
        }
        if (drawable == null) {
            LazyFont font = fontSource.get();
            if (font == null) {
                return null;
            }
            drawable = font.createText("", baseColor, fontSize);
            drawable.setAlignment(LazyFont.TextAlignment.LEFT);
            drawable.setAnchor(anchor);
            contentDirty = true;
        }
        boolean highlighted = !highlightSpans.isEmpty();
        int shownAlpha = highlighted ? quantisedAlpha(alpha) : alpha;
        boolean colourChanged = shownAlpha != drawnAlpha || !baseColor.equals(drawnColor);
        if (contentDirty || (highlighted && colourChanged)) {
            write(shownAlpha);
        } else if (colourChanged) {
            drawable.setBaseColor(tinted(baseColor, shownAlpha));
        }
        drawnAlpha = shownAlpha;
        drawnColor = baseColor;
        contentDirty = false;
        return drawable;
    }

    private void write(int shownAlpha) {
        drawable.setBaseColor(tinted(baseColor, shownAlpha));
        if (highlightSpans.isEmpty()) {
            drawable.setText(plainText);
            return;
        }
        String text = plainText + HIGHLIGHT_TAIL;
        drawable.setText("");
        int cursor = 0;
        for (StyledText.Span span : highlightSpans) {
            int start = colourChangeIndex(text, span.start());
            int end = colourChangeIndex(text, span.end());
            drawable.append(text.substring(cursor, start));
            drawable.append(text.substring(start, end), tinted(highlightColors.apply(span.style()), shownAlpha));
            cursor = end;
        }
        drawable.append(text.substring(cursor));
    }
}
