package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.socketables.SocketMaterials;
import exiledsector.socketables.SocketableCrafting;
import exiledsector.socketables.SocketableDisassembly;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.util.HoloFrame;
import exiledsector.ui.util.Rects;

import java.util.ArrayList;
import java.util.List;

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

    protected final CustomPanelAPI hostPanel;
    private final HoloFrame panelFrame;
    private final List<Runnable> queuedActions = new ArrayList<>();
    protected CustomPanelAPI panelRoot;
    protected PositionAPI panelPosition;
    protected float panelLeft;
    protected float panelTop;
    protected float panelWidth;
    protected float panelHeight;
    private boolean closing;

    HoloPanel(CustomPanelAPI hostPanel, Class<?> owner) {
        this.hostPanel = hostPanel;
        this.panelFrame = new HoloFrame(owner, HoloFrame.Look.VANILLA_HOST);
    }

    final void attach(float left, float top, float width, float height) {
        panelLeft = left;
        panelTop = top;
        panelWidth = width;
        panelHeight = height;
        panelRoot = Global.getSettings().createCustom(width, height, this);
        hostPanel.addComponent(panelRoot).inTL(left, top);
        I18n.forGameText(this::build);
        panelFrame.open();
        refreshOpacity();
    }

    protected abstract void build();

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
        element.setParaFont(Fonts.ORBITRON_20AABOLD);
        element.addPara("%s", 0f, Misc.getBasePlayerColor(), Misc.getBasePlayerColor(), Translation.text(titleKey));
        element.addButton(Translation.text(closeKey), closeId, CLOSE_BUTTON_WIDTH, HEADER_BUTTON_HEIGHT, 0f).getPosition().inTR(0f, 0f);
        return element;
    }

    final UIComponentAPI finishHeader(TooltipMakerAPI element, UIComponentAPI previousHeader, float partsRight) {
        if (previousHeader != null) {
            panelRoot.removeComponent(previousHeader);
        }
        addPartsCount(element, partsRight);
        panelRoot.addUIElement(element).inTL(PAD, PAD);
        return element;
    }

    static CargoAPI playerCargo() {
        CampaignFleetAPI fleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        return fleet == null ? null : fleet.getCargo();
    }

    static SocketMaterials playerMaterials() {
        return SocketMaterials.forPlayer(playerCargo());
    }

    private static void addPartsCount(TooltipMakerAPI element, float countRight) {
        CommoditySpecAPI partsSpec = Global.getSettings().getCommoditySpec(SocketableDisassembly.PARTS_COMMODITY_ID);
        if (partsSpec == null) {
            return;
        }
        String countText = String.valueOf(SocketableCrafting.parts(playerMaterials()));
        float countWidth = Global.getSettings().computeStringWidth(countText, Fonts.ORBITRON_12) + LINE_PAD;
        element.setParaFont(Fonts.ORBITRON_12);
        LabelAPI countLabel = element.addPara("%s", 0f, Misc.getTextColor(), Misc.getTextColor(), countText);
        countLabel.autoSizeToWidth(countWidth).inTR(countRight, PARTS_TEXT_TOP);
        element.addImage(partsSpec.getIconName(), PARTS_ICON_SIZE, PARTS_ICON_SIZE, 0f);
        element.getPrev().getPosition().inTR(countRight + countWidth + LINE_PAD, 0f);
        element.addTooltipToPrevious(new GameTextTooltip(PARTS_TOOLTIP_WIDTH,
                tooltip -> tooltip.addPara("%s", 0f, Misc.getTextColor(), Misc.getTextColor(),
                        Translation.msg("ui.socketStorage.parts.tooltip").arg("name", partsSpec.getName()).text())),
                TooltipMakerAPI.TooltipLocation.BELOW);
    }
}
