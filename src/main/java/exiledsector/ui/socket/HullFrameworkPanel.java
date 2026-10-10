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
import exiledsector.socketables.FrameworkSockets;
import exiledsector.socketables.HullUpgradeData;
import exiledsector.socketables.HullUpgradeTooltip;
import exiledsector.socketables.SocketType;
import exiledsector.socketables.SocketableRarity;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.VanillaText;
import exiledsector.ui.framework.FrameworkSocketFlair;
import exiledsector.ui.util.FramedPanelPlugin;

import java.awt.Color;
import java.util.List;

public final class HullFrameworkPanel extends HoloPanel {

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
    private static final float UPGRADE_TILE_SCALE = 1.3f;
    private static final float MIN_TILE_SIZE = 64f;
    private static final float FIT_SLACK = 12f;
    private static final float SCROLLBAR_ROOM = 14f;
    private static final float NAME_HEIGHT = 22f;
    private static final float STATE_HEIGHT = 20f;
    private static final float TOOLTIP_WIDTH = 520f;
    private static final Color BLOCKED_TEXT_COLOR = new Color(130, 130, 130);
    private static final float CONFIRM_HEIGHT = 170f;
    private static final float CONFIRM_BUTTON_HEIGHT = 26f;
    private static final float POINTS_LINE_HEIGHT = 30f;

    private enum Control {CLOSE, CONFIRM_INSTALL, CANCEL_INSTALL}

    private record TileSizes(int socketColumns, float socketTile, float upgradeTile) {
    }

    private final Listener panelListener;
    private final SocketableHoverTooltip hoverTooltip;
    private View view;
    private UIComponentAPI headerElement;
    private TooltipMakerAPI listElement;
    private CustomPanelAPI confirmPanel;
    private CustomPanelAPI confirmBlocker;

    private HullFrameworkPanel(CustomPanelAPI hostPanel, View view, Listener panelListener) {
        super(hostPanel, HullFrameworkPanel.class);
        this.view = view;
        this.panelListener = panelListener;
        this.hoverTooltip = new SocketableHoverTooltip(hostPanel);
    }

    public static HullFrameworkPanel open(CustomPanelAPI hostPanel, float panelLeft, float panelTop, float panelHeight, View view,
                                          Listener panelListener) {
        HullFrameworkPanel panel = new HullFrameworkPanel(hostPanel, view, panelListener);
        panel.attach(panelLeft, panelTop, WIDTH, panelHeight);
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
        TooltipMakerAPI element = startHeader("framework.panel.title", "ui.socketStorage.close", Control.CLOSE);
        headerElement = finishHeader(element, headerElement, CLOSE_BUTTON_WIDTH + GAP);
        rebuildList();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{listElement == null ? null
                : listElement.getExternalScroller() != null ? listElement.getExternalScroller() : listElement,
                confirmPanel, confirmBlocker};
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
        if (confirmPanel == null) {
            return false;
        }
        queue(this::closeConfirm);
        return true;
    }

    private void openConfirm() {
        if (confirmPanel != null) {
            return;
        }
        hoverTooltip.hide();
        HullUpgradeData upgrade = new HullUpgradeData(view.hullSize());
        confirmBlocker = Global.getSettings().createCustom(panelWidth, panelHeight, new InputBlocker());
        panelRoot.addComponent(confirmBlocker).inTL(0f, 0f);
        float confirmWidth = panelWidth - PAD * 2f;
        confirmPanel = Global.getSettings().createCustom(confirmWidth, CONFIRM_HEIGHT,
                new FramedPanelPlugin(HullFrameworkPanel.class, this::buttonPressed));
        TooltipMakerAPI element = confirmPanel.createUIElement(confirmWidth - PAD * 2f, CONFIRM_HEIGHT - PAD * 2f, false);
        Color uniqueColor = SocketableRarity.UNIQUE.color();
        element.addPara("%s", 0f, uniqueColor, uniqueColor, Translation.msg("framework.confirm.title").arg("name", upgrade.name()).text());
        element.addPara("%s", LINE_PAD * 2f, Misc.getTextColor(), Misc.getTextColor(), Translation.msg("framework.confirm.body")
                .arg("left", view.upgradesInStorage() - 1).arg("points", view.points() + 1).arg("max", FrameworkSockets.MAX_POINTS).text());
        float buttonWidth = (confirmWidth - PAD * 2f - GAP) / 2f;
        element.addButton(Translation.text("framework.confirm.install"), Control.CONFIRM_INSTALL, buttonWidth, CONFIRM_BUTTON_HEIGHT, 0f)
                .getPosition().inBR(0f, 0f);
        element.addButton(Translation.text("framework.confirm.cancel"), Control.CANCEL_INSTALL, buttonWidth, CONFIRM_BUTTON_HEIGHT, 0f)
                .getPosition().inBL(0f, 0f);
        confirmPanel.addUIElement(element).inTL(PAD, PAD);
        panelRoot.addComponent(confirmPanel).inTL(PAD, (panelHeight - CONFIRM_HEIGHT) / 2f);
        refreshOpacity();
    }

    private void closeConfirm() {
        if (confirmPanel == null) {
            return;
        }
        panelRoot.removeComponent(confirmPanel);
        panelRoot.removeComponent(confirmBlocker);
        confirmPanel = null;
        confirmBlocker = null;
    }

    private void rebuildList() {
        hoverTooltip.hide();
        closeConfirm();
        if (listElement != null) {
            panelRoot.removeComponent(listElement.getExternalScroller() != null ? listElement.getExternalScroller() : listElement);
        }
        float listWidth = panelWidth - PAD * 2f;
        float listTop = PAD + HEADER_HEIGHT + GAP;
        TooltipMakerAPI element = panelRoot.createUIElement(listWidth, panelHeight - listTop - PAD, true);
        element.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(),
                Translation.msg("framework.panel.hint").arg("max", FrameworkSockets.MAX_POINTS).text());
        float gridWidth = listWidth - SCROLLBAR_ROOM;
        float beforeHeading = element.getHeightSoFar();
        element.addSectionHeading(Translation.text("framework.panel.upgrades"), Alignment.MID, GAP * 3f);
        float headingHeight = element.getHeightSoFar() - beforeHeading;
        float labelsHeight = NAME_HEIGHT + STATE_HEIGHT;
        float fixedHeight = element.getHeightSoFar() + GAP * 2f + labelsHeight
                + GAP * 3f + POINTS_LINE_HEIGHT + headingHeight + FIT_SLACK;
        TileSizes tileSizes = tileSizes(gridWidth, panelHeight - listTop - PAD - fixedHeight, view.sockets().size());
        float tileWidth = tileSizes.socketTile();
        int socketColumns = tileSizes.socketColumns();
        element.addCustom(upgradeRow(gridWidth, tileSizes.upgradeTile()), GAP * 2f);
        addPointsLine(element, (view.points() - view.unspentPoints()) + "/" + view.points(), gridWidth);
        element.addSectionHeading(Translation.text("framework.panel.sockets"), Alignment.MID, GAP * 3f);
        List<SocketEntry> sockets = view.sockets();
        for (int start = 0; start < sockets.size(); start += socketColumns) {
            CustomPanelAPI rowPanel = Global.getSettings().createCustom(gridWidth, tileWidth + NAME_HEIGHT + STATE_HEIGHT, null);
            for (int i = start; i < Math.min(sockets.size(), start + socketColumns); i++) {
                rowPanel.addComponent(socketCell(sockets.get(i), tileWidth)).inTL((i - start) * (tileWidth + GAP), 0f);
            }
            element.addCustom(rowPanel, GAP * 2f);
        }
        panelRoot.addUIElement(element).inTL(PAD, listTop);
        listElement = element;
        refreshOpacity();
    }

    private static TileSizes tileSizes(float gridWidth, float tileRoom, int socketCount) {
        TileSizes best = null;
        for (int socketColumns = MIN_SOCKET_COLUMNS; socketColumns <= MAX_SOCKET_COLUMNS; socketColumns++) {
            int rows = (socketCount + socketColumns - 1) / socketColumns;
            float widthLimit = (gridWidth - GAP * (socketColumns - 1)) / socketColumns;
            float rowLabels = rows * (NAME_HEIGHT + STATE_HEIGHT + GAP * 2f);
            float heightLimit = (tileRoom - rowLabels) / (UPGRADE_TILE_SCALE + rows);
            float socketTile = Math.max(MIN_TILE_SIZE, Math.min(widthLimit, heightLimit));
            if (best == null || socketTile > best.socketTile()) {
                best = new TileSizes(socketColumns, socketTile, Math.min(gridWidth, socketTile * UPGRADE_TILE_SCALE));
            }
        }
        return best;
    }

    private CustomPanelAPI upgradeRow(float gridWidth, float tileWidth) {
        HullUpgradeData upgrade = new HullUpgradeData(view.hullSize());
        boolean blocked = view.upgradeBlock() != null;
        CustomPanelAPI rowPanel = Global.getSettings().createCustom(gridWidth, tileWidth + NAME_HEIGHT + STATE_HEIGHT, null);
        CustomPanelAPI cellPanel = Global.getSettings().createCustom(tileWidth, tileWidth + NAME_HEIGHT + STATE_HEIGHT, null);
        TooltipMakerAPI cellElement = cellPanel.createUIElement(tileWidth, tileWidth + NAME_HEIGHT + STATE_HEIGHT, false);
        IconTileCell.Hooks hooks = new IconTileCell.Hooks(() -> queue(this::openConfirm),
                tilePosition -> queue(() -> showUpgradeTooltip(upgrade, tilePosition)), () -> queue(hoverTooltip::hide));
        cellElement.addCustom(Global.getSettings().createCustom(tileWidth, tileWidth,
                new IconTileCell(upgrade.iconPath(), SocketableRarity.UNIQUE.color(), blocked, hooks)), 0f);
        addCentredLabel(cellElement, upgrade.name(), blocked ? BLOCKED_TEXT_COLOR : SocketableRarity.UNIQUE.color(), tileWidth);
        addCentredLabel(cellElement, Translation.msg("framework.panel.inStorage").arg("count", view.upgradesInStorage()).text(),
                Misc.getGrayColor(), tileWidth);
        cellPanel.addUIElement(cellElement).inTL(0f, 0f);
        rowPanel.addComponent(cellPanel).inTL((gridWidth - tileWidth) / 2f, 0f);
        return rowPanel;
    }

    private static void addPointsLine(TooltipMakerAPI element, String points, float width) {
        element.setParaFont(Fonts.ORBITRON_24AABOLD);
        LabelAPI pointsLabel = element.addPara("%s", GAP * 3f, SkillTreePanelStyle.OP_TEXT_COLOR,
                SkillTreePanelStyle.OP_TEXT_COLOR, points);
        pointsLabel.setAlignment(Alignment.MID);
        pointsLabel.getPosition().setSize(width, POINTS_LINE_HEIGHT);
        element.setParaFontDefault();
    }

    private String upgradeStateText() {
        FrameworkSockets.UpgradeBlock block = view.upgradeBlock();
        if (block == null) {
            return Translation.text("framework.panel.upgradeReady");
        }
        return switch (block) {
            case AT_MAX_POINTS -> Translation.msg("framework.panel.upgradeAtMax").arg("max", FrameworkSockets.MAX_POINTS).text();
            case NONE_IN_STORAGE -> Translation.msg("framework.panel.upgradeNone")
                    .arg("size", Translation.text("hullSize." + view.hullSize().name())).text();
        };
    }

    private CustomPanelAPI socketCell(SocketEntry entry, float tileWidth) {
        CustomPanelAPI cellPanel = Global.getSettings().createCustom(tileWidth, tileWidth + NAME_HEIGHT + STATE_HEIGHT, null);
        TooltipMakerAPI cellElement = cellPanel.createUIElement(tileWidth, tileWidth + NAME_HEIGHT + STATE_HEIGHT, false);
        IconTileCell.Hooks hooks = new IconTileCell.Hooks(() -> queue(() -> panelListener.toggleSocket(entry.type())),
                tilePosition -> queue(() -> showSocketTooltip(entry, tilePosition)), () -> queue(hoverTooltip::hide));
        cellElement.addCustom(Global.getSettings().createCustom(tileWidth, tileWidth, new SocketTypeTile(entry.type(), entry.state(),
                entry.itemIconPath(), hooks)), 0f);
        boolean blocked = entry.state() != SocketState.UNLOCKED;
        addCentredLabel(cellElement, entry.type().displayName(), blocked ? BLOCKED_TEXT_COLOR : FrameworkSocketFlair.colorOf(entry.type()), tileWidth);
        addCentredLabel(cellElement, stateText(entry), stateColor(entry.state()), tileWidth);
        cellPanel.addUIElement(cellElement).inTL(0f, 0f);
        return cellPanel;
    }

    private static String stateText(SocketEntry entry) {
        return switch (entry.state()) {
            case UNLOCKED -> Translation.text("framework.socketState.unlocked");
            case AVAILABLE -> Translation.text("framework.socketState.available");
            case RESTRICTED -> Translation.msg("framework.socketState.restricted").arg("requirement", entry.requirementText()).text();
            case NO_POINTS -> Translation.text("framework.socketState.noPoints");
        };
    }

    private static Color stateColor(SocketState state) {
        return switch (state) {
            case UNLOCKED -> Misc.getHighlightColor();
            case AVAILABLE -> Misc.getPositiveHighlightColor();
            case RESTRICTED -> Misc.getNegativeHighlightColor();
            case NO_POINTS -> Misc.getGrayColor();
        };
    }

    private static void addCentredLabel(TooltipMakerAPI element, String text, Color color, float width) {
        LabelAPI label = element.addPara("%s", LINE_PAD, color, color, text);
        label.setAlignment(Alignment.MID);
        label.getPosition().setSize(width, NAME_HEIGHT - LINE_PAD);
    }

    private void showUpgradeTooltip(HullUpgradeData upgrade, PositionAPI tilePosition) {
        List<StyledText> footer = List.of(StyledText.of(upgradeStateText()));
        hoverTooltip.show(upgrade, TOOLTIP_WIDTH, (tooltip, expanded) -> HullUpgradeTooltip.write(tooltip, upgrade, () -> footer), tilePosition);
    }

    private void showSocketTooltip(SocketEntry entry, PositionAPI tilePosition) {
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
