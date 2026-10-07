package exiledsector.ui;

import exiledsector.ui.util.TextLabel;

final class SkillTreeInfoTooltipRenderer {

    private static final float TOOLTIP_MAX_TITLE_HEIGHT = 200f;
    private static final float TOOLTIP_MAX_BODY_HEIGHT = 800f;

    private final SkillTreePanelStyle panelStyle;
    private final TextLabel titleLabel = new TextLabel(SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel bodyLabel = new TextLabel(SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);

    SkillTreeInfoTooltipRenderer(SkillTreePanelStyle panelStyle) {
        this.panelStyle = panelStyle;
    }

    void render(String titleText, String bodyText, float mouseX, float mouseY, float alphaMult) {
        if (SkillTreePanelStyle.font() == null) return;

        titleLabel.refresh(titleText, label -> label.setWrapped(titleText, SkillTreePanelStyle.TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TITLE_HEIGHT));
        bodyLabel.refresh(bodyText, label -> label.setWrapped(bodyText, SkillTreePanelStyle.TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_BODY_HEIGHT));
        panelStyle.drawTitleBodyTooltip(titleLabel, bodyLabel, mouseX, mouseY, alphaMult);
    }
}
