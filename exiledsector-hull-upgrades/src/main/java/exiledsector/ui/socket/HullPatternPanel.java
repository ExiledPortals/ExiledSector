package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.items.hull.ShipSection;
import exiledsector.items.pattern.HullPatternRules;
import exiledsector.items.pattern.HullPatternShips;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.VanillaText;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class HullPatternPanel extends HoloPanel {

    public interface Listener {

        void create();

        void deletePattern();

        void select(ShipSection section);

        void deselect(ShipSection section);

        void link(String shipId);

        void unlink(String shipId);

        void closed();
    }

    public enum State {NOT_IN_FLEET, NOT_PATTERNABLE, NO_PATTERN, PATTERN_HERE, FOLLOWER, PATTERN_ELSEWHERE}

    public enum SocketStatus {ITEM, EMPTY, INACTIVE, UNIQUE, LOCKED}

    public enum FollowerStatus {CLONED, OWN, NEEDS_UNLOCK, NOTHING}

    public record SocketEntry(ShipSection section, SocketStatus status, String itemName, Color itemColor, boolean selected,
                              HullPatternRules.SelectBlock block) {
    }

    public record ShipEntry(String shipId, String shipName, FleetMemberAPI member, boolean away, boolean linked, HullPatternRules.LinkBlock block) {
    }

    public record FollowerSocket(ShipSection section, FollowerStatus status, String itemName, Color itemColor) {
    }

    public record PatternShip(String shipName, HullPatternShips.Where where, String marketName) {
    }

    public record View(State state, String shipId, String shipName, String hullType, String hullName, PatternShip patternShip, int selectCost, int linkCost, int selectedCount,
                       int followerCount, List<SocketEntry> sockets, List<ShipEntry> ships, List<FollowerSocket> followerSockets) {
    }

    private static final float BUTTON_HEIGHT = 26f;
    private static final float ROW_HEIGHT = 24f;
    private static final float CONFIRM_HEIGHT = 170f;
    private static final float SCROLLBAR_ROOM = 14f;
    private static final float BLOCK_TOOLTIP_WIDTH = 320f;
    private static final int SHIP_COLUMNS = 2;
    private static final float SHIP_ROW_GAP = GAP;
    private static final float MIN_SHIP_CELL_HEIGHT = 110f;
    private static final float MAX_SHIP_CELL_ASPECT = 1.25f;
    private static final float SHIP_FIT_SLACK = 4f;
    private static final int MIN_SHIP_ROWS_SHOWN = 2;
    private static final Color AWAY_NAME_COLOR = new Color(130, 130, 130);

    private enum Control {CLOSE, CREATE, DELETE, UNLINK, CONFIRM, CANCEL}

    private record SocketToggle(SocketEntry entry) {
    }

    private final Listener panelListener;
    private final PatternShipPortraits shipPortraits;
    private View view;
    private Runnable pendingAction;
    private UIComponentAPI headerElement;
    private TooltipMakerAPI listElement;
    private TooltipMakerAPI shipGridElement;
    private float shipGridScroll;

    private HullPatternPanel(CustomPanelAPI hostPanel, View view, Listener panelListener, PatternShipPortraits shipPortraits) {
        super(hostPanel);
        this.view = view;
        this.panelListener = panelListener;
        this.shipPortraits = shipPortraits;
    }

    public static HullPatternPanel open(CustomPanelAPI hostPanel, SocketPanelPlacement placement, View view, Listener panelListener,
                                        PatternShipPortraits shipPortraits, Consumer<SocketPanelTab> tabListener, Set<SocketPanelTab> shownTabs) {
        HullPatternPanel panel = new HullPatternPanel(hostPanel, view, panelListener, shipPortraits);
        panel.setTabListener(tabListener);
        panel.setShownTabs(shownTabs);
        panel.attach(placement.panelLeft(), placement.panelTop(), placement.panelWidth(), placement.panelHeight(), placement.instantly());
        return panel;
    }

    public void update(View updatedView) {
        queue(() -> {
            view = updatedView;
            rebuildHeader();
            if (!isConfirmOpen()) {
                rebuildList();
            }
        });
    }

    @Override
    protected void build() {
        rebuildHeader();
        rebuildList();
    }

    @Override
    protected SocketPanelTab tab() {
        return SocketPanelTab.HULL_PATTERNS;
    }

    @Override
    protected float headerToolsWidth() {
        return FEEDSTOCK_ROOM;
    }

    @Override
    protected void rebuildHeader() {
        TooltipMakerAPI element = startHeader("ui.socketPanel.tab.hullPatterns", "ui.socketStorage.close", Control.CLOSE);
        addFeedstockCount(element, CLOSE_BUTTON_WIDTH + GAP);
        headerElement = finishHeader(element, headerElement);
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
    protected void onClose() {
        hoverTooltip.hide();
        panelListener.closed();
    }

    public void socketClicked(ShipSection section) {
        queue(() -> {
            if (isConfirmOpen()) {
                return;
            }
            SocketEntry entry = view.state() == State.PATTERN_HERE ? socketEntry(section) : null;
            if (entry == null || (!entry.selected() && entry.block() != null)) {
                SkillTreeSounds.refused();
                return;
            }
            socketPressed(entry);
        });
    }

    private SocketEntry socketEntry(ShipSection section) {
        for (SocketEntry entry : view.sockets()) {
            if (entry.section() == section) {
                return entry;
            }
        }
        return null;
    }

    public boolean escape() {
        if (!isConfirmOpen()) {
            return false;
        }
        queue(this::cancelConfirm);
        return true;
    }

    @Override
    public void buttonPressed(Object id) {
        if (tabPressed(id)) {
            return;
        }
        queue(() -> pressed(id));
    }

    private void pressed(Object id) {
        if (id == Control.CLOSE) {
            close();
        } else if (id == Control.CONFIRM) {
            Runnable action = pendingAction;
            pendingAction = null;
            closeConfirm();
            if (action != null) {
                action.run();
            }
            rebuildList();
        } else if (id == Control.CANCEL) {
            cancelConfirm();
        } else if (id == Control.CREATE) {
            panelListener.create();
        } else if (id == Control.DELETE) {
            confirm(Translation.msg("pattern.confirm.delete.title").arg("hull", view.hullName()).text(),
                    Translation.text("pattern.confirm.delete.body"), panelListener::deletePattern);
        } else if (id == Control.UNLINK) {
            confirm(Translation.msg("pattern.confirm.unlink.title").arg("ship", view.shipName()).text(),
                    Translation.msg("pattern.confirm.unlink.body").arg("cost", view.linkCost()).text(), () -> panelListener.unlink(view.shipId()));
        } else if (id instanceof SocketToggle toggle) {
            socketPressed(toggle.entry());
        }
    }

    private void cancelConfirm() {
        pendingAction = null;
        closeConfirm();
        rebuildList();
    }

    private void socketPressed(SocketEntry entry) {
        String sectionName = entry.section().displayName();
        if (entry.selected()) {
            confirm(Translation.msg("pattern.confirm.deselect.title").arg("section", sectionName).text(),
                    Translation.msg("pattern.confirm.deselect.body").arg("cost", view.selectCost()).text(),
                    () -> panelListener.deselect(entry.section()));
        } else if (entry.block() != null) {
            SkillTreeSounds.refused();
        } else if (view.selectCost() <= 0) {
            panelListener.select(entry.section());
            rebuildList();
        } else {
            confirm(Translation.msg("pattern.confirm.select.title").arg("section", sectionName).text(),
                    Translation.msg("pattern.confirm.select.body").count(view.followerCount()).arg("cost", view.selectCost())
                            .arg("per", HullPatternRules.FEEDSTOCK_PER_SOCKET).text(), () -> panelListener.select(entry.section()));
        }
    }

    private void shipPressed(ShipEntry entry) {
        if (entry.linked()) {
            confirm(Translation.msg("pattern.confirm.unlink.title").arg("ship", entry.shipName()).text(),
                    Translation.msg("pattern.confirm.unlink.body").arg("cost", view.linkCost()).text(), () -> panelListener.unlink(entry.shipId()));
        } else if (entry.block() != null) {
            SkillTreeSounds.refused();
        } else if (view.linkCost() <= 0) {
            panelListener.link(entry.shipId());
            rebuildList();
        } else {
            confirm(Translation.msg("pattern.confirm.link.title").arg("ship", entry.shipName()).text(),
                    Translation.msg("pattern.confirm.link.body").count(view.selectedCount()).arg("cost", view.linkCost())
                            .arg("per", HullPatternRules.FEEDSTOCK_PER_SOCKET).text(), () -> panelListener.link(entry.shipId()));
        }
    }

    private void confirm(String title, String body, Runnable action) {
        hoverTooltip.hide();
        pendingAction = action;
        openConfirm(CONFIRM_HEIGHT, element -> {
            VanillaText.plainPara(element, title, 0f, Misc.getHighlightColor());
            VanillaText.plainPara(element, body, LINE_PAD * 2f, Misc.getTextColor());
        }, "pattern.confirm.yes", Control.CONFIRM, "pattern.confirm.cancel", Control.CANCEL);
    }

    private void rebuildList() {
        float restoredScroll = scrollOffset(listElement);
        shipGridScroll = scrollOffset(shipGridElement);
        shipGridElement = null;
        hoverTooltip.hide();
        float listWidth = innerWidth();
        float listTop = PAD + HEADER_HEIGHT + GAP;
        float listHeight = panelHeight - listTop - PAD;
        TooltipMakerAPI element = panelRoot.createUIElement(listWidth, listHeight, true);
        float rowWidth = listWidth - SCROLLBAR_ROOM;
        switch (view.state()) {
            case NOT_IN_FLEET -> paragraph(element, Translation.text("pattern.state.notInFleet"), 0f, Misc.getTextColor());
            case NOT_PATTERNABLE -> paragraph(element, Translation.text("pattern.state.notPatternable"), 0f, Misc.getTextColor());
            case NO_PATTERN -> writeNoPattern(element, rowWidth);
            case PATTERN_HERE -> writePatternHere(element, rowWidth, listHeight);
            case FOLLOWER -> writeFollower(element, rowWidth);
            case PATTERN_ELSEWHERE -> writeElsewhere(element, rowWidth);
        }
        listElement = replaceElement(placedComponent(listElement), element, PAD, listTop);
        if (restoredScroll > 0f && element.getExternalScroller() != null) {
            element.getExternalScroller().setYOffset(Math.min(restoredScroll, Math.max(0f, element.getHeightSoFar() - listHeight)));
        }
        refreshOpacity();
    }

    private void writeNoPattern(TooltipMakerAPI element, float rowWidth) {
        paragraph(element, Translation.msg("pattern.state.noPattern").arg("hull", view.hullName()).text(), 0f, Misc.getTextColor());
        element.addButton(Translation.msg("pattern.button.create").arg("hull", view.hullName()).text(), Control.CREATE, rowWidth, BUTTON_HEIGHT,
                GAP * 3f);
    }

    private void writePatternHere(TooltipMakerAPI element, float rowWidth, float listHeight) {
        paragraph(element, Translation.msg("pattern.state.here").arg("hull", view.hullName()).text(), 0f, Misc.getTextColor());
        paragraph(element, Translation.msg("pattern.costs").arg("cost", HullPatternRules.FEEDSTOCK_PER_SOCKET).arg("select", view.selectCost())
                .arg("link", view.linkCost()).text(), GAP * 2f, Misc.getGrayColor());
        paragraph(element, Translation.text("pattern.lossWarning"), GAP * 2f, Misc.getNegativeHighlightColor());
        element.addSectionHeading(Translation.text("pattern.heading.sockets"), Alignment.MID, GAP * 3f);
        paragraph(element, Translation.msg("pattern.sockets.hint").arg("selected", view.selectedCount()).arg("count", HullPatternRules.MAX_SOCKETS)
                .text(), GAP, Misc.getGrayColor());
        for (SocketEntry entry : view.sockets()) {
            if (entry.selected() && entry.status() == SocketStatus.LOCKED) {
                String line = Translation.msg("pattern.socket.line").arg("section", entry.section().displayName()).arg("item", socketStatusText(entry))
                        .text();
                checkedRow(element, line, new SocketToggle(entry), rowWidth);
            }
        }
        element.addSectionHeading(Translation.text("pattern.heading.ships"), Alignment.MID, GAP * 3f);
        if (view.ships().isEmpty()) {
            paragraph(element, Translation.msg("pattern.ships.none").arg("hull", view.hullName()).text(), GAP, Misc.getGrayColor());
        } else {
            paragraph(element, Translation.text("pattern.ships.hint"), GAP, Misc.getGrayColor());
            addShipGrid(element, rowWidth, listHeight);
        }
        element.addButton(Translation.text("pattern.button.delete"), Control.DELETE, rowWidth, BUTTON_HEIGHT, GAP * 4f);
    }

    private void addShipGrid(TooltipMakerAPI element, float rowWidth, float listHeight) {
        List<ShipEntry> ships = view.ships();
        Set<String> shownShipIds = new HashSet<>();
        ships.forEach(entry -> shownShipIds.add(entry.shipId()));
        shipPortraits.retain(shownShipIds);
        int rows = (ships.size() + SHIP_COLUMNS - 1) / SHIP_COLUMNS;
        float gridHeight = shipGridHeight(listHeight - element.getHeightSoFar() - BUTTON_HEIGHT - GAP * 5f);
        float cellHeight = shipCellHeight(columnWidth(rowWidth), gridHeight, rows);
        boolean scrolls = rowsHeight(cellHeight, rows) > gridHeight;
        float cellsWidth = scrolls ? rowWidth - SCROLLBAR_ROOM : rowWidth;
        float columnWidth = columnWidth(cellsWidth);
        CustomPanelAPI gridPanel = Global.getSettings().createCustom(rowWidth, gridHeight, null);
        TooltipMakerAPI gridElement = gridPanel.createUIElement(rowWidth, gridHeight, true);
        for (int start = 0; start < ships.size(); start += SHIP_COLUMNS) {
            CustomPanelAPI rowPanel = Global.getSettings().createCustom(cellsWidth, cellHeight, null);
            int rowCount = Math.min(ships.size(), start + SHIP_COLUMNS) - start;
            for (int i = 0; i < rowCount; i++) {
                rowPanel.addComponent(shipCell(ships.get(start + i), columnWidth, cellHeight)).inTL(i * (columnWidth + GAP), 0f);
            }
            gridElement.addCustom(rowPanel, start == 0 ? 0f : SHIP_ROW_GAP);
        }
        gridPanel.addUIElement(gridElement).inTL(0f, 0f);
        shipGridElement = gridElement;
        if (shipGridScroll > 0f && gridElement.getExternalScroller() != null) {
            gridElement.getExternalScroller().setYOffset(Math.min(shipGridScroll, Math.max(0f, gridElement.getHeightSoFar() - gridHeight)));
        }
        element.addCustom(gridPanel, GAP);
    }

    private static float columnWidth(float cellsWidth) {
        return (cellsWidth - GAP * (SHIP_COLUMNS - 1)) / SHIP_COLUMNS;
    }

    static float shipGridHeight(float roomBelow) {
        return Math.max(rowsHeight(MIN_SHIP_CELL_HEIGHT, MIN_SHIP_ROWS_SHOWN), (float) Math.floor(roomBelow));
    }

    static float rowsHeight(float cellHeight, int rows) {
        return rows <= 0 ? 0f : rows * cellHeight + (rows - 1) * SHIP_ROW_GAP;
    }

    static float shipCellHeight(float columnWidth, float gridHeight, int rows) {
        float maxHeight = (float) Math.floor(columnWidth * MAX_SHIP_CELL_ASPECT);
        if (rows <= 0) {
            return maxHeight;
        }
        float roomPerCell = (float) Math.floor((gridHeight - SHIP_FIT_SLACK - (rows - 1) * SHIP_ROW_GAP) / rows);
        return Math.max(MIN_SHIP_CELL_HEIGHT, Math.min(maxHeight, roomPerCell));
    }

    private static float scrollOffset(TooltipMakerAPI scrolledElement) {
        return scrolledElement == null || scrolledElement.getExternalScroller() == null ? 0f : scrolledElement.getExternalScroller().getYOffset();
    }

    private CustomPanelAPI shipCell(ShipEntry entry, float cellWidth, float cellHeight) {
        boolean blocked = !entry.linked() && entry.block() != null;
        IconTileCell.Hooks hooks = new IconTileCell.Hooks(() -> queue(() -> shipPressed(entry)),
                cellPosition -> queue(() -> showShipTooltip(entry, cellPosition)), () -> queue(() -> hideTooltipOf(entry)));
        PatternShipCell cell = new PatternShipCell(shipPortraits.of(entry.shipId(), entry.member(), view.hullType()), entry.linked(), blocked,
                this::visibleShipAreas, hooks);
        CustomPanelAPI cellPanel = Global.getSettings().createCustom(cellWidth, cellHeight, cell);
        TooltipMakerAPI nameElement = cellPanel.createUIElement(cellWidth, PatternShipCell.NAME_HEIGHT, false);
        String name = entry.away() ? Translation.msg("pattern.ship.away").arg("ship", entry.shipName()).text() : entry.shipName();
        Color nameColor = entry.linked() ? Misc.getTextColor() : Misc.getGrayColor();
        if (entry.away()) {
            nameColor = AWAY_NAME_COLOR;
        }
        LabelAPI label = VanillaText.plainPara(nameElement, name, 0f, nameColor);
        label.setAlignment(Alignment.MID);
        label.getPosition().setSize(cellWidth - GAP * 2f, PatternShipCell.NAME_HEIGHT - LINE_PAD).inTL(GAP, 0f);
        cellPanel.addUIElement(nameElement).inTL(0f, cellHeight - PatternShipCell.NAME_HEIGHT);
        return cellPanel;
    }

    private List<PositionAPI> visibleShipAreas() {
        List<PositionAPI> areas = new ArrayList<>(2);
        for (UIComponentAPI placed : new UIComponentAPI[]{placedComponent(listElement), placedComponent(shipGridElement)}) {
            if (placed != null) {
                areas.add(placed.getPosition());
            }
        }
        return areas;
    }

    private void showShipTooltip(ShipEntry entry, PositionAPI cellPosition) {
        if (isConfirmOpen()) {
            return;
        }
        String text;
        if (entry.linked()) {
            text = Translation.text("pattern.ship.hintUnlink");
        } else if (entry.block() != null) {
            text = linkBlockText(entry.block());
        } else {
            text = Translation.text("pattern.ship.hintLink");
        }
        if (text == null) {
            return;
        }
        hoverTooltip.show(entry, BLOCK_TOOLTIP_WIDTH, (tooltip, expanded) -> VanillaText.plainPara(tooltip, text, 0f, Misc.getTextColor()),
                cellPosition);
    }

    private void hideTooltipOf(Object tooltipKey) {
        if (hoverTooltip.isShowing(tooltipKey)) {
            hoverTooltip.hide();
        }
    }

    private void writeFollower(TooltipMakerAPI element, float rowWidth) {
        PatternShip patternShip = view.patternShip();
        paragraph(element, Translation.msg("pattern.state.follower").arg("hull", view.hullName()).arg("ship", patternShip.shipName())
                .arg("location", locationText(patternShip)).text(), 0f, Misc.getTextColor());
        if (patternShip.where() != HullPatternShips.Where.FLEET) {
            paragraph(element, Translation.text("pattern.state.followerAway"), GAP * 2f, Misc.getNegativeHighlightColor());
        }
        element.addSectionHeading(Translation.text("pattern.heading.sockets"), Alignment.MID, GAP * 3f);
        for (FollowerSocket socket : view.followerSockets()) {
            String status = switch (socket.status()) {
                case CLONED -> Translation.msg("pattern.follower.cloned").arg("item", socket.itemName()).text();
                case OWN -> Translation.text("pattern.follower.own");
                case NEEDS_UNLOCK -> Translation.text("pattern.follower.locked");
                case NOTHING -> Translation.text("pattern.follower.nothing");
            };
            Color color = socket.status() == FollowerStatus.CLONED && socket.itemColor() != null ? socket.itemColor() : Misc.getGrayColor();
            paragraph(element, Translation.msg("pattern.follower.line").arg("section", socket.section().displayName()).arg("status", status).text(),
                    GAP, color);
        }
        element.addButton(Translation.text("pattern.button.unlink"), Control.UNLINK, rowWidth, BUTTON_HEIGHT, GAP * 4f);
    }

    private void writeElsewhere(TooltipMakerAPI element, float rowWidth) {
        PatternShip patternShip = view.patternShip();
        paragraph(element, Translation.msg("pattern.state.elsewhere").arg("hull", view.hullName()).arg("ship", patternShip.shipName())
                .arg("location", locationText(patternShip)).text(), 0f, Misc.getTextColor());
        element.addButton(Translation.text("pattern.button.deleteOther"), Control.DELETE, rowWidth, BUTTON_HEIGHT, GAP * 3f);
    }

    private static void checkedRow(TooltipMakerAPI element, String text, Object data, float rowWidth) {
        ButtonAPI checkbox = element.addAreaCheckbox(text, data, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
                rowWidth, ROW_HEIGHT, GAP);
        checkbox.setChecked(true);
    }

    private static void paragraph(TooltipMakerAPI element, String text, float pad, Color color) {
        VanillaText.plainPara(element, text, pad, color);
    }

    private static String socketStatusText(SocketEntry entry) {
        return switch (entry.status()) {
            case ITEM -> entry.itemName();
            case EMPTY -> Translation.text("pattern.socket.empty");
            case INACTIVE -> Translation.text("pattern.socket.requirement");
            case UNIQUE -> Translation.text("pattern.socket.unique");
            case LOCKED -> Translation.text("pattern.socket.locked");
        };
    }

    public static String selectBlockText(HullPatternRules.SelectBlock block, int selectCost) {
        if (block == null) {
            return null;
        }
        return switch (block) {
            case SECTION_LOCKED -> Translation.text("pattern.block.locked");
            case REQUIREMENT_UNMET -> Translation.text("pattern.block.requirement");
            case HOLDS_UNIQUE -> Translation.text("pattern.block.unique");
            case AT_LIMIT -> Translation.msg("pattern.limit").arg("count", HullPatternRules.MAX_SOCKETS).text();
            case CANNOT_AFFORD -> Translation.msg("pattern.block.cannotAfford").arg("cost", selectCost).text();
        };
    }

    private String linkBlockText(HullPatternRules.LinkBlock block) {
        if (block == null) {
            return null;
        }
        return switch (block) {
            case NOT_IN_FLEET -> Translation.text("pattern.block.away");
            case NOT_PATTERNABLE -> Translation.text("pattern.state.notPatternable");
            case OTHER_HULL_TYPE, PATTERN_SHIP -> null;
            case CANNOT_AFFORD -> Translation.msg("pattern.block.cannotAfford").arg("cost", view.linkCost()).text();
        };
    }

    private static String locationText(PatternShip patternShip) {
        return switch (patternShip.where()) {
            case FLEET -> Translation.text("pattern.location.fleet");
            case STORAGE -> Translation.msg("pattern.location.storage").arg("market", String.valueOf(patternShip.marketName())).text();
            case AWAY -> Translation.text("pattern.location.away");
        };
    }
}
