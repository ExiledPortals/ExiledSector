package exiledsector.ui.inspect;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.codex.CodexDialogAPI;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.util.List;

public class SkillTreeCodexEntry extends CodexEntryV2 implements CustomUIPanelPlugin {

    public static final String ID_PREFIX = "exiledSector_skillTree_";
    private static final float RELATED_ENTRIES_WIDTH = 290f;
    private static final float BOX_HORIZONTAL_PAD = 30f;
    private static final float PAD = 10f;

    private final ShipTreeLookup.ShipTree shipTree;

    public SkillTreeCodexEntry(String id, FleetMemberAPI member, ShipTreeLookup.ShipTree shipTree) {
        super(id, Translation.gameText("codex.title"), null, member);
        this.shipTree = shipTree;
    }

    ShipTreeLookup.ShipTree shipTree() {
        return shipTree;
    }

    @Override
    public void createTitleForList(TooltipMakerAPI info, float width, ListMode mode) {
        I18n.forGameText(() -> {
            VanillaText.addPara(info, Translation.styled("codex.title"), 0f, Misc.getBasePlayerColor());
            VanillaText.addPara(info, Translation.msg("summary.level").arg("level", shipTree.skillData().getLevel()).styled(), 0f,
                    Misc.getGrayColor());
        });
    }

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
        ShipTreeSummaryRenderer.render(textElement, (FleetMemberAPI) getParam(), shipTree, 0f);
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
