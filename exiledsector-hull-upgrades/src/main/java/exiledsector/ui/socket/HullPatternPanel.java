package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.items.hull.ShipSection;
import exiledsector.items.pattern.HullPatternRules;
import exiledsector.items.pattern.HullPatternShips;
import exiledsector.ui.VanillaText;

import java.awt.Color;
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

    public record ShipEntry(String shipId, String shipName, boolean away, boolean linked, HullPatternRules.LinkBlock block) {
    }

    public record FollowerSocket(ShipSection section, FollowerStatus status, String itemName, Color itemColor) {
    }

    public record PatternShip(String shipName, HullPatternShips.Where where, String marketName) {
    }

    public record View(State state, String shipId, String shipName, String hullName, PatternShip patternShip, int selectCost, int linkCost, int selectedCount,
                       int followerCount, List<SocketEntry> sockets, List<ShipEntry> ships, List<FollowerSocket> followerSockets) {
    }

    private static final float BUTTON_HEIGHT = 26f;
    private static final float ROW_HEIGHT = 24f;
    private static final float CONFIRM_HEIGHT = 170f;
    private static final float SCROLLBAR_ROOM = 14f;
    private static final float BLOCK_TOOLTIP_WIDTH = 320f;

    private enum Control {CLOSE, CREATE, DELETE, UNLINK, CONFIRM, CANCEL}

    private record SocketToggle(SocketEntry entry) {
    }

    private record ShipToggle(ShipEntry entry) {
    }

    private final Listener panelListener;
    private View view;
    private Runnable pendingAction;
    private UIComponentAPI headerElement;
    private TooltipMakerAPI listElement;

    private HullPatternPanel(CustomPanelAPI hostPanel, View view, Listener panelListener) {
        super(hostPanel);
        this.view = view;
        this.panelListener = panelListener;
    }

    public static HullPatternPanel open(CustomPanelAPI hostPanel, SocketPanelPlacement placement, View view, Listener panelListener,
                                        Consumer<SocketPanelTab> tabListener, Set<SocketPanelTab> shownTabs) {
        HullPatternPanel panel = new HullPatternPanel(hostPanel, view, panelListener);
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
        } else if (id instanceof ShipToggle toggle) {
            shipPressed(toggle.entry());
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
            rebuildList();
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
            rebuildList();
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
        float restoredScroll = listElement == null || listElement.getExternalScroller() == null ? 0f : listElement.getExternalScroller().getYOffset();
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
            case PATTERN_HERE -> writePatternHere(element, rowWidth);
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

    private void writePatternHere(TooltipMakerAPI element, float rowWidth) {
        paragraph(element, Translation.msg("pattern.state.here").arg("hull", view.hullName()).text(), 0f, Misc.getTextColor());
        paragraph(element, Translation.msg("pattern.costs").arg("cost", HullPatternRules.FEEDSTOCK_PER_SOCKET).arg("select", view.selectCost())
                .arg("link", view.linkCost()).text(), GAP * 2f, Misc.getGrayColor());
        paragraph(element, Translation.text("pattern.lossWarning"), GAP * 2f, Misc.getNegativeHighlightColor());
        element.addSectionHeading(Translation.text("pattern.heading.sockets"), Alignment.MID, GAP * 3f);
        paragraph(element, Translation.msg("pattern.limit").arg("count", HullPatternRules.MAX_SOCKETS).text(), GAP, Misc.getGrayColor());
        for (SocketEntry entry : view.sockets()) {
            String line = Translation.msg("pattern.socket.line").arg("section", entry.section().displayName()).arg("item", socketStatusText(entry)).text();
            checkboxRow(element, line, new SocketToggle(entry), entry.selected(), entry.block() == null || entry.selected(), rowWidth,
                    entry.selected() ? null : selectBlockText(entry.block()));
        }
        element.addSectionHeading(Translation.text("pattern.heading.ships"), Alignment.MID, GAP * 3f);
        if (view.ships().isEmpty()) {
            paragraph(element, Translation.msg("pattern.ships.none").arg("hull", view.hullName()).text(), GAP, Misc.getGrayColor());
        }
        for (ShipEntry entry : view.ships()) {
            String line = entry.away() ? Translation.msg("pattern.ship.away").arg("ship", entry.shipName()).text() : entry.shipName();
            checkboxRow(element, line, new ShipToggle(entry), entry.linked(), entry.block() == null || entry.linked(), rowWidth,
                    entry.linked() ? null : linkBlockText(entry.block()));
        }
        element.addButton(Translation.text("pattern.button.delete"), Control.DELETE, rowWidth, BUTTON_HEIGHT, GAP * 4f);
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

    private void checkboxRow(TooltipMakerAPI element, String text, Object data, boolean checked, boolean enabled, float rowWidth, String blockText) {
        ButtonAPI checkbox = element.addAreaCheckbox(text, data, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
                rowWidth, ROW_HEIGHT, GAP);
        checkbox.setChecked(checked);
        checkbox.setEnabled(enabled);
        if (blockText != null) {
            element.addTooltipToPrevious(new GameTextTooltip(BLOCK_TOOLTIP_WIDTH,
                    tooltip -> VanillaText.plainPara(tooltip, blockText, 0f, Misc.getTextColor())), TooltipMakerAPI.TooltipLocation.BELOW);
        }
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

    private String selectBlockText(HullPatternRules.SelectBlock block) {
        if (block == null) {
            return null;
        }
        return switch (block) {
            case SECTION_LOCKED -> Translation.text("pattern.block.locked");
            case REQUIREMENT_UNMET -> Translation.text("pattern.block.requirement");
            case HOLDS_UNIQUE -> Translation.text("pattern.block.unique");
            case AT_LIMIT -> Translation.msg("pattern.limit").arg("count", HullPatternRules.MAX_SOCKETS).text();
            case CANNOT_AFFORD -> Translation.msg("pattern.block.cannotAfford").arg("cost", view.selectCost()).text();
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
