package exiledsector.ui.atlas;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.tree.AtlasTree;
import exiledsector.ui.ChromeMetrics;
import exiledsector.ui.socket.WormholeButtonFx;

final class AtlasTreeButton {

    static final float BUTTON_WIDTH = 140f;
    static final float GLOW_ROOM = 32f;
    static final WormholeButtonFx.Intensity NOTHING_TO_SPEND = new WormholeButtonFx.Intensity(0.3f, 0f);
    static final WormholeButtonFx.Intensity POINTS_TO_SPEND = new WormholeButtonFx.Intensity(1.15f, 0.7f);

    private final WormholeButtonFx glow = new WormholeButtonFx();
    private WormholeButtonFx.Intensity glowIntensity = NOTHING_TO_SPEND;
    private TooltipMakerAPI buttonElement;
    private ButtonAPI button;
    private float buttonOpacity = 1f;

    static float buttonLeft(float panelWidth) {
        return (panelWidth - BUTTON_WIDTH) / 2f;
    }

    static float searchBarLeft(float panelWidth, float searchBarWidth) {
        return buttonLeft(panelWidth) - GLOW_ROOM - searchBarWidth;
    }

    static WormholeButtonFx.Intensity intensity(int unspentPoints) {
        return unspentPoints > 0 ? POINTS_TO_SPEND : NOTHING_TO_SPEND;
    }

    void add(CustomPanelAPI panel, String buttonText, Object buttonId, float buttonTop) {
        buttonElement = panel.createUIElement(BUTTON_WIDTH, ChromeMetrics.CONTROL_HEIGHT, false);
        buttonElement.setButtonFontOrbitron20Bold();
        button = buttonElement.addButton(buttonText, buttonId, WormholeButtonFx.TEXT_COLOR, WormholeButtonFx.BACKGROUND_COLOR, Alignment.MID,
                CutStyle.ALL, BUTTON_WIDTH, ChromeMetrics.CONTROL_HEIGHT, 0f);
        buttonElement.setButtonFontDefault();
        panel.addUIElement(buttonElement).inTL(buttonLeft(panel.getPosition().getWidth()), buttonTop);
    }

    void refresh(AtlasState state) {
        glowIntensity = intensity(AtlasTree.unspentPoints(state, AtlasTree.allocation(state)));
    }

    void advance(float amount, float opacity, boolean enabled) {
        glow.advance(amount);
        buttonOpacity = opacity;
        if (buttonElement != null) {
            buttonElement.setOpacity(opacity);
            button.setEnabled(enabled);
        }
    }

    void render(float alpha) {
        if (button == null) {
            return;
        }
        PositionAPI position = button.getPosition();
        glow.render(position.getX(), position.getY(), position.getWidth(), position.getHeight(), glowIntensity, buttonOpacity * alpha);
    }
}
