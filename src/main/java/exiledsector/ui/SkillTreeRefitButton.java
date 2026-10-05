package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import exiledsector.i18n.Translation;
import exiledsector.ui.refit.RefitButtonConfig;
import lunalib.backend.ui.refit.RefitButtonAdder;
import lunalib.backend.ui.refit.RefitPanelBackgroundPlugin;
import lunalib.lunaRefit.BaseRefitButton;
import lunalib.lunaRefit.LunaRefitManager;

import java.awt.Color;
import java.util.Collections;

public class SkillTreeRefitButton extends BaseRefitButton {

    private static final float SCREEN_FRACTION = 1f;
    private static final String CLICK_SOUND = "ui_button_pressed";
    static final float SHIP_CARD_ICON_SIZE = 128f;
    static final float SHIP_CARD_MARGIN = 16f;

    public static void addButton() {
        LunaRefitManager.addRefitButton(new SkillTreeRefitButton());
    }

    public static boolean openPanel(InputEventAPI event) {
        RefitButtonAdder adder = refitButtonAdder();
        if (adder == null || adder.getActivePanel() != null || adder.getCorePanel() == null) return false;
        if (!(LunaRefitManager.getFirstButtonOfClass(SkillTreeRefitButton.class) instanceof SkillTreeRefitButton button)) return false;
        FleetMemberAPI member = adder.getMember();
        ShipVariantAPI variant = adder.getVariant();
        MarketAPI market = adder.getMarket();
        if (member == null || variant == null || !button.isClickable(member, variant, market) || !button.hasPanel(member, variant, market)) {
            return false;
        }
        Global.getSoundPlayer().playUISound(CLICK_SOUND, 1f, 1f);
        button.onClick(member, variant, event, market);

        UIPanelAPI corePanel = adder.getCorePanel();
        float width = button.getPanelWidth(member, variant);
        float height = button.getPanelHeight(member, variant);
        RefitPanelBackgroundPlugin background = new RefitPanelBackgroundPlugin(corePanel, true);
        CustomPanelAPI panel = Global.getSettings().createCustom(width, height, background);
        adder.setActivePanel(panel);
        adder.setActivePanelButton(button);
        panel.getPosition().inTL(Global.getSettings().getScreenWidth() / 2f - width / 2f, Global.getSettings().getScreenHeight() / 2f - height / 2f);
        background.setPanel(panel);
        corePanel.addComponent(panel);
        try {
            button.initPanel(panel, member, variant, market);
        } catch (RuntimeException | LinkageError e) {
            corePanel.removeComponent(panel);
            adder.setActivePanel(null);
            adder.setActivePanelButton(null);
            throw e;
        }
        return true;
    }

    private static RefitButtonAdder refitButtonAdder() {
        for (Object script : Global.getSector().getTransientScripts()) {
            if (script instanceof RefitButtonAdder adder) {
                return adder;
            }
        }
        return null;
    }

    @Override
    public float getPanelWidth(FleetMemberAPI member, ShipVariantAPI variant) {
        return Global.getSettings().getScreenWidth() * SCREEN_FRACTION;
    }

    @Override
    public float getPanelHeight(FleetMemberAPI member, ShipVariantAPI variant) {
        return Global.getSettings().getScreenHeight() * SCREEN_FRACTION;
    }

    @Override
    public String getButtonName(FleetMemberAPI member, ShipVariantAPI variant) {
        return Translation.gameText("ui.refitButton");
    }

    @Override
    public String getIconName(FleetMemberAPI member, ShipVariantAPI variant) {
        return "graphics/icons/skills.png";
    }

    @Override
    public boolean shouldShow(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return !RefitButtonConfig.buttonUnderHullMods();
    }

    @Override
    public boolean hasPanel(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return true;
    }

    @Override
    public void initPanel(CustomPanelAPI backgroundPanel, FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        float panelWidth = getPanelWidth(member, variant);
        float panelHeight = getPanelHeight(member, variant);

        TooltipMakerAPI shipCard = backgroundPanel.createUIElement(SHIP_CARD_ICON_SIZE, SHIP_CARD_ICON_SIZE, false);
        shipCard.addShipList(1, 1, SHIP_CARD_ICON_SIZE, new Color(0, 0, 0, 0), Collections.singletonList(member), 0f);
        float shipCardHeight = shipCard.getPrev().getPosition().getHeight();

        TooltipMakerAPI element = backgroundPanel.createUIElement(panelWidth, panelHeight, false);
        backgroundPanel.addUIElement(element);
        element.getPosition().inTL(0f, 0f);

        SkillTreeCanvasPlugin plugin = new SkillTreeCanvasPlugin(member, variant, shipCardHeight, this, backgroundPanel);
        CustomPanelAPI canvas = Global.getSettings().createCustom(panelWidth, panelHeight, plugin);
        element.addCustom(canvas, 0f).getPosition().inTL(0f, 0f);

        backgroundPanel.addUIElement(shipCard);
        shipCard.getPosition().inBL(SHIP_CARD_MARGIN, SHIP_CARD_MARGIN);
    }
}
