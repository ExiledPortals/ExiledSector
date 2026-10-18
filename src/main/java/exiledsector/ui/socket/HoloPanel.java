package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.ItemCrafting;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.MaterialItems;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.VanillaText;
import exiledsector.ui.util.FramedPanelPlugin;
import exiledsector.ui.util.HoloFrame;
import exiledsector.ui.util.Rects;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

abstract class HoloPanel extends BaseCustomUIPanelPlugin {

    static final float PAD = 12f;
    static final float GAP = 6f;
    static final float LINE_PAD = 3f;
    static final float HEADER_HEIGHT = 28f;
    static final float HEADER_BUTTON_HEIGHT = 24f;
    static final float CLOSE_BUTTON_WIDTH = 70f;
    private static final float PARTS_ICON_SIZE = 24f;
    static final float PARTS_TEXT_TOP = 5f;
    private static final float PARTS_TOOLTIP_WIDTH = 300f;
    private static final float CONFIRM_BUTTON_HEIGHT = 26f;
    private static final float TAB_TEXT_PADDING = 24f;
    static final float PARTS_ROOM = 80f;

    private record TabButton(SocketPanelTab tab) {
    }

    protected final CustomPanelAPI hostPanel;
    protected final ItemHoverTooltip hoverTooltip;
    private final HoloFrame panelFrame;
    private final List<Runnable> queuedActions = new ArrayList<>();
    protected CustomPanelAPI panelRoot;
    protected PositionAPI panelPosition;
    protected float panelLeft;
    protected float panelTop;
    protected float panelWidth;
    protected float panelHeight;
    private CustomPanelAPI confirmPanel;
    private CustomPanelAPI confirmBlocker;
    private boolean closing;
    private Consumer<SocketPanelTab> tabListener;
    private ButtonAPI activeTabButton;

    HoloPanel(CustomPanelAPI hostPanel) {
        this.hostPanel = hostPanel;
        this.hoverTooltip = new ItemHoverTooltip(hostPanel);
        this.panelFrame = new HoloFrame(HoloFrame.Look.VANILLA_HOST);
    }

    final void attach(float left, float top, float width, float height, boolean instantly) {
        panelLeft = left;
        panelTop = top;
        panelWidth = width;
        panelHeight = height;
        panelRoot = Global.getSettings().createCustom(width, height, this);
        hostPanel.addComponent(panelRoot).inTL(left, top);
        I18n.forGameText(this::build);
        if (instantly) {
            panelFrame.openInstantly();
        } else {
            panelFrame.open();
        }
        refreshOpacity();
    }

    protected abstract void build();

    protected SocketPanelTab tab() {
        return null;
    }

    protected void rebuildHeader() {
    }

    public final void setTabListener(Consumer<SocketPanelTab> listener) {
        if (listener == tabListener) {
            return;
        }
        tabListener = listener;
        if (panelRoot != null) {
            queue(this::rebuildHeader);
        }
    }

    public final void swapOut() {
        if (panelRoot == null || closing) {
            return;
        }
        closing = true;
        hoverTooltip.hide();
        closeConfirm();
        onSwappedOut();
        hostPanel.removeComponent(panelRoot);
        panelRoot = null;
    }

    protected void onSwappedOut() {
    }

    protected abstract UIComponentAPI[] chromeComponents();

    protected abstract UIComponentAPI[] contentComponents();

    protected abstract void onClose();

    protected void onScrollInside() {
    }

    protected void onPressInside() {
    }

    protected void advanceAlways(float amount) {
    }

    protected void advanceOpen(float amount) {
    }

    public final void close() {
        if (panelRoot == null || closing) {
            return;
        }
        closing = true;
        panelFrame.close();
        refreshOpacity();
        onClose();
    }

    public final boolean isVisible() {
        return panelRoot != null;
    }

    final boolean isClosing() {
        return closing;
    }

    public boolean contains(float x, float y) {
        return panelRoot != null && !closing && Rects.contains(panelPosition, x, y);
    }

    final void queue(Runnable action) {
        queuedActions.add(action);
    }

    final void refreshOpacity() {
        float chromeOpacity = panelFrame.chromeAlpha();
        for (UIComponentAPI component : chromeComponents()) {
            if (component != null) {
                component.setOpacity(chromeOpacity);
            }
        }
        float contentOpacity = panelFrame.contentAlpha();
        for (UIComponentAPI component : contentComponents()) {
            if (component != null) {
                component.setOpacity(contentOpacity);
            }
        }
        if (confirmPanel != null) {
            confirmPanel.setOpacity(contentOpacity);
            confirmBlocker.setOpacity(contentOpacity);
        }
    }

    final float contentAlpha() {
        return panelFrame.contentAlpha();
    }

    @Override
    public void positionChanged(PositionAPI panelPosition) {
        this.panelPosition = panelPosition;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (panelPosition == null) {
            return;
        }
        panelFrame.render(panelPosition.getX(), panelPosition.getY(), panelPosition.getWidth(), panelPosition.getHeight(),
                SkillTreePanelStyle.GLOW_COLOR, alphaMult, null);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (closing) {
            return;
        }
        for (InputEventAPI event : events) {
            boolean inside = contains(event.getX(), event.getY());
            if (event.isMouseDownEvent() && inside) {
                onPressInside();
            }
            if (event.isMouseScrollEvent() && inside) {
                onScrollInside();
            }
            boolean press = event.isMouseDownEvent() || event.isMouseScrollEvent();
            if (!event.isConsumed() && press && inside) {
                event.consume();
            }
        }
    }

    @Override
    public void advance(float amount) {
        if (panelFrame.advance(amount)) {
            refreshOpacity();
        }
        advanceAlways(amount);
        if (closing) {
            if (panelFrame.isFullyClosed() && panelRoot != null) {
                hostPanel.removeComponent(panelRoot);
                panelRoot = null;
            }
            return;
        }
        if (!queuedActions.isEmpty()) {
            List<Runnable> actions = List.copyOf(queuedActions);
            queuedActions.clear();
            for (Runnable action : actions) {
                if (panelRoot != null) {
                    I18n.forGameText(action);
                }
            }
        }
        if (panelRoot != null) {
            advanceOpen(amount);
        }
    }

    final TooltipMakerAPI startHeader(String titleKey, String closeKey, Object closeId) {
        TooltipMakerAPI element = panelRoot.createUIElement(panelWidth - PAD * 2f, HEADER_HEIGHT, false);
        activeTabButton = null;
        if (tabListener == null) {
            element.setParaFont(Fonts.ORBITRON_20AABOLD);
            VanillaText.plainPara(element, Translation.text(titleKey), 0f, Misc.getBasePlayerColor());
        } else {
            addTabs(element);
        }
        element.addButton(Translation.text(closeKey), closeId, CLOSE_BUTTON_WIDTH, HEADER_BUTTON_HEIGHT, 0f).getPosition().inTR(0f, 0f);
        return element;
    }

    final UIComponentAPI finishHeader(TooltipMakerAPI element, UIComponentAPI previousHeader) {
        return replaceElement(previousHeader, element, PAD, PAD);
    }

    private void addTabs(TooltipMakerAPI element) {
        float tabLeft = 0f;
        for (SocketPanelTab panelTab : SocketPanelTab.values()) {
            String label = panelTab.label();
            float tabWidth = Global.getSettings().computeStringWidth(label, Fonts.ORBITRON_20AABOLD) + TAB_TEXT_PADDING;
            ButtonAPI tabButton = element.addAreaCheckbox(label, new TabButton(panelTab), Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                    Misc.getBrightPlayerColor(), tabWidth, HEADER_BUTTON_HEIGHT, 0f);
            tabButton.setChecked(panelTab == tab());
            tabButton.getPosition().inTL(tabLeft, 0f);
            if (panelTab == tab()) {
                activeTabButton = tabButton;
            }
            tabLeft += tabWidth + GAP;
        }
    }

    final boolean tabPressed(Object id) {
        if (!(id instanceof TabButton tabButton)) {
            return false;
        }
        queue(() -> {
            if (tabButton.tab() == tab() || tabListener == null) {
                if (activeTabButton != null) {
                    activeTabButton.setChecked(true);
                }
                return;
            }
            tabListener.accept(tabButton.tab());
        });
        return true;
    }

    final TooltipMakerAPI replaceElement(UIComponentAPI previous, TooltipMakerAPI replacement, float left, float top) {
        if (previous != null) {
            panelRoot.removeComponent(previous);
        }
        panelRoot.addUIElement(replacement).inTL(left, top);
        return replacement;
    }

    final boolean isConfirmOpen() {
        return confirmPanel != null;
    }

    final void openConfirm(float confirmHeight, Consumer<TooltipMakerAPI> bodyWriter, String confirmKey, Object confirmId, String cancelKey,
                           Object cancelId) {
        if (confirmPanel != null) {
            return;
        }
        hoverTooltip.hide();
        confirmBlocker = Global.getSettings().createCustom(panelWidth, panelHeight, new InputBlocker());
        panelRoot.addComponent(confirmBlocker).inTL(0f, 0f);
        float confirmWidth = panelWidth - PAD * 2f;
        confirmPanel = Global.getSettings().createCustom(confirmWidth, confirmHeight, new FramedPanelPlugin(this::buttonPressed));
        TooltipMakerAPI element = confirmPanel.createUIElement(confirmWidth - PAD * 2f, confirmHeight - PAD * 2f, false);
        bodyWriter.accept(element);
        float buttonWidth = (confirmWidth - PAD * 2f - GAP) / 2f;
        element.addButton(Translation.text(confirmKey), confirmId, buttonWidth, CONFIRM_BUTTON_HEIGHT, 0f).getPosition().inBR(0f, 0f);
        element.addButton(Translation.text(cancelKey), cancelId, buttonWidth, CONFIRM_BUTTON_HEIGHT, 0f).getPosition().inBL(0f, 0f);
        confirmPanel.addUIElement(element).inTL(PAD, PAD);
        panelRoot.addComponent(confirmPanel).inTL(PAD, (panelHeight - confirmHeight) / 2f);
        refreshOpacity();
    }

    final void closeConfirm() {
        if (confirmPanel == null) {
            return;
        }
        panelRoot.removeComponent(confirmPanel);
        panelRoot.removeComponent(confirmBlocker);
        confirmPanel = null;
        confirmBlocker = null;
    }

    static UIComponentAPI placedComponent(TooltipMakerAPI element) {
        if (element == null) {
            return null;
        }
        return element.getExternalScroller() != null ? element.getExternalScroller() : element;
    }

    static CraftingMaterials playerMaterials() {
        return CraftingMaterials.forPlayer(PlayerFleet.cargoOrNull());
    }

    static void addPartsCount(TooltipMakerAPI element, float countRight) {
        String partsIconPath = MaterialItems.loadedIconPath(ItemDisassembly.PARTS_ITEM_ID);
        String partsName = MaterialItems.displayName(ItemDisassembly.PARTS_ITEM_ID);
        String countText = String.valueOf(ItemCrafting.parts(playerMaterials()));
        float countWidth = Global.getSettings().computeStringWidth(countText, Fonts.ORBITRON_12) + LINE_PAD;
        element.setParaFont(Fonts.ORBITRON_12);
        LabelAPI countLabel = VanillaText.plainPara(element, countText, 0f, Misc.getTextColor());
        countLabel.autoSizeToWidth(countWidth).inTR(countRight, PARTS_TEXT_TOP);
        if (partsIconPath != null) {
            element.addImage(partsIconPath, PARTS_ICON_SIZE, PARTS_ICON_SIZE, 0f);
            element.getPrev().getPosition().inTR(countRight + countWidth + LINE_PAD, 0f);
        }
        element.addTooltipToPrevious(new GameTextTooltip(PARTS_TOOLTIP_WIDTH,
                tooltip -> VanillaText.plainPara(tooltip, Translation.msg("ui.socketStorage.parts.tooltip").arg("name", partsName).text(),
                        0f, Misc.getTextColor())),
                TooltipMakerAPI.TooltipLocation.BELOW);
    }
}
