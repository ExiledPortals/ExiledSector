package exiledsector.ui.inspect;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.impl.codex.CodexDialogAPI;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.util.List;

public abstract class BoxedCodexEntry extends CodexEntryV2 implements CustomUIPanelPlugin {

    static final float PAD = 10f;
    private static final float RELATED_ENTRIES_WIDTH = 290f;
    private static final float BOX_HORIZONTAL_PAD = 30f;

    protected BoxedCodexEntry(String id, String title, String icon, Object param) {
        super(id, title, icon, param);
    }

    protected abstract void writeDetail(TooltipMakerAPI info, float textWidth);

    @Override
    public boolean hasCustomDetailPanel() {
        return true;
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return this;
    }

    @Override
    public void createCustomDetail(CustomPanelAPI panel, UIPanelAPI relatedEntries, CodexDialogAPI codex) {
        float panelWidth = panel.getPosition().getWidth();
        float textWidth = panelWidth - RELATED_ENTRIES_WIDTH - PAD - BOX_HORIZONTAL_PAD + PAD;
        TooltipMakerAPI textElement = panel.createUIElement(textWidth, 0f, false);
        writeDetail(textElement, textWidth);
        panel.updateUIElementSizeAndMakeItProcessInput(textElement);

        UIPanelAPI textBox = panel.wrapTooltipWithBox(textElement);
        panel.addComponent(textBox).inTL(0f, 0f);
        float panelHeight = textBox.getPosition().getHeight();
        if (relatedEntries != null) {
            panel.addComponent(relatedEntries).inTR(0f, 0f);
            panelHeight = Math.max(panelHeight, relatedEntries.getPosition().getHeight());
        }
        panel.getPosition().setSize(panelWidth, panelHeight);
    }

    @Override
    public void destroyCustomDetail() {
    }

    @Override
    public void positionChanged(PositionAPI position) {
    }

    @Override
    public void renderBelow(float alphaMult) {
    }

    @Override
    public void render(float alphaMult) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
    }

    @Override
    public void buttonPressed(Object buttonId) {
    }
}
