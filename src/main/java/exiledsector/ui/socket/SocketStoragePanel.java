package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.socketables.SocketCustody;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableDisassembly;
import exiledsector.socketables.SocketableRarity;
import exiledsector.socketables.SocketableStore;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.FramedPanelPlugin;
import exiledsector.ui.util.HoloTransition;
import exiledsector.ui.util.Rects;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

public final class SocketStoragePanel extends BaseCustomUIPanelPlugin {

    public interface Listener {

        void selected(Socketable socketable);

        void closed();
    }

    private static final float PAD = 12f;
    private static final float LINE_PAD = 3f;
    private static final float GAP = 6f;
    private static final float HEADER_HEIGHT = 28f;
    private static final float FIELD_HEIGHT = 26f;
    private static final float CHIP_HEIGHT = 24f;
    private static final float CHIP_TEXT_PADDING = 28f;
    private static final float CHIP_DARK_SCALE = 0.3f;
    private static final int CONTROL_ROWS = 2;
    private static final float LABEL_WIDTH = 70f;
    private static final float CLOSE_BUTTON_WIDTH = 70f;
    private static final float NOTICE_HEIGHT = 56f;
    private static final float FOOTER_HEIGHT = 24f;
    private static final float SCROLLBAR_ROOM = 14f;
    private static final float CELL_SIZE = 96f;
    private static final int COLUMNS = 6;
    private static final float WIDTH = PAD * 2f + SCROLLBAR_ROOM + COLUMNS * CELL_SIZE + (COLUMNS - 1) * GAP;
    private static final float CONFIRM_HEIGHT = 130f;
    private static final float WORKBENCH_GAP = 12f;
    private static final float BATCH_CONFIRM_HEIGHT = 170f;
    private static final float BUTTON_TEXT_PADDING = 30f;
    private static final float PARTS_ICON_SIZE = 24f;
    private static final float PARTS_TEXT_TOP = 5f;
    private static final float SEARCH_DELAY_SECONDS = 0.25f;
    private static final SocketableCell.Look CELL_LOOK = new SocketableCell.Look(9f, true, 1f);

    private enum Control {CLOSE, WORKBENCH, DISASSEMBLY_MODE, CONFIRM_DISASSEMBLE, CANCEL_DISASSEMBLE}

    private record StatusChip(SocketStorageFilter.Status status) {
    }

    private record RarityChip(SocketableRarity rarity) {
    }

    private record ChipColors(Color base, Color dark, Color bright) {

        static ChipColors of(Color base) {
            return new ChipColors(base, Misc.scaleColorOnly(base, CHIP_DARK_SCALE), base.brighter());
        }

        static ChipColors player() {
            return new ChipColors(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor());
        }
    }

    private final CustomPanelAPI hostPanel;
    private final Listener storageListener;
    private final SocketStorageFilter storageFilter = SocketStorageFilter.SESSION;
    private final BorderedPanel panelFrame = new BorderedPanel(SocketStoragePanel.class);
    private final HoloTransition holoTransition = new HoloTransition();
    private boolean closing;

    private Function<Socketable, String> installedShipLookup;
    private CustomPanelAPI panelRoot;
    private SocketWorkbenchPanel workbenchPanel;
    private SocketWorkbenchPanel fadingWorkbenchPanel;
    private float panelLeft;
    private float panelTop;
    private PositionAPI panelPosition;
    private float panelWidth;
    private float panelHeight;
    private float gridTop;
    private UIComponentAPI headerElement;
    private UIComponentAPI searchRowElement;
    private UIComponentAPI controlsElement;
    private TooltipMakerAPI gridElement;
    private UIComponentAPI noticeElement;
    private UIComponentAPI summaryElement;
    private CustomPanelAPI confirmPanel;
    private CustomPanelAPI confirmBlocker;
    private TextFieldAPI searchField;
    private String typedQuery;
    private float searchDelay;
    private List<SocketStorageRow> storageRows = List.of();
    private Socketable selectedSocketable;
    private boolean targetingSocket;
    private List<Socketable> pendingDisassembly = List.of();
    private boolean disassemblyMode;
    private final Set<Socketable> markedForDisassembly = new LinkedHashSet<>();
    private final List<Runnable> queuedActions = new ArrayList<>();
    private int movedFromCargo;
    private String disassembledNotice;
    private final SocketableHoverTooltip hoverTooltip;

    private SocketStoragePanel(CustomPanelAPI hostPanel, Function<Socketable, String> installedShipLookup, Listener storageListener) {
        this.hostPanel = hostPanel;
        this.hoverTooltip = new SocketableHoverTooltip(hostPanel);
        this.installedShipLookup = installedShipLookup;
        this.storageListener = storageListener;
    }

    public static SocketStoragePanel open(CustomPanelAPI hostPanel, float panelLeft, float panelTop, float panelHeight,
                                          Function<Socketable, String> installedShipLookup, Listener storageListener) {
        SocketStoragePanel storagePanel = new SocketStoragePanel(hostPanel, installedShipLookup, storageListener);
        storagePanel.panelWidth = WIDTH;
        storagePanel.panelHeight = panelHeight;
        storagePanel.panelLeft = panelLeft;
        storagePanel.panelTop = panelTop;
        storagePanel.panelRoot = Global.getSettings().createCustom(WIDTH, panelHeight, storagePanel);
        hostPanel.addComponent(storagePanel.panelRoot).inTL(panelLeft, panelTop);
        storagePanel.movedFromCargo = absorbPlayerCargo();
        I18n.forGameText(storagePanel::build);
        storagePanel.holoTransition.open();
        storagePanel.applyContentOpacity(0f);
        return storagePanel;
    }

    private static int absorbPlayerCargo() {
        CampaignFleetAPI fleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        return fleet == null ? 0 : SocketableStore.get().absorbFrom(fleet.getCargo());
    }

    public void close() {
        if (panelRoot == null || closing) {
            return;
        }
        closing = true;
        hideHoverTooltip();
        if (workbenchPanel != null) {
            workbenchPanel.close();
        }
        holoTransition.close();
        applyContentOpacity(holoTransition.contentAlpha());
        storageListener.closed();
    }

    private void applyContentOpacity(float opacity) {
        float chromeOpacity = opacity >= 1f && !closing ? 1f : 0f;
        for (UIComponentAPI component : new UIComponentAPI[]{headerElement, searchRowElement}) {
            if (component != null) {
                component.setOpacity(chromeOpacity);
            }
        }
        for (UIComponentAPI component : new UIComponentAPI[]{controlsElement, gridElement == null ? null : gridComponent(), noticeElement,
                summaryElement, confirmPanel, confirmBlocker}) {
            if (component != null) {
                component.setOpacity(opacity);
            }
        }
    }

    private UIComponentAPI gridComponent() {
        return gridElement.getExternalScroller() != null ? gridElement.getExternalScroller() : gridElement;
    }

    public boolean contains(float x, float y) {
        return panelRoot != null && !closing && Rects.contains(panelPosition, x, y);
    }

    public void setSelected(Socketable socketable) {
        selectedSocketable = socketable;
        if (socketable != null) {
            leaveDisassemblyMode();
        }
        queuedActions.add(() -> rebuildFooter(SocketStorageQuery.apply(storageRows, storageFilter)));
    }

    public void setTargetingSocket(boolean targeting) {
        if (targeting == targetingSocket) {
            return;
        }
        targetingSocket = targeting;
        if (targeting) {
            leaveDisassemblyMode();
        }
        queuedActions.add(() -> rebuildFooter(SocketStorageQuery.apply(storageRows, storageFilter)));
    }

    public void refresh(Function<Socketable, String> installedShipLookup) {
        this.installedShipLookup = installedShipLookup;
        queuedActions.add(() -> {
            if (pendingDisassembly.stream().anyMatch(SocketCustody::isInstalled)) {
                closeConfirm();
            }
            reloadRows();
            markedForDisassembly.removeIf(socketable -> SocketCustody.isInstalled(socketable)
                    || !SocketableStore.get().owned().contains(socketable));
            rebuildGrid();
            if (workbenchPanel != null) {
                workbenchPanel.refresh();
            }
        });
    }

    public boolean escape() {
        if (confirmPanel != null) {
            queuedActions.add(this::closeConfirm);
            return true;
        }
        if (disassemblyMode) {
            queuedActions.add(() -> {
                leaveDisassemblyMode();
                rebuildGrid();
            });
            return true;
        }
        if (workbenchPanel != null) {
            queuedActions.add(workbenchPanel::close);
            return true;
        }
        return false;
    }

    private void leaveDisassemblyMode() {
        disassemblyMode = false;
        markedForDisassembly.clear();
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
        holoTransition.drawProjection(panelPosition.getX(), panelPosition.getY(), panelPosition.getWidth(), panelPosition.getHeight(),
                SkillTreePanelStyle.GLOW_COLOR, alphaMult);
        float contentAlpha = holoTransition.contentAlpha();
        if (contentAlpha > 0f) {
            panelFrame.draw(panelPosition.getX(), panelPosition.getY(), panelPosition.getWidth(), panelPosition.getHeight(),
                    contentAlpha * alphaMult);
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (closing) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isMouseScrollEvent() && contains(event.getX(), event.getY())) {
                queuedActions.add(this::hideHoverTooltip);
            }
            boolean press = event.isMouseDownEvent() || event.isMouseScrollEvent();
            if (!event.isConsumed() && press && contains(event.getX(), event.getY())) {
                event.consume();
            }
        }
    }

    @Override
    public void advance(float amount) {
        if (holoTransition.isAnimating()) {
            holoTransition.advance(amount);
            applyContentOpacity(holoTransition.contentAlpha());
        }
        if (closing) {
            if (holoTransition.isFullyClosed() && panelRoot != null) {
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
            hoverTooltip.refreshIfExpansionChanged();
        }
        if (panelRoot == null || searchField == null || confirmPanel != null) {
            return;
        }
        String searchText = searchField.getText();
        if (!searchText.equals(typedQuery)) {
            typedQuery = searchText;
            searchDelay = SEARCH_DELAY_SECONDS;
        } else if (searchDelay > 0f) {
            searchDelay -= amount;
            if (searchDelay <= 0f) {
                storageFilter.setQuery(typedQuery);
                I18n.forGameText(this::rebuildGrid);
            }
        }
    }

    @Override
    public void buttonPressed(Object id) {
        queuedActions.add(() -> handleButton(id));
    }

    private void handleButton(Object id) {
        if (confirmPanel != null) {
            if (id == Control.CONFIRM_DISASSEMBLE) {
                List<Socketable> targets = pendingDisassembly.stream().filter(socketable -> !SocketCustody.isInstalled(socketable)).toList();
                if (selectedSocketable != null && targets.contains(selectedSocketable)) {
                    storageListener.selected(null);
                }
                disassemble(targets);
                if (workbenchPanel != null) {
                    workbenchPanel.refresh();
                }
                if (disassemblyMode) {
                    leaveDisassemblyMode();
                }
                closeConfirm();
                reloadRows();
                buildHeader();
                rebuildControls();
            } else if (id == Control.CANCEL_DISASSEMBLE) {
                closeConfirm();
            }
            return;
        }
        if (id == Control.CLOSE) {
            close();
        } else if (id == Control.WORKBENCH) {
            toggleWorkbench();
        } else if (id == Control.DISASSEMBLY_MODE) {
            disassemblyModePressed();
        } else if (id instanceof StatusChip chip) {
            storageFilter.setStatus(chip.status());
            rebuildControls();
        } else if (id instanceof RarityChip chip) {
            storageFilter.toggleRarity(chip.rarity());
            rebuildControls();
        }
    }

    private void disassemblyModePressed() {
        if (!disassemblyMode) {
            disassemblyMode = true;
            if (selectedSocketable != null) {
                storageListener.selected(null);
            }
            rebuildFooter(SocketStorageQuery.apply(storageRows, storageFilter));
        } else if (markedForDisassembly.isEmpty()) {
            leaveDisassemblyMode();
            rebuildFooter(SocketStorageQuery.apply(storageRows, storageFilter));
        } else {
            openConfirm(List.copyOf(markedForDisassembly));
        }
    }

    private void cellClicked(SocketStorageRow row) {
        if (confirmPanel != null || row.installed()) {
            return;
        }
        if (disassemblyMode) {
            queuedActions.add(() -> {
                if (!markedForDisassembly.remove(row.socketable())) {
                    markedForDisassembly.add(row.socketable());
                }
                rebuildFooter(SocketStorageQuery.apply(storageRows, storageFilter));
            });
            return;
        }
        if (workbenchPanel != null) {
            workbenchPanel.load(row.socketable());
            return;
        }
        queuedActions.add(() -> storageListener.selected(row.socketable() == selectedSocketable ? null : row.socketable()));
    }

    public boolean isWorkbenchOpen() {
        return workbenchPanel != null || (fadingWorkbenchPanel != null && fadingWorkbenchPanel.isVisible());
    }

    private void toggleWorkbench() {
        if (workbenchPanel != null) {
            workbenchPanel.close();
            return;
        }
        if (selectedSocketable != null) {
            storageListener.selected(null);
        }
        SkillTreeSounds.panelOpened();
        workbenchPanel = SocketWorkbenchPanel.open(hostPanel, panelLeft + panelWidth + WORKBENCH_GAP, panelTop, panelHeight,
                new SocketWorkbenchPanel.Listener() {
                    @Override
                    public void changed(Socketable loaded) {
                        List<Socketable> owned = SocketableStore.get().owned();
                        if (pendingDisassembly.stream().anyMatch(socketable -> !owned.contains(socketable))) {
                            closeConfirm();
                        } else if (confirmPanel != null) {
                            List<Socketable> pendingTargets = pendingDisassembly;
                            closeConfirm();
                            openConfirm(pendingTargets);
                        }
                        markedForDisassembly.removeIf(socketable -> !owned.contains(socketable));
                        reloadRows();
                        buildHeader();
                        rebuildGrid();
                    }

                    @Override
                    public void closed() {
                        fadingWorkbenchPanel = workbenchPanel;
                        workbenchPanel = null;
                    }
                });
    }

    private void cellRightClicked(SocketStorageRow row) {
        if (confirmPanel == null && !disassemblyMode && !row.installed()) {
            queuedActions.add(() -> openConfirm(List.of(row.socketable())));
        }
    }

    private float innerWidth() {
        return panelWidth - PAD * 2f;
    }

    private void build() {
        buildHeader();
        buildSearch();
        reloadRows();
        rebuildControls();
    }

    private void buildHeader() {
        if (headerElement != null) {
            panelRoot.removeComponent(headerElement);
        }
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), HEADER_HEIGHT, false);
        element.setParaFont(Fonts.ORBITRON_20AABOLD);
        element.addPara("%s", 0f, Misc.getBasePlayerColor(), Misc.getBasePlayerColor(), Translation.text("ui.socketStorage.title"));
        ButtonAPI closeButton = element.addButton(Translation.text("ui.socketStorage.close"), Control.CLOSE, CLOSE_BUTTON_WIDTH, CHIP_HEIGHT,
                0f);
        closeButton.getPosition().inTR(0f, 0f);
        String workbenchLabel = Translation.text("ui.socketStorage.workbench");
        float workbenchButtonWidth = Global.getSettings().computeStringWidth(workbenchLabel, Fonts.ORBITRON_20AA) + BUTTON_TEXT_PADDING;
        element.addButton(workbenchLabel, Control.WORKBENCH, workbenchButtonWidth, CHIP_HEIGHT, 0f).getPosition()
                .inTR(CLOSE_BUTTON_WIDTH + GAP, 0f);
        addPartsCount(element, CLOSE_BUTTON_WIDTH + GAP + workbenchButtonWidth + GAP * 2f);
        panelRoot.addUIElement(element).inTL(PAD, PAD);
        headerElement = element;
    }

    private static void addPartsCount(TooltipMakerAPI element, float countRight) {
        CommoditySpecAPI partsSpec = Global.getSettings().getCommoditySpec(SocketableDisassembly.PARTS_COMMODITY_ID);
        if (partsSpec == null) {
            return;
        }
        String countText = String.valueOf(partsInCargo());
        float countWidth = Global.getSettings().computeStringWidth(countText, Fonts.ORBITRON_12) + LINE_PAD;
        element.setParaFont(Fonts.ORBITRON_12);
        LabelAPI countLabel = element.addPara("%s", 0f, Misc.getTextColor(), Misc.getTextColor(), countText);
        countLabel.autoSizeToWidth(countWidth).inTR(countRight, PARTS_TEXT_TOP);
        element.addImage(partsSpec.getIconName(), PARTS_ICON_SIZE, PARTS_ICON_SIZE, 0f);
        element.getPrev().getPosition().inTR(countRight + countWidth + LINE_PAD, 0f);
        element.addTooltipToPrevious(new PartsTooltip(partsSpec.getName()), TooltipMakerAPI.TooltipLocation.BELOW);
    }

    private static int partsInCargo() {
        CampaignFleetAPI fleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        return fleet == null ? 0 : Math.round(fleet.getCargo().getCommodityQuantity(SocketableDisassembly.PARTS_COMMODITY_ID));
    }

    private record PartsTooltip(String partsName) implements TooltipMakerAPI.TooltipCreator {

        private static final float WIDTH = 300f;

        @Override
        public boolean isTooltipExpandable(Object tooltipParam) {
            return false;
        }

        @Override
        public float getTooltipWidth(Object tooltipParam) {
            return WIDTH;
        }

        @Override
        public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
            tooltip.addPara("%s", 0f, Misc.getTextColor(), Misc.getTextColor(),
                    Translation.msg("ui.socketStorage.parts.tooltip").arg("name", partsName).text());
        }
    }

    private void buildSearch() {
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), FIELD_HEIGHT, false);
        element.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), Translation.text("ui.socketStorage.search"))
                .getPosition().inTL(0f, LINE_PAD * 2f);
        searchField = element.addTextField(innerWidth() - LABEL_WIDTH, FIELD_HEIGHT, Fonts.DEFAULT_SMALL, 0f);
        searchField.getPosition().inTL(LABEL_WIDTH, 0f);
        searchField.setText(storageFilter.query);
        typedQuery = storageFilter.query;
        panelRoot.addUIElement(element).inTL(PAD, PAD + HEADER_HEIGHT + GAP);
        searchRowElement = element;
    }

    private void reloadRows() {
        List<Socketable> owned = SocketableStore.get().owned();
        List<SocketStorageRow> built = new ArrayList<>(owned.size());
        for (int i = 0; i < owned.size(); i++) {
            built.add(SocketStorageRow.of(owned.get(i), i, installedShipLookup));
        }
        storageRows = built;
    }

    private void rebuildControls() {
        if (controlsElement != null) {
            panelRoot.removeComponent(controlsElement);
        }
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), CONTROL_ROWS * CHIP_HEIGHT + (CONTROL_ROWS - 1) * GAP, false);
        float chipLeft = 0f;
        for (SocketStorageFilter.Status status : SocketStorageFilter.Status.values()) {
            String label = Translation.text("ui.socketStorage.status." + status.name().toLowerCase(Locale.ROOT));
            chipLeft += addChip(element, label, new StatusChip(status), ChipColors.player(), storageFilter.status == status, chipLeft, 0) + GAP;
        }
        chipLeft = 0f;
        for (SocketableRarity rarity : SocketableRarity.values()) {
            String label = Translation.text("ui.socketStorage.rarity." + rarity.name().toLowerCase(Locale.ROOT));
            chipLeft += addChip(element, label, new RarityChip(rarity), ChipColors.of(rarity.color()), storageFilter.rarities.contains(rarity),
                    chipLeft, 1) + GAP;
        }
        float controlsTop = PAD + HEADER_HEIGHT + GAP + FIELD_HEIGHT + GAP;
        panelRoot.addUIElement(element).inTL(PAD, controlsTop);
        controlsElement = element;
        gridTop = controlsTop + CONTROL_ROWS * (CHIP_HEIGHT + GAP) + NOTICE_HEIGHT;
        rebuildGrid();
    }

    private static float addChip(TooltipMakerAPI element, String label, Object chipId, ChipColors colors, boolean checked, float chipLeft,
                                 int chipRow) {
        float chipWidth = Global.getSettings().computeStringWidth(label, Fonts.ORBITRON_12) + CHIP_TEXT_PADDING;
        ButtonAPI chip = element.addAreaCheckbox(label, chipId, colors.base(), colors.dark(), colors.bright(), chipWidth, CHIP_HEIGHT, 0f);
        chip.setChecked(checked);
        chip.getPosition().inTL(chipLeft, chipRow * (CHIP_HEIGHT + GAP));
        return chipWidth;
    }

    private void rebuildGrid() {
        hideHoverTooltip();
        if (gridElement != null) {
            panelRoot.removeComponent(gridElement.getExternalScroller() != null ? gridElement.getExternalScroller() : gridElement);
        }
        List<SocketStorageRow> matching = SocketStorageQuery.apply(storageRows, storageFilter);
        float gridHeight = Math.max(CELL_SIZE, panelHeight - PAD - FOOTER_HEIGHT - GAP - gridTop);
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), gridHeight, true);
        if (matching.isEmpty()) {
            String key = storageRows.isEmpty() ? "ui.socketStorage.empty" : "ui.socketStorage.noMatches";
            element.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), Translation.text(key));
        }
        for (int start = 0; start < matching.size(); start += COLUMNS) {
            CustomPanelAPI rowPanel = Global.getSettings().createCustom(innerWidth() - SCROLLBAR_ROOM, CELL_SIZE, null);
            for (int i = start; i < Math.min(matching.size(), start + COLUMNS); i++) {
                SocketStorageRow row = matching.get(i);
                CustomPanelAPI cellPanel = Global.getSettings().createCustom(CELL_SIZE, CELL_SIZE, cellFor(row));
                rowPanel.addComponent(cellPanel).inTL((i - start) * (CELL_SIZE + GAP), 0f);
            }
            element.addCustom(rowPanel, start == 0 ? 0f : GAP);
        }
        panelRoot.addUIElement(element).inTL(PAD, gridTop);
        gridElement = element;
        rebuildFooter(matching);
    }

    private void rebuildFooter(List<SocketStorageRow> matching) {
        int shownCount = matching.size();
        if (noticeElement != null) {
            panelRoot.removeComponent(noticeElement);
        }
        if (summaryElement != null) {
            panelRoot.removeComponent(summaryElement);
        }
        TooltipMakerAPI newNotice = panelRoot.createUIElement(innerWidth(), NOTICE_HEIGHT, false);
        if (disassemblyMode) {
            int markedParts = markedForDisassembly.stream().mapToInt(socketable -> socketable.rarity().disassemblyParts()).sum();
            String hint = markedForDisassembly.isEmpty() ? Translation.text("ui.socketStorage.disassemblyHint")
                    : Translation.msg("ui.socketStorage.disassemblyMarked").arg("count", markedForDisassembly.size()).arg("parts", markedParts)
                    .text();
            newNotice.addPara("%s", 0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(), hint);
        } else if (selectedSocketable != null) {
            newNotice.addPara("%s", 0f, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(),
                    Translation.msg("ui.socketStorage.placingHint").arg("name", selectedSocketable.name()).text());
        } else if (targetingSocket) {
            newNotice.addPara("%s", 0f, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(),
                    Translation.text("ui.socketStorage.targetHint"));
        } else if (disassembledNotice != null) {
            newNotice.addPara("%s", 0f, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(), disassembledNotice);
        } else if (movedFromCargo > 0) {
            newNotice.addPara("%s", 0f, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(),
                    Translation.msg("ui.socketStorage.moved").count(movedFromCargo).arg("count", movedFromCargo).text());
        }
        panelRoot.addUIElement(newNotice).inTL(PAD, gridTop - NOTICE_HEIGHT);
        noticeElement = newNotice;

        TooltipMakerAPI newSummary = panelRoot.createUIElement(innerWidth(), FOOTER_HEIGHT, false);
        int installedCount = (int) storageRows.stream().filter(SocketStorageRow::installed).count();
        newSummary.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), Translation.msg("ui.socketStorage.summary")
                .arg("shown", shownCount).arg("stored", storageRows.size()).arg("installed", installedCount).text())
                .getPosition().inTL(0f, PARTS_TEXT_TOP);
        String modeKey = !disassemblyMode ? "ui.socketStorage.disassemblyMode"
                : markedForDisassembly.isEmpty() ? "ui.socketStorage.exitDisassemblyMode" : "ui.socketStorage.confirmDisassembly";
        String modeLabel = Translation.text(modeKey);
        float modeWidth = Global.getSettings().computeStringWidth(modeLabel, Fonts.ORBITRON_20AA) + BUTTON_TEXT_PADDING;
        ButtonAPI modeButton = newSummary.addButton(modeLabel, Control.DISASSEMBLY_MODE, modeWidth, FOOTER_HEIGHT, 0f);
        modeButton.getPosition().inTR(0f, 0f);
        modeButton.setEnabled(disassemblyMode || storageRows.stream().anyMatch(row -> !row.installed()));
        panelRoot.addUIElement(newSummary).inTL(PAD, panelHeight - PAD - FOOTER_HEIGHT);
        summaryElement = newSummary;
    }

    private void openConfirm(List<Socketable> targets) {
        if (targets.isEmpty()) {
            return;
        }
        pendingDisassembly = List.copyOf(targets);
        boolean singleTarget = targets.size() == 1;
        float confirmHeight = singleTarget ? CONFIRM_HEIGHT : BATCH_CONFIRM_HEIGHT;
        confirmBlocker = Global.getSettings().createCustom(panelWidth, panelHeight, new Blocker());
        panelRoot.addComponent(confirmBlocker).inTL(0f, 0f);
        float confirmWidth = innerWidth();
        confirmPanel = Global.getSettings().createCustom(confirmWidth, confirmHeight,
                new FramedPanelPlugin(SocketStoragePanel.class, this::buttonPressed));
        TooltipMakerAPI element = confirmPanel.createUIElement(confirmWidth - PAD * 2f, confirmHeight - PAD * 2f, false);
        int totalParts = targets.stream().mapToInt(socketable -> socketable.rarity().disassemblyParts()).sum();
        if (singleTarget) {
            Socketable socketable = targets.get(0);
            element.addPara("%s", 0f, socketable.rarity().color(), socketable.rarity().color(),
                    Translation.msg("ui.socketStorage.confirm.title").arg("name", socketable.name()).text());
            element.addPara("%s", LINE_PAD * 2f, Misc.getTextColor(), Misc.getTextColor(),
                    partsText("ui.socketStorage.confirm.body", totalParts));
        } else {
            element.addPara("%s", 0f, Misc.getBasePlayerColor(), Misc.getBasePlayerColor(),
                    Translation.msg("ui.socketStorage.confirm.batchTitle").arg("count", targets.size()).text());
            addRarityBreakdown(element, targets);
            element.addPara("%s", LINE_PAD * 2f, Misc.getTextColor(), Misc.getTextColor(),
                    Translation.msg("ui.socketStorage.confirm.batchBody").arg("count", totalParts).text());
            if (targets.stream().anyMatch(socketable -> socketable.rarity() == SocketableRarity.UNIQUE)) {
                element.addPara("%s", LINE_PAD * 2f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(),
                        Translation.text("ui.socketStorage.confirm.includesUnique"));
            }
        }
        float buttonWidth = (confirmWidth - PAD * 2f - GAP) / 2f;
        element.addButton(Translation.text("ui.socketStorage.confirm.disassemble"), Control.CONFIRM_DISASSEMBLE, buttonWidth, FIELD_HEIGHT, 0f)
                .getPosition().inBR(0f, 0f);
        element.addButton(Translation.text("ui.socketStorage.confirm.cancel"), Control.CANCEL_DISASSEMBLE, buttonWidth, FIELD_HEIGHT, 0f)
                .getPosition().inBL(0f, 0f);
        confirmPanel.addUIElement(element).inTL(PAD, PAD);
        panelRoot.addComponent(confirmPanel).inTL(PAD, (panelHeight - confirmHeight) / 2f);
    }

    private static void addRarityBreakdown(TooltipMakerAPI element, List<Socketable> targets) {
        List<String> rarityCounts = new ArrayList<>();
        List<Color> rarityColors = new ArrayList<>();
        for (SocketableRarity rarity : SocketableRarity.values()) {
            long count = targets.stream().filter(socketable -> socketable.rarity() == rarity).count();
            if (count > 0) {
                rarityCounts.add(Translation.msg("ui.socketStorage.confirm.rarityCount").arg("count", count)
                        .arg("rarity", Translation.text("ui.socketStorage.rarity." + rarity.name().toLowerCase(Locale.ROOT))).text());
                rarityColors.add(rarity.color());
            }
        }
        String format = String.join(Translation.text("ui.socketStorage.confirm.raritySeparator"),
                rarityCounts.stream().map(text -> "%s").toList());
        element.addPara(format, LINE_PAD * 2f, rarityColors.toArray(new Color[0]), rarityCounts.toArray(new String[0]));
    }

    private void disassemble(List<Socketable> targets) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        CargoAPI cargo = playerFleet == null ? null : playerFleet.getCargo();
        int totalParts = 0;
        int disassembledCount = 0;
        Socketable lastDisassembled = null;
        for (Socketable socketable : targets) {
            int gainedParts = SocketableDisassembly.disassemble(socketable, cargo);
            if (gainedParts > 0) {
                totalParts += gainedParts;
                disassembledCount++;
                lastDisassembled = socketable;
            }
        }
        if (disassembledCount == 1) {
            disassembledNotice = Translation.msg("ui.socketStorage.disassembled").count(totalParts)
                    .arg("name", lastDisassembled.name()).arg("count", totalParts).text();
        } else if (disassembledCount > 1) {
            disassembledNotice = Translation.msg("ui.socketStorage.disassembledMany").arg("count", disassembledCount)
                    .arg("parts", totalParts).text();
        }
    }

    private static String partsText(String key, int partCount) {
        return Translation.msg(key).count(partCount).arg("count", partCount).text();
    }

    private void closeConfirm() {
        if (confirmPanel == null) {
            return;
        }
        panelRoot.removeComponent(confirmPanel);
        panelRoot.removeComponent(confirmBlocker);
        confirmPanel = null;
        confirmBlocker = null;
        pendingDisassembly = List.of();
    }

    private List<StyledText> cellFooter(SocketStorageRow row) {
        if (row.installed()) {
            return List.of(Translation.msg("ui.socketStorage.cell.installed").arg("ship", row.installedShipName()).styled());
        }
        if (disassemblyMode) {
            int partCount = row.socketable().rarity().disassemblyParts();
            String key = markedForDisassembly.contains(row.socketable()) ? "ui.socketStorage.cell.unmark" : "ui.socketStorage.cell.mark";
            return List.of(Translation.msg(key).count(partCount).arg("count", partCount).styled());
        }
        String installKey = workbenchPanel != null ? "ui.socketStorage.cell.workbench"
                : targetingSocket ? "ui.socketStorage.cell.freeTarget" : "ui.socketStorage.cell.free";
        return List.of(Translation.styled(installKey), Translation.msg("ui.socketStorage.cell.disassemble")
                .count(row.socketable().rarity().disassemblyParts()).arg("count", row.socketable().rarity().disassemblyParts()).styled());
    }

    private SocketableCell cellFor(SocketStorageRow row) {
        return new SocketableCell(row.socketable(), CELL_LOOK, new SocketableCell.Listener() {
            @Override
            public void hovered(PositionAPI cellPosition) {
                cellHovered(row, cellPosition);
            }

            @Override
            public void left() {
                cellLeft(row);
            }

            @Override
            public boolean clicked() {
                cellClicked(row);
                return true;
            }

            @Override
            public boolean rightClicked() {
                cellRightClicked(row);
                return true;
            }

            @Override
            public boolean selected() {
                return row.socketable() == selectedSocketable || (workbenchPanel != null && row.socketable() == workbenchPanel.loaded());
            }

            @Override
            public boolean marked() {
                return markedForDisassembly.contains(row.socketable());
            }

            @Override
            public boolean dimmed() {
                return row.installed();
            }
        });
    }

    private void cellHovered(SocketStorageRow row, PositionAPI cellPosition) {
        queuedActions.add(() -> showHoverTooltip(row, cellPosition));
    }

    private void cellLeft(SocketStorageRow row) {
        queuedActions.add(() -> {
            if (hoverTooltip.isShowing(row)) {
                hideHoverTooltip();
            }
        });
    }

    private void showHoverTooltip(SocketStorageRow row, PositionAPI cellPosition) {
        hoverTooltip.show(row, row.socketable(), () -> cellFooter(row), cellPosition);
    }

    private void hideHoverTooltip() {
        hoverTooltip.hide();
    }



    private static final class Blocker extends BaseCustomUIPanelPlugin {

        private PositionAPI blockerPosition;

        @Override
        public void positionChanged(PositionAPI blockerPosition) {
            this.blockerPosition = blockerPosition;
        }

        @Override
        public void processInput(List<InputEventAPI> events) {
            for (InputEventAPI event : events) {
                boolean escape = event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE;
                boolean press = (event.isMouseDownEvent() || event.isMouseScrollEvent()) && blockerPosition != null
                        && blockerPosition.containsEvent(event);
                if (!event.isConsumed() && (press || (event.isKeyboardEvent() && !escape))) {
                    event.consume();
                }
            }
        }
    }
}
