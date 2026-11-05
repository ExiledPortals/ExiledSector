package exiledsector.ui;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.TextWrapper;
import exiledsector.i18n.Translation;
import exiledsector.items.HullUpgradesSetting;
import exiledsector.ui.refit.RefitScreenLayout;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.TextLabel;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class HullItemsLevelNotice {

    static final float FONT_SIZE = 40f;
    private static final float LINE_GAP = 6f;
    private static final float PARAGRAPH_GAP = 24f;
    private static final float MAX_WIDTH_SHARE = 0.7f;
    private static final float BAND_PADDING = 32f;
    private static final float BAND_ALPHA = 0.8f;
    private static final Color LEVEL_COLOR = Color.WHITE;
    private static final Color SETTING_COLOR = SkillTreePanelStyle.TOOLTIP_BODY_COLOR;

    private final List<String> paragraphs;
    private final List<List<TextLabel>> wrappedParagraphs = new ArrayList<>();
    private float wrappedWidth = -1f;

    private HullItemsLevelNotice(int unlockLevel) {
        paragraphs = List.of(
                Translation.msg("ui.hullItemsLevelNotice.level").arg("level", unlockLevel).text(),
                Translation.text("ui.hullItemsLevelNotice.setting"));
    }

    static HullItemsLevelNotice forSector(SectorAPI sector) {
        return HullUpgradesSetting.hullItemDropsOpen(sector) ? null : new HullItemsLevelNotice(HullUpgradesSetting.unlockLevel());
    }

    List<String> paragraphs() {
        return paragraphs;
    }

    void draw(RefitScreenLayout layout, float alphaMult) {
        if (alphaMult <= 0f) {
            return;
        }
        wrapTo(layout.frameWidth() * MAX_WIDTH_SHARE);
        float lineHeight = FONT_SIZE * SkillTreePanelStyle.FONT_LINE_HEIGHT_FACTOR;
        float textHeight = textHeight(lineHeight);
        float centreX = layout.frameX() + layout.frameWidth() / 2f;
        float textTop = layout.frameY() + (layout.frameHeight() + textHeight) / 2f;
        GLDraw.fillQuad(layout.frameX(), textTop - textHeight - BAND_PADDING, layout.frameWidth(), textHeight + BAND_PADDING * 2f,
                Color.BLACK, BAND_ALPHA * alphaMult);
        float lineTop = textTop;
        for (List<TextLabel> lines : wrappedParagraphs) {
            for (TextLabel line : lines) {
                line.setAlpha(alphaMult).draw(centreX, lineTop);
                lineTop -= lineHeight + LINE_GAP;
            }
            lineTop += LINE_GAP - PARAGRAPH_GAP;
        }
    }

    void dispose() {
        wrappedParagraphs.forEach(lines -> lines.forEach(TextLabel::dispose));
        wrappedParagraphs.clear();
        wrappedWidth = -1f;
    }

    private float textHeight(float lineHeight) {
        int lineCount = wrappedParagraphs.stream().mapToInt(List::size).sum();
        return lineCount * lineHeight + (lineCount - wrappedParagraphs.size()) * LINE_GAP
                + Math.max(0, wrappedParagraphs.size() - 1) * PARAGRAPH_GAP;
    }

    private void wrapTo(float maxWidth) {
        if (maxWidth == wrappedWidth) {
            return;
        }
        dispose();
        wrappedWidth = maxWidth;
        LazyFont font = SkillTreePanelStyle.font();
        for (int index = 0; index < paragraphs.size(); index++) {
            String paragraph = paragraphs.get(index);
            String wrapped = font == null ? paragraph
                    : TextWrapper.wrap(StyledText.of(paragraph), line -> font.calcWidth(line, FONT_SIZE), FONT_SIZE, maxWidth, Float.MAX_VALUE).plain();
            Color color = index == 0 ? LEVEL_COLOR : SETTING_COLOR;
            List<TextLabel> lines = new ArrayList<>();
            for (String line : wrapped.split("\n")) {
                lines.add(new TextLabel(FONT_SIZE, color, LazyFont.TextAnchor.TOP_CENTER).set(line));
            }
            wrappedParagraphs.add(lines);
        }
    }
}
