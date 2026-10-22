package exiledsector.ui.cargo;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.ui.ModificationBenchCargoLauncher;
import exiledsector.ui.refit.RefitButtonConfig;
import exiledsector.ui.refit.UiReflection;
import org.apache.log4j.Logger;

public final class CargoCraftingButton extends BaseCustomUIPanelPlugin {

    private static final Logger LOG = Logger.getLogger(CargoCraftingButton.class);
    static final String BUTTON_ID = "exiledSector_cargoCraftingButton";
    static final float BUTTON_WIDTH = 263f;
    static final float BUTTON_HEIGHT = 25f;
    static final float GAP_BELOW_TRANSACTION_PANEL = 10f;

    private static boolean failed;

    private final UIPanelAPI tradePanel;
    private CustomPanelAPI buttonContainer;
    private ButtonAPI craftingButton;
    private boolean shown = true;
    private boolean usable = true;

    private CargoCraftingButton(UIPanelAPI tradePanel) {
        this.tradePanel = tradePanel;
    }

    static boolean failed() {
        return failed;
    }

    static UIPanelAPI tradePanel(CampaignUIAPI campaignUI) {
        if (failed) return null;
        try {
            Object cargoTab = UiReflection.call(UiReflection.coreUI(campaignUI), "getCurrentTab");
            if (Boolean.TRUE.equals(UiReflection.call(cargoTab, "isLootMode"))) return null;
            return UiReflection.call(cargoTab, "getTradePanel") instanceof UIPanelAPI panel ? panel : null;
        } catch (Throwable e) {
            disable(e);
            return null;
        }
    }

    static void attachIfMissing(UIPanelAPI tradePanel) {
        if (failed || tradePanel == null) return;
        try {
            for (Object child : UiReflection.children(tradePanel)) {
                if (child instanceof CustomPanelAPI panel && panel.getPlugin() instanceof CargoCraftingButton) return;
            }
            attach(tradePanel);
        } catch (Throwable e) {
            disable(e);
        }
    }

    private static void attach(UIPanelAPI tradePanel) throws Throwable {
        if (!(UiReflection.childWithMethod(tradePanel, "getTakeAll", 0) instanceof UIComponentAPI transactionPanel)) {
            throw new IllegalStateException("The inventory screen has no transaction panel to anchor the Crafting button below");
        }
        CargoCraftingButton plugin = new CargoCraftingButton(tradePanel);
        CustomPanelAPI container = Global.getSettings().createCustom(BUTTON_WIDTH, BUTTON_HEIGHT, plugin);
        TooltipMakerAPI element = container.createUIElement(BUTTON_WIDTH, BUTTON_HEIGHT, false);
        element.setButtonFontOrbitron20Bold();
        ButtonAPI button = element.addButton(Translation.gameText("ui.cargoCraftingButton"), BUTTON_ID,
                Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.ALL, BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
        int hotkey = RefitButtonConfig.benchKey();
        if (hotkey > 0) button.setShortcut(hotkey, true);
        plugin.buttonContainer = container;
        plugin.craftingButton = button;
        container.addUIElement(element);
        if (showsMarketTitleRow(tradePanel)) {
            if (!(UiReflection.call(tradePanel, "getEntityCargoDisplay") instanceof UIComponentAPI marketCargoGrid)) {
                throw new IllegalStateException("The market screen has no cargo grid to align the Crafting button with");
            }
            tradePanel.addComponent(container).inTL(marketCargoGrid.getPosition().getWidth() - BUTTON_WIDTH, 0f);
        } else {
            tradePanel.addComponent(container).belowLeft(transactionPanel, GAP_BELOW_TRANSACTION_PANEL);
        }
        plugin.refreshState();
    }

    private static boolean showsMarketTitleRow(UIPanelAPI tradePanel) throws Throwable {
        Object colonyInfoButton = UiReflection.call(tradePanel, "getMarketInfo");
        return colonyInfoButton != null && UiReflection.children(tradePanel).contains(colonyInfoButton);
    }

    static void resetForTests() {
        failed = false;
    }

    private static void disable(Throwable e) {
        failed = true;
        LOG.error("[ExiledSector] Could not add the Crafting button to the inventory screen; it stays off until the game restarts", e);
    }

    @Override
    public void advance(float amount) {
        if (craftingButton == null) return;
        try {
            refreshState();
        } catch (Throwable e) {
            disable(e);
            show(false, false);
        }
    }

    private void refreshState() throws Throwable {
        boolean nowShown = !failed && !Boolean.TRUE.equals(UiReflection.call(tradePanel, "isShowingMarketInfo"));
        show(nowShown, nowShown && !transferPending());
    }

    private void show(boolean nowShown, boolean nowUsable) {
        if (nowShown != shown) {
            shown = nowShown;
            buttonContainer.setOpacity(shown ? 1f : 0f);
        }
        if (nowUsable != usable) {
            usable = nowUsable;
            craftingButton.setEnabled(usable);
        }
    }

    private boolean transferPending() throws Throwable {
        Object transfers = UiReflection.call(tradePanel, "getTransferHandler");
        return Boolean.TRUE.equals(UiReflection.call(transfers, "transactionExists")) || UiReflection.call(transfers, "getPickedUpStack") != null;
    }

    @Override
    public void buttonPressed(Object buttonId) {
        if (!usable || failed || !BUTTON_ID.equals(buttonId)) return;
        ModificationBenchCargoLauncher.open(Global.getSector().getCampaignUI(), this::refreshCargoViews);
    }

    private void refreshCargoViews() {
        try {
            UiReflection.call(tradePanel, "updateCargoViews");
        } catch (Throwable e) {
            LOG.warn("[ExiledSector] Could not refresh the inventory grids after crafting; reopen the inventory to see the changes", e);
        }
    }
}
