package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.ShipHullSizes;
import exiledsector.hullitems.FrameworkSockets;
import exiledsector.hullitems.HullUpgradeData;
import exiledsector.hullitems.HullUpgradeTooltip;
import exiledsector.hullitems.SocketType;
import exiledsector.hullitems.HullItemRarity;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.VanillaText;
import exiledsector.ui.framework.FrameworkSocketFlair;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

public final class HullUpgradePanel extends HoloPanel {

    public interface Listener {

        void installUpgrade();

        void toggleSocket(SocketType socketType);

        void closed();
    }

    public enum SocketState {UNLOCKED, AVAILABLE, RESTRICTED, NO_POINTS}

    public record SocketEntry(SocketType type, SocketState state, String requirementText, String itemIconPath) {
    }

    public record View(HullSize hullSize, int points, int unspentPoints, int upgradesInStorage,
                       FrameworkSockets.UpgradeBlock upgradeBlock, List<SocketEntry> sockets) {
    }

    public static final float WIDTH = SocketStoragePanel.WIDTH;
    private static final int MIN_SOCKET_COLUMNS = 3;
    private static final int MAX_SOCKET_COLUMNS = 6;
    private static final float UPGRADE_TILE_SIZE = 64f;
    private static final float POINTS_WIDTH = 80f;
    private static final float MIN_TILE_SIZE = 64f;

    public enum Sizing {FIT_HEIGHT, SCROLL}
    private static final float FIT_SLACK = 12f;
    private static final float SCROLLBAR_ROOM = 14f;
    private static final float NAME_HEIGHT = 22f;
    private static final float SOCKET_NAME_HEIGHT = 36f;
    private static final float STATE_HEIGHT = 20f;
    private static final float TOOLTIP_WIDTH = 520f;
    private static final Color BLOCKED_TEXT_COLOR = new Color(130, 130, 130);
    private static final float CONFIRM_HEIGHT = 170f;
    private static final float POINTS_LINE_HEIGHT = 30f;

    private enum Control {CLOSE, CONFIRM_INSTALL, CANCEL_INSTALL}

    record TileSizes(int socketColumns, float columnWidth, float socketTile) {
    }

    private final Listener panelListener;
    private View view;
    private Sizing sizing = Sizing.FIT_HEIGHT;
    private UIComponentAPI headerElement;
    private TooltipMakerAPI listElement;

    private HullUpgradePanel(CustomPanelAPI hostPanel, View view, Listener panelListener) {
        super(hostPanel);
        this.view = view;
        this.panelListener = panelListener;
    }

    public static HullUpgradePanel open(CustomPanelAPI hostPanel, SocketPanelPlacement placement, View view, Listener panelListener,
                                        Consumer<SocketPanelTab> tabListener, Sizing sizing) {
        HullUpgradePanel panel = new HullUpgradePanel(hostPanel, view, panelListener);
        panel.sizing = sizing;
        panel.setTabListener(tabListener);
        panel.attach(placement.panelLeft(), placement.panelTop(), placement.panelWidth(), placement.panelHeight(), placement.instantly());
        return panel;
    }

    public void update(View updatedView) {
        queue(() -> {
            view = updatedView;
            rebuildList();
        });
    }

    @Override
    protected void build() {
        rebuildHeader();
        rebuildList();
    }

    @Override
    protected SocketPanelTab tab() {
        return SocketPanelTab.HULL_UPGRADES;
    }

    @Override
    protected void rebuildHeader() {
        closeConfirm();
        headerElement = finishHeader(startHeader("framework.panel.title", "ui.socketStorage.close", Control.CLOSE), headerElement);
        refreshOpacity();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{placedComponent(listElement)};
    }

    @Override
    protected void onScrollInside() {
        queue(hoverTooltip::hide);
    }

    @Override
    protected void advanceOpen(float amount) {
        hoverTooltip.refreshIfExpansionChanged();
    }

    @Override
    protected void onClose() {
        hoverTooltip.hide();
        panelListener.closed();
    }

    @Override
    public void buttonPressed(Object id) {
        if (tabPressed(id)) {
            return;
        }
        queue(() -> {
            if (id == Control.CLOSE) {
                close();
            } else if (id == Control.CONFIRM_INSTALL) {
                closeConfirm();
                panelListener.installUpgrade();
            } else if (id == Control.CANCEL_INSTALL) {
                closeConfirm();
            }
        });
    }

    public boolean escape() {
        if (!isConfirmOpen()) {
            return false;
        }
        queue(this::closeConfirm);
        return true;
    }

    private void openInstallConfirm() {
        HullUpgradeData upgrade = new HullUpgradeData(view.hullSize());
        openConfirm(CONFIRM_HEIGHT, element -> {
            Color uniqueColor = HullItemRarity.UNIQUE.color();
            VanillaText.plainPara(element, Translation.msg("framework.confirm.title").arg("name", upgrade.name()).text(), 0f, uniqueColor);
            VanillaText.plainPara(element, Translation.msg("framework.confirm.body")
                    .arg("left", view.upgradesInStorage() - 1).arg("points", view.points() + 1).arg("max", FrameworkSockets.MAX_POINTS).text(),
                    LINE_PAD * 2f, Misc.getTextColor());
        }, "framework.confirm.install", Control.CONFIRM_INSTALL, "framework.confirm.cancel", Control.CANCEL_INSTALL);
    }

    private void rebuildList() {
        hoverTooltip.hide();
        closeConfirm();
        float listWidth = panelWidth - PAD * 2f;
        float listTop = PAD + HEADER_HEIGHT + GAP;
        float listHeight = panelHeight - listTop - PAD;
        TooltipMakerAPI element = panelRoot.createUIElement(listWidth, listHeight, true);
        float gridWidth = listWidth - SCROLLBAR_ROOM;
        element.addSectionHeading(Translation.text("framework.panel.upgrades"), Alignment.MID, 0f);
        element.addCustom(upgradeRow(gridWidth), GAP * 2f);
        VanillaText.plainPara(element, Translation.msg("framework.panel.hint").arg("max", FrameworkSockets.MAX_POINTS).text(), GAP * 2f,
                Misc.getGrayColor());
        element.addSectionHeading(Translation.text("framework.panel.sockets"), Alignment.MID, GAP * 3f);
        TileSizes tileSizes = sizing == Sizing.SCROLL ? scrollingTileSizes(gridWidth)
                : tileSizes(gridWidth, listHeight - element.getHeightSoFar() - FIT_SLACK, view.sockets().size());
        int socketColumns = tileSizes.socketColumns();
        float columnWidth = tileSizes.columnWidth();
        float cellHeight = tileSizes.socketTile() + SOCKET_NAME_HEIGHT + STATE_HEIGHT;
        List<SocketEntry> sockets = view.sockets();
        for (int start = 0; start < sockets.size(); start += socketColumns) {
            int rowCount = Math.min(sockets.size(), start + socketColumns) - start;
            float rowLeft = (gridWidth - rowCount * columnWidth - (rowCount - 1) * GAP) / 2f;
            CustomPanelAPI rowPanel = Global.getSettings().createCustom(gridWidth, cellHeight, null);
            for (int i = 0; i < rowCount; i++) {
                rowPanel.addComponent(socketCell(sockets.get(start + i), columnWidth, tileSizes.socketTile()))
                        .inTL(rowLeft + i * (columnWidth + GAP), 0f);
            }
            element.addCustom(rowPanel, GAP * 2f);
        }
        listElement = replaceElement(placedComponent(listElement), element, PAD, listTop);
        refreshOpacity();
    }

    static TileSizes scrollingTileSizes(float gridWidth) {
        float columnWidth = (gridWidth - GAP * (MIN_SOCKET_COLUMNS - 1)) / MIN_SOCKET_COLUMNS;
        return new TileSizes(MIN_SOCKET_COLUMNS, columnWidth, columnWidth);
    }

    static TileSizes tileSizes(float gridWidth, float tileRoom, int socketCount) {
        TileSizes best = null;
        for (int socketColumns = MIN_SOCKET_COLUMNS; socketColumns <= MAX_SOCKET_COLUMNS; socketColumns++) {
            int rows = (socketCount + socketColumns - 1) / socketColumns;
            float columnWidth = (gridWidth - GAP * (socketColumns - 1)) / socketColumns;
            float rowLabels = rows * (SOCKET_NAME_HEIGHT + STATE_HEIGHT + GAP * 2f);
            float heightLimit = (tileRoom - rowLabels) / rows;
            float socketTile = Math.min(columnWidth, Math.max(MIN_TILE_SIZE, heightLimit));
            if (best == null || socketTile > best.socketTile()) {
                best = new TileSizes(socketColumns, columnWidth, socketTile);
            }
        }
        return best;
    }

    private CustomPanelAPI upgradeRow(float gridWidth) {
        HullUpgradeData upgrade = new HullUpgradeData(view.hullSize());
        boolean blocked = view.upgradeBlock() != null;
        CustomPanelAPI rowPanel = Global.getSettings().createCustom(gridWidth, UPGRADE_TILE_SIZE, null);
        IconTileCell.Hooks hooks = new IconTileCell.Hooks(() -> queue(this::openInstallConfirm),
                tilePosition -> queue(() -> showUpgradeTooltip(upgrade, tilePosition)), () -> queue(() -> hideTooltipOf(upgrade)));
        rowPanel.addComponent(Global.getSettings().createCustom(UPGRADE_TILE_SIZE, UPGRADE_TILE_SIZE,
                new IconTileCell(upgrade.iconPath(), HullItemRarity.UNIQUE.color(), blocked, hooks))).inTL(0f, 0f);
        float textLeft = UPGRADE_TILE_SIZE + GAP * 2f;
        float textWidth = gridWidth - textLeft - POINTS_WIDTH;
        float textTop = (UPGRADE_TILE_SIZE - NAME_HEIGHT - STATE_HEIGHT) / 2f;
        TooltipMakerAPI labelElement = rowPanel.createUIElement(textWidth, NAME_HEIGHT + STATE_HEIGHT, false);
        addLabel(labelElement, upgrade.name(), blocked ? BLOCKED_TEXT_COLOR : HullItemRarity.UNIQUE.color(), Alignment.LMID, textWidth, 0f,
                NAME_HEIGHT);
        addLabel(labelElement, Translation.msg("framework.panel.inStorage").arg("count", view.upgradesInStorage()).text(),
                Misc.getGrayColor(), Alignment.LMID, textWidth, NAME_HEIGHT, STATE_HEIGHT);
        rowPanel.addUIElement(labelElement).inTL(textLeft, textTop);
        TooltipMakerAPI pointsElement = rowPanel.createUIElement(POINTS_WIDTH, POINTS_LINE_HEIGHT, false);
        pointsElement.setParaFont(Fonts.ORBITRON_24AABOLD);
        String points = (view.points() - view.unspentPoints()) + "/" + view.points();
        addLabel(pointsElement, points, SkillTreePanelStyle.OP_TEXT_COLOR, Alignment.RMID, POINTS_WIDTH, 0f, POINTS_LINE_HEIGHT);
        rowPanel.addUIElement(pointsElement).inTL(gridWidth - POINTS_WIDTH, (UPGRADE_TILE_SIZE - POINTS_LINE_HEIGHT) / 2f);
        return rowPanel;
    }

    private String upgradeStateText() {
        FrameworkSockets.UpgradeBlock block = view.upgradeBlock();
        if (block == null) {
            return Translation.text("framework.panel.upgradeReady");
        }
        return switch (block) {
            case AT_MAX_POINTS -> Translation.msg("framework.panel.upgradeAtMax").arg("max", FrameworkSockets.MAX_POINTS).text();
            case NONE_IN_STORAGE -> Translation.msg("framework.panel.upgradeNone")
                    .arg("size", ShipHullSizes.displayName(view.hullSize())).text();
        };
    }

    private CustomPanelAPI socketCell(SocketEntry entry, float columnWidth, float tileWidth) {
        CustomPanelAPI cellPanel = Global.getSettings().createCustom(columnWidth, tileWidth + SOCKET_NAME_HEIGHT + STATE_HEIGHT, null);
        IconTileCell.Hooks hooks = new IconTileCell.Hooks(() -> queue(() -> panelListener.toggleSocket(entry.type())),
                tilePosition -> queue(() -> showSocketTooltip(entry, tilePosition)), () -> queue(() -> hideTooltipOf(entry)));
        cellPanel.addComponent(Global.getSettings().createCustom(tileWidth, tileWidth, new SocketTypeTile(entry.type(), entry.state(),
                entry.itemIconPath(), hooks))).inTL((columnWidth - tileWidth) / 2f, 0f);
        TooltipMakerAPI labelElement = cellPanel.createUIElement(columnWidth, SOCKET_NAME_HEIGHT + STATE_HEIGHT, false);
        boolean blocked = entry.state() != SocketState.UNLOCKED;
        addLabel(labelElement, entry.type().displayName(), blocked ? BLOCKED_TEXT_COLOR : FrameworkSocketFlair.colorOf(entry.type()),
                Alignment.MID, columnWidth, 0f, SOCKET_NAME_HEIGHT);
        String stateText = stateText(entry.state());
        if (stateText != null) {
            addLabel(labelElement, stateText, stateColor(entry.state()), Alignment.MID, columnWidth, SOCKET_NAME_HEIGHT, STATE_HEIGHT);
        }
        cellPanel.addUIElement(labelElement).inTL(0f, tileWidth);
        return cellPanel;
    }

    private static String stateText(SocketState state) {
        return switch (state) {
            case UNLOCKED -> Translation.text("framework.socketState.unlocked");
            case AVAILABLE -> Translation.text("framework.socketState.available");
            case RESTRICTED, NO_POINTS -> null;
        };
    }

    private static Color stateColor(SocketState state) {
        return state == SocketState.UNLOCKED ? Misc.getHighlightColor() : Misc.getPositiveHighlightColor();
    }

    private static void addLabel(TooltipMakerAPI element, String text, Color color, Alignment alignment, float width, float top,
                                 float height) {
        LabelAPI label = VanillaText.plainPara(element, text, 0f, color);
        label.setAlignment(alignment);
        label.getPosition().setSize(width, height - LINE_PAD).inTL(0f, top + LINE_PAD);
    }

    private void hideTooltipOf(Object tooltipKey) {
        if (hoverTooltip.isShowing(tooltipKey)) {
            hoverTooltip.hide();
        }
    }

    private void showUpgradeTooltip(HullUpgradeData upgrade, PositionAPI tilePosition) {
        if (isConfirmOpen()) {
            return;
        }
        hoverTooltip.show(upgrade, TOOLTIP_WIDTH, (tooltip, expanded) -> HullUpgradeTooltip.write(tooltip, upgrade,
                () -> List.of(StyledText.of(upgradeStateText()))), tilePosition);
    }

    private void showSocketTooltip(SocketEntry entry, PositionAPI tilePosition) {
        if (isConfirmOpen()) {
            return;
        }
        hoverTooltip.show(entry, TOOLTIP_WIDTH, (tooltip, expanded) -> I18n.forGameText(() -> {
            tooltip.setTitleOrbitronVeryLarge();
            tooltip.addTitle(entry.type().displayName(), FrameworkSocketFlair.colorOf(entry.type()));
            VanillaText.addPara(tooltip, Translation.styled("framework.socketDescription." + entry.type().id()), PAD, Misc.getTextColor());
            if (entry.type().requirementTag() != null) {
                VanillaText.addPara(tooltip, HullUpgradeTooltip.socketLine(entry.type()), LINE_PAD, Misc.getGrayColor());
            }
            String hintKey = switch (entry.state()) {
                case UNLOCKED -> "framework.socketHint.unlocked";
                case AVAILABLE -> "framework.socketHint.available";
                case RESTRICTED -> "framework.socketHint.restricted";
                case NO_POINTS -> "framework.socketHint.noPoints";
            };
            VanillaText.addPara(tooltip, Translation.styled(hintKey), PAD, Misc.getGrayColor());
        }), tilePosition);
    }
}
