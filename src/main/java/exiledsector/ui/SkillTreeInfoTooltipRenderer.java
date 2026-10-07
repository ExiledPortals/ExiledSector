package exiledsector.ui;

import exiledsector.ui.util.CachedText;
import org.lazywizard.lazylib.ui.LazyFont;

final class SkillTreeInfoTooltipRenderer {

    private static final float TOOLTIP_MAX_TITLE_HEIGHT = 200f;
    private static final float TOOLTIP_MAX_BODY_HEIGHT = 800f;

    private final SkillTreePanelStyle panelStyle;
    private final CachedText<Void, SkillTreePanelStyle.TooltipText> titleCache = new CachedText<>();
    private final CachedText<Void, SkillTreePanelStyle.TooltipText> bodyCache = new CachedText<>();

    SkillTreeInfoTooltipRenderer(SkillTreePanelStyle panelStyle) {
        this.panelStyle = panelStyle;
    }

    void render(String titleText, String bodyText, float mouseX, float mouseY, float alphaMult) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return;

        SkillTreePanelStyle.TooltipText wrappedTitle = titleCache.get(titleText, () -> SkillTreePanelStyle.buildWrappedText(
                font, titleText, SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_MAX_TEXT_WIDTH,
                TOOLTIP_MAX_TITLE_HEIGHT, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR));
        SkillTreePanelStyle.TooltipText wrappedBody = bodyCache.get(bodyText, () -> SkillTreePanelStyle.buildWrappedText(
                font, bodyText, SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_MAX_TEXT_WIDTH,
                TOOLTIP_MAX_BODY_HEIGHT, SkillTreePanelStyle.TOOLTIP_BODY_COLOR));

        panelStyle.drawTitleBodyTooltip(wrappedTitle, wrappedBody, mouseX, mouseY, alphaMult);
    }
}
