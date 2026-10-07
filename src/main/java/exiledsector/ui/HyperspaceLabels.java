package exiledsector.ui;

import exiledsector.i18n.Translation;
import exiledsector.ui.hyperspace.HyperspaceAnchor;
import exiledsector.ui.hyperspace.HyperspaceCamera;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.ReusableText;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class HyperspaceLabels {

    static final float FONT_SIZE = SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE;
    private static final float PADDING_X = 16f;
    private static final float PADDING_Y = 8f;
    private static final float GAP = 6f;
    private static final float MIN_ANCHOR_HIT_RADIUS = 18f;
    static final float LABEL_SPACE = FONT_SIZE + PADDING_Y * 2f + GAP;
    private static final Color TEXT_COLOR = SkillTreePanelStyle.TOOLTIP_TITLE_COLOR;
    private static final Color HOVER_COLOR = SkillTreePanelStyle.GLOW_COLOR;

    private final BorderedPanel labelPanel = new BorderedPanel(HyperspaceLabels.class);
    private final Map<String, ReusableText> regionTexts = new HashMap<>();
    private int textAlpha = -1;
    private Color textColor = TEXT_COLOR;
    private Color hoverColor = HOVER_COLOR;

    void render(TreeViewport viewport, List<HyperspaceAnchor> anchors, float mapStarRadius, float mapAmount, HyperspaceAnchor hovered,
                float alphaMult) {
        if (alphaMult <= 0f) {
            return;
        }
        int alpha = Math.round(255f * Math.min(1f, alphaMult));
        if (alpha != textAlpha) {
            textAlpha = alpha;
            textColor = withAlpha(TEXT_COLOR, alpha);
            hoverColor = withAlpha(HOVER_COLOR, alpha);
        }
        for (HyperspaceAnchor anchor : anchors) {
            ReusableText labelText = text(anchor);
            ScreenRect labelBox = box(viewport, anchor, mapStarRadius, mapAmount, labelText.width());
            labelPanel.draw(labelBox.left(), labelBox.bottom(), labelBox.width(), labelBox.height(), alphaMult);
            labelText.setColor(anchor.equals(hovered) ? hoverColor : textColor);
            labelText.draw(labelBox.left() + PADDING_X, labelBox.bottom() + labelBox.height() - PADDING_Y);
        }
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    HyperspaceAnchor anchorAt(TreeViewport viewport, List<HyperspaceAnchor> anchors, float mapStarRadius, float mapAmount, float x, float y) {
        for (int i = anchors.size() - 1; i >= 0; i--) {
            HyperspaceAnchor anchor = anchors.get(i);
            if (box(viewport, anchor, mapStarRadius, mapAmount, text(anchor).width()).contains(x, y)) {
                return anchor;
            }
            float dx = x - viewport.screenX(anchor.x());
            float dy = y - viewport.screenY(anchor.y());
            float hitRadius = Math.max(MIN_ANCHOR_HIT_RADIUS, anchor.displayRadius(mapStarRadius, mapAmount) * viewport.zoom());
            if (dx * dx + dy * dy <= hitRadius * hitRadius) {
                return anchor;
            }
        }
        return null;
    }

    private ReusableText text(HyperspaceAnchor anchor) {
        return regionTexts.computeIfAbsent(anchor.region(),
                region -> new ReusableText(FONT_SIZE, TEXT_COLOR).set(Translation.text("hyperspace.region." + region)));
    }

    private static ScreenRect box(TreeViewport viewport, HyperspaceAnchor anchor, float mapStarRadius, float mapAmount, float textWidth) {
        float labelWidth = textWidth + PADDING_X * 2f;
        float labelHeight = FONT_SIZE + PADDING_Y * 2f;
        float anchorTop = viewport.screenY(anchor.y()) + anchor.displayRadius(mapStarRadius, mapAmount) * HyperspaceCamera.ANCHOR_REACH * viewport.zoom();
        return new ScreenRect(viewport.screenX(anchor.x()) - labelWidth / 2f, anchorTop + GAP, labelWidth, labelHeight);
    }
}
