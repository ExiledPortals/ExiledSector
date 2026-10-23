package exiledsector.ui.util;

import exiledlib.i18n.StyledText;
import exiledlib.i18n.TextWrapper;
import exiledsector.ui.SkillTreePanelStyle;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ItalicTextLabel {

    static final float SLANT = 0.2f;
    private static final FloatBuffer SLANT_MATRIX = BufferUtils.createFloatBuffer(16).put(new float[]{
            1f, 0f, 0f, 0f,
            SLANT, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f}).flip();

    private final Supplier<LazyFont> fontSource;
    private final float fontSize;
    private final Color color;
    private final List<TextLabel> lineLabels = new ArrayList<>();
    private int lineCount;

    public ItalicTextLabel(float fontSize, Color color) {
        this(SkillTreePanelStyle::font, fontSize, color);
    }

    ItalicTextLabel(Supplier<LazyFont> fontSource, float fontSize, Color color) {
        this.fontSource = fontSource;
        this.fontSize = fontSize;
        this.color = color;
    }

    public ItalicTextLabel setWrapped(String rawText, float maxWidth, float maxHeight) {
        LazyFont font = fontSource.get();
        String wrapped = font == null ? rawText
                : TextWrapper.wrap(StyledText.of(rawText), line -> font.calcWidth(line, fontSize), fontSize, maxWidth - slantOverhang(), maxHeight).plain();
        lineLabels.clear();
        String[] lines = wrapped.split("\n", -1);
        lineCount = lines.length;
        for (String line : lines) {
            lineLabels.add(line.isEmpty() ? null : new TextLabel(fontSource, fontSize, color, LazyFont.TextAnchor.TOP_LEFT).set(line));
        }
        return this;
    }

    public ItalicTextLabel setAlpha(float value) {
        for (TextLabel lineLabel : lineLabels) {
            if (lineLabel != null) {
                lineLabel.setAlpha(value);
            }
        }
        return this;
    }

    public void draw(float x, float y) {
        float lineHeight = lineHeight();
        float lineTop = y;
        for (TextLabel lineLabel : lineLabels) {
            if (lineLabel != null) {
                GL11.glPushMatrix();
                GL11.glTranslatef(x, lineTop - lineHeight / 2f, 0f);
                GL11.glMultMatrix(SLANT_MATRIX);
                lineLabel.draw(0f, lineHeight / 2f);
                GL11.glPopMatrix();
            }
            lineTop -= lineHeight;
        }
    }

    public float width() {
        float widest = 0f;
        for (TextLabel lineLabel : lineLabels) {
            if (lineLabel != null) {
                widest = Math.max(widest, lineLabel.width());
            }
        }
        return widest == 0f ? 0f : widest + slantOverhang();
    }

    public float height() {
        return lineCount * lineHeight();
    }

    private float lineHeight() {
        return fontSize * SkillTreePanelStyle.FONT_LINE_HEIGHT_FACTOR;
    }

    private float slantOverhang() {
        return SLANT * lineHeight() / 2f;
    }
}
