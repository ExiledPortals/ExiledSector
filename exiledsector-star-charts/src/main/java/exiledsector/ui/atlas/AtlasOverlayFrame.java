package exiledsector.ui.atlas;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.ui.ChromeMetrics;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.TextLabel;
import exiledsector.ui.util.UiClip;

import java.awt.Color;

final class AtlasOverlayFrame {

    private static final float PANEL_MARGIN = AtlasScreen.PANEL_MARGIN;
    private static final float BORDER_INSET = AtlasScreen.BORDER_INSET;
    private static final float CHROME_PAD = AtlasScreen.CHROME_PAD;
    private static final float BACKDROP_ALPHA = AtlasScreen.BACKDROP_ALPHA;

    private PositionAPI panelPosition;

    static ButtonAPI addTopRightButton(CustomPanelAPI panel, String buttonText, Object buttonId, float buttonWidth) {
        return addTopRightButton(panel, buttonText, buttonId, buttonWidth, 0f);
    }

    static ButtonAPI addTopRightButton(CustomPanelAPI panel, String buttonText, Object buttonId, float buttonWidth, float rightOffset) {
        TooltipMakerAPI element = panel.createUIElement(buttonWidth, ChromeMetrics.CONTROL_HEIGHT, false);
        ButtonAPI button = element.addButton(buttonText, buttonId, buttonWidth, ChromeMetrics.CONTROL_HEIGHT, 0f);
        panel.addUIElement(element).inTL(panel.getPosition().getWidth() - PANEL_MARGIN - CHROME_PAD - rightOffset - buttonWidth, PANEL_MARGIN + CHROME_PAD);
        return button;
    }

    void place(PositionAPI position) {
        panelPosition = position;
    }

    float innerLeft() {
        return panelPosition.getX() + PANEL_MARGIN;
    }

    float innerBottom() {
        return panelPosition.getY() + PANEL_MARGIN;
    }

    float innerWidth() {
        return panelPosition.getWidth() - PANEL_MARGIN * 2f;
    }

    float innerHeight() {
        return panelPosition.getHeight() - PANEL_MARGIN * 2f;
    }

    float chromeLeft() {
        return innerLeft() + CHROME_PAD;
    }

    float chromeRight() {
        return innerLeft() + innerWidth() - CHROME_PAD;
    }

    float chromeBottom() {
        return innerBottom() + CHROME_PAD;
    }

    float chromeTop() {
        return innerBottom() + innerHeight() - CHROME_PAD;
    }

    void drawBackdrop(float alpha, boolean withMargin) {
        if (withMargin) {
            GLDraw.fillQuad(panelPosition, Color.BLACK, BACKDROP_ALPHA * alpha);
        }
        fillInner(Color.BLACK, alpha);
        BorderedPanel.draw(innerLeft(), innerBottom(), innerWidth(), innerHeight(), alpha);
    }

    void fillInner(Color fillColour, float alpha) {
        GLDraw.fillQuad(innerLeft(), innerBottom(), innerWidth(), innerHeight(), fillColour, alpha);
    }

    GlScope clipContent() {
        return UiClip.clipUi(innerLeft() + BORDER_INSET, innerBottom(), innerWidth() - BORDER_INSET * 2f, innerHeight());
    }

    void drawFooterHint(TextLabel hintLabel, float alpha) {
        hintLabel.setAlpha(alpha).draw(chromeLeft(), innerBottom() + CHROME_PAD + hintLabel.height());
    }
}
