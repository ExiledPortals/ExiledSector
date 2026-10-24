package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.ui.refit.RefitButtonColumn;
import exiledsector.ui.refit.RefitButtonConfig;
import lunalib.backend.ui.refit.RefitPanelBackgroundPlugin;
import lunalib.lunaRefit.LunaRefitManager;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.Collections;

public class SkillTreeRefitButton extends FullScreenRefitButton {

    public static final RefitButtonColumn.ButtonSpec COLUMN_BUTTON = new RefitButtonColumn.ButtonSpec("exiledSector_skillTreeButton",
            "ui.refitButton", () -> Keyboard.KEY_K, RefitButtonConfig::buttonUnderHullMods, () -> openPanel(null));
    static final float SHIP_CARD_ICON_SIZE = 128f;

    public static void addButton() {
        LunaRefitManager.addRefitButton(new SkillTreeRefitButton());
    }

    public static boolean openPanel(InputEventAPI event) {
        if (!SkillTreeSetting.enabled()) {
            return false;
        }
        return RefitPanelLauncher.open(SkillTreeRefitButton.class, event, context -> {
            RefitPanelBackgroundPlugin backgroundPlugin = new RefitPanelBackgroundPlugin(context.corePanel(), true);
            return new RefitPanelLauncher.Background(backgroundPlugin, backgroundPlugin::setPanel);
        });
    }

    @Override
    protected String buttonKey() {
        return "ui.refitButton";
    }

    @Override
    protected String iconPath() {
        return "graphics/icons/skills.png";
    }

    @Override
    protected boolean enabled() {
        return SkillTreeSetting.enabled();
    }

    @Override
    public void initPanel(CustomPanelAPI backgroundPanel, FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        float panelWidth = getPanelWidth(member, variant);
        float panelHeight = getPanelHeight(member, variant);

        TooltipMakerAPI shipCard = backgroundPanel.createUIElement(SHIP_CARD_ICON_SIZE, SHIP_CARD_ICON_SIZE, false);
        shipCard.addShipList(1, 1, SHIP_CARD_ICON_SIZE, new Color(0, 0, 0, 0), Collections.singletonList(member), 0f);
        float shipCardHeight = shipCard.getPrev().getPosition().getHeight();

        TooltipMakerAPI canvasElement = backgroundPanel.createUIElement(panelWidth, panelHeight, false);
        backgroundPanel.addUIElement(canvasElement);
        canvasElement.getPosition().inTL(0f, 0f);

        SkillTreeCanvasPlugin canvasPlugin = new SkillTreeCanvasPlugin(member, variant, shipCardHeight, this, backgroundPanel);
        CustomPanelAPI canvasPanel = Global.getSettings().createCustom(panelWidth, panelHeight, canvasPlugin);
        canvasElement.addCustom(canvasPanel, 0f).getPosition().inTL(0f, 0f);

        backgroundPanel.addUIElement(shipCard);
        shipCard.getPosition().inBL(ChromeMetrics.EDGE_MARGIN, ChromeMetrics.EDGE_MARGIN);
        canvasPlugin.setShipCard(shipCard);
    }
}
