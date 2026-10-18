package exiledsector.ui;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import exiledsector.i18n.Translation;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipLevelSystem;
import exiledsector.skills.progression.ShipOpBudget;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.ui.util.BorderedPanel;

final class SkillTreeChrome {

    private static final float SHIP_CARD_FRAME_OUTSET = 8f;
    private static final int READOUT_ROWS = 3;

    private final SkillTreeStatPanel statPanel;
    private final SkillTreeSearchBar searchBar;
    private final SkillTreeTemplateController templateUi;
    private final SocketPlacement socketPlacement;
    private final FleetMemberAPI member;
    private final SkillTreeReadoutBar ordnancePointsBar = new SkillTreeReadoutBar(SkillTreeChrome.class, 0, "ui.readout.opBar");
    private final SkillTreeLevelBar levelBar;
    private final SkillTreeReadoutBar nodesBar = new SkillTreeReadoutBar(SkillTreeChrome.class, 2, "ui.readout.nodesBar");
    private final int maxAllocatedNodes = ShipLevelConfig.maxAllocatedNodes();
    private final SkillTreeInfoTooltipRenderer infoTooltipRenderer;
    private final String readoutTooltipTitle = Translation.text("ui.readout.title");
    private final String readoutTooltipBody;
    private final String storageTooltipTitle = Translation.text("ui.hullItems.title");
    private final String storageTooltipHint = Translation.text("ui.socketStorage.hint");
    private final ChromeVisibility visibility = new ChromeVisibility();
    private final float shipCardHeight;
    private ScreenRect shipCardFrame = ScreenRect.NONE;
    private float shipCardFrameCanvasX = Float.NaN;
    private float shipCardFrameCanvasY = Float.NaN;
    private UIComponentAPI shipCard;
    private boolean shipCardHidden;

    SkillTreeChrome(FleetMemberAPI member, SkillTreePanelStyle panelStyle, SkillTreeStatPanel statPanel, SkillTreeSearchBar searchBar,
                    SkillTreeTemplateController templateUi, SocketPlacement socketPlacement, float shipCardHeight) {
        this.member = member;
        this.statPanel = statPanel;
        this.searchBar = searchBar;
        this.templateUi = templateUi;
        this.socketPlacement = socketPlacement;
        this.levelBar = new SkillTreeLevelBar(member, 1);
        this.infoTooltipRenderer = new SkillTreeInfoTooltipRenderer(panelStyle);
        this.readoutTooltipBody = buildReadoutTooltipBody(member);
        this.shipCardHeight = shipCardHeight;
    }

    void setShipCard(UIComponentAPI component) {
        shipCard = component;
        shipCardHidden = false;
    }

    void advanceVisibility(float amount, CanvasMode mode, float chromeAlpha, float treeAlpha) {
        visibility.advance(amount, mode, chromeAlpha, treeAlpha);
    }

    void advance(float amount, PositionAPI canvasPosition, CanvasMode mode, ShipOpBudget budget, float mouseX, float mouseY,
                 boolean mouseKnown) {
        boolean storageButtonShown = visibility.alpha(CanvasMode.Chrome.STORAGE_BUTTON) > 0f;
        socketPlacement.layoutButton(canvasPosition, shipCardFrame(canvasPosition), storageButtonShown);
        if (!mode.enables(CanvasMode.Chrome.SEARCH)) {
            searchBar.unfocus();
        }
        hideShipCard(mode == CanvasMode.MODAL);
        boolean readoutsLive = mouseKnown && mode.enables(CanvasMode.Chrome.READOUTS);
        ordnancePointsBar.advance(amount, canvasPosition, budget.usedOp, budget.totalOp, mouseX, mouseY, readoutsLive);
        levelBar.advance(amount, canvasPosition, mouseX, mouseY, readoutsLive);
        nodesBar.advance(amount, canvasPosition, ShipSkillDataManager.get(member.getId()).getAllocatedNodeIds().size(), maxAllocatedNodes, mouseX,
                mouseY, readoutsLive);
    }

    private void hideShipCard(boolean hide) {
        if (shipCard != null && hide != shipCardHidden) {
            shipCardHidden = hide;
            shipCard.setOpacity(hide ? 0f : 1f);
        }
    }

    boolean contains(PositionAPI canvasPosition, CanvasMode mode, float x, float y) {
        return (mode.shows(CanvasMode.Chrome.STATS_TOGGLE) && statPanel.contains(x, y))
                || ordnancePointsBar.isHovered(canvasPosition, x, y)
                || levelBar.isHovered(canvasPosition, x, y)
                || nodesBar.isHovered(canvasPosition, x, y)
                || (mode.shows(CanvasMode.Chrome.SEARCH) && SkillTreeSearchBar.contains(canvasPosition, x, y))
                || (mode.shows(CanvasMode.Chrome.TEMPLATE_BAR) && templateUi.barContains(x, y))
                || (mode.shows(CanvasMode.Chrome.STORAGE_BUTTON) && socketPlacement.buttonContains(x, y))
                || socketPlacement.panelContains(x, y)
                || shipCardFrame(canvasPosition).contains(x, y);
    }

    boolean press(PositionAPI canvasPosition, CanvasMode mode, float x, float y) {
        templateUi.forgetBarPress();
        if (mode.enables(CanvasMode.Chrome.SEARCH) && searchBar.handleClick(canvasPosition, x, y)) {
            return true;
        }
        if (mode.enables(CanvasMode.Chrome.STORAGE_BUTTON) && socketPlacement.isButtonClickable(x, y)) {
            socketPlacement.toggle(canvasPosition, shipCardFrame(canvasPosition));
            return true;
        }
        if (mode.enables(CanvasMode.Chrome.TEMPLATE_BAR) && templateUi.handleLmbDown(x, y)) {
            return true;
        }
        if (statsToggleLive(mode) && statPanel.isToggleHit(x, y)) {
            statPanel.toggle();
            return true;
        }
        return contains(canvasPosition, mode, x, y);
    }

    boolean release(float x, float y) {
        return templateUi.handleLmbUp(x, y);
    }

    private boolean statsToggleLive(CanvasMode mode) {
        return mode.enables(CanvasMode.Chrome.STATS_TOGGLE);
    }

    void render(PositionAPI canvasPosition, CanvasMode mode, float mouseX, float mouseY, float alphaMult, float workbenchDim) {
        statPanel.render(canvasPosition, mouseX, mouseY, alphaMult, visibility.alpha(CanvasMode.Chrome.STATS_TOGGLE) * (1f - workbenchDim),
                statsToggleLive(mode));
        float readoutAlpha = alphaMult * visibility.alpha(CanvasMode.Chrome.READOUTS);
        ordnancePointsBar.render(canvasPosition, readoutAlpha, null);
        levelBar.render(canvasPosition, readoutAlpha);
        nodesBar.render(canvasPosition, readoutAlpha, null);
        float searchAlpha = visibility.alpha(CanvasMode.Chrome.SEARCH);
        if (searchAlpha > 0f) {
            searchBar.render(canvasPosition, alphaMult * searchAlpha);
        }
        float templateBarAlpha = visibility.alpha(CanvasMode.Chrome.TEMPLATE_BAR);
        if (templateBarAlpha > 0f) {
            templateUi.renderBar(canvasPosition, mouseX, mouseY, alphaMult * templateBarAlpha);
        }
        socketPlacement.renderButton(mouseX, mouseY, alphaMult * visibility.alpha(CanvasMode.Chrome.STORAGE_BUTTON),
                mode.enables(CanvasMode.Chrome.STORAGE_BUTTON));
        ScreenRect frame = shipCardFrame(canvasPosition);
        BorderedPanel.draw(frame.left(), frame.bottom(), frame.width(), frame.height(), alphaMult);
    }

    void renderTooltips(PositionAPI canvasPosition, CanvasMode mode, float mouseX, float mouseY, float alphaMult) {
        if (!mode.showsChromeTooltips()) {
            return;
        }
        if (mode.enables(CanvasMode.Chrome.READOUTS)
                && readoutHovered(canvasPosition, mouseX, mouseY)) {
            infoTooltipRenderer.render(readoutTooltipTitle, readoutTooltipBody, mouseX, mouseY, alphaMult);
        }
        if (mode.enables(CanvasMode.Chrome.STORAGE_BUTTON) && socketPlacement.buttonContains(mouseX, mouseY)) {
            infoTooltipRenderer.render(storageTooltipTitle, storageTooltipHint, mouseX, mouseY, alphaMult);
        }
        if (mode.enables(CanvasMode.Chrome.TEMPLATE_BAR)) {
            templateUi.renderBarTooltip(mouseX, mouseY, alphaMult);
        }
    }

    ScreenRect shipCardFrame(PositionAPI canvasPosition) {
        float canvasX = canvasPosition.getX();
        float canvasY = canvasPosition.getY();
        if (canvasX != shipCardFrameCanvasX || canvasY != shipCardFrameCanvasY) {
            shipCardFrameCanvasX = canvasX;
            shipCardFrameCanvasY = canvasY;
            shipCardFrame = new ScreenRect(canvasX + ChromeMetrics.EDGE_MARGIN - SHIP_CARD_FRAME_OUTSET,
                    canvasY + ChromeMetrics.EDGE_MARGIN - SHIP_CARD_FRAME_OUTSET,
                    SkillTreeRefitButton.SHIP_CARD_ICON_SIZE + SHIP_CARD_FRAME_OUTSET * 2f, shipCardHeight + SHIP_CARD_FRAME_OUTSET * 2f);
        }
        return shipCardFrame;
    }

    private boolean readoutHovered(PositionAPI canvasPosition, float x, float y) {
        return ordnancePointsBar.isHovered(canvasPosition, x, y) || levelBar.isHovered(canvasPosition, x, y) || nodesBar.isHovered(canvasPosition, x, y);
    }

    static float sidePanelTop() {
        return SkillTreeReadoutBar.stackHeight(READOUT_ROWS) + ChromeMetrics.EDGE_MARGIN;
    }

    private static String buildReadoutTooltipBody(FleetMemberAPI member) {
        int opCost = SkillNodeOpCost.perNode(member.getHullSpec());
        int maxNodes = ShipLevelConfig.maxAllocatedNodes();
        return Translation.msg("ui.readout.body").arg("opCost", opCost).arg("maxNodes", maxNodes)
                .arg("xpRules", xpRulesText()).text();
    }

    static String xpRulesText() {
        int lossPercent = Math.round(ShipLevelConfig.xpLossMultiplier() * 100f);
        float maxMultiplier = ShipLevelSystem.difficultyMultiplier(FleetEncounterContext.MAX_XP_MULT,
                ShipLevelConfig.xpDifficultyStrength(), ShipLevelConfig.xpDifficultyMaxMultiplier());
        int maxBonus = Math.round((maxMultiplier - 1f) * 100f);
        if (maxBonus < 1) {
            return Translation.msg("ui.readout.xp").arg("lossPercent", lossPercent).text();
        }
        return Translation.msg("ui.readout.xpWithDifficulty").arg("lossPercent", lossPercent).arg("maxBonus", maxBonus).text();
    }
}
