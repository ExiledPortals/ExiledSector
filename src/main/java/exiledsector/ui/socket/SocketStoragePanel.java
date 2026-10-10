package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.socketables.SocketCustody;
import exiledsector.socketables.SocketType;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableDisassembly;
import exiledsector.socketables.SocketableRarity;
import exiledsector.socketables.SocketableStore;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.framework.FrameworkSocketFlair;
import exiledsector.ui.util.FramedPanelPlugin;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

public final class SocketStoragePanel extends HoloPanel {

    public interface Listener {

        void selected(Socketable socketable);

        void closed();

        void pressedInside();
    }

    private static final float FIELD_HEIGHT = 26f;
    private static final float CHIP_HEIGHT = 24f;
    private static final float CHIP_TEXT_PADDING = 28f;
    private static final float CHIP_DARK_SCALE = 0.3f;
    private static final int FIXED_CONTROL_ROWS = 2;
    private static final float LABEL_WIDTH = 70f;
    private static final float NOTICE_HEIGHT = 56f;
    private static final float FOOTER_HEIGHT = 24f;
    private static final float SCROLLBAR_ROOM = 14f;
    private static final float CELL_SIZE = 96f;
    private static final int COLUMNS = 6;
    public static final float WIDTH = PAD * 2f + SCROLLBAR_ROOM + COLUMNS * CELL_SIZE + (COLUMNS - 1) * GAP;
    private static final float CONFIRM_HEIGHT = 130f;
    private static final float WORKBENCH_GAP = 12f;
    private static final float BATCH_CONFIRM_HEIGHT = 170f;
    private static final float BUTTON_TEXT_PADDING = 30f;
    private static final float SEARCH_DELAY_SECONDS = 0.25f;
    private static final SocketableCell.Look CELL_LOOK = new SocketableCell.Look(9f, true, 1f);

    private enum Control {CLOSE, WORKBENCH, DISASSEMBLY_MODE, CONFIRM_DISASSEMBLE, CANCEL_DISASSEMBLE}

    private record StatusChip(SocketStorageFilter.Status status) {
    }

    private record RarityChip(SocketableRarity rarity) {
    }

    private record TypeChip(SocketType socketType) {
    }

    private record ChipColors(Color base, Color dark, Color bright) {

        static ChipColors of(Color base) {
            return new ChipColors(base, Misc.scaleColorOnly(base, CHIP_DARK_SCALE), base.brighter());
        }

        static ChipColors player() {
            return new ChipColors(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor());
        }
    }

    private final Listener storageListener;
    private final SocketStorageFilter storageFilter = SocketStorageFilter.SESSION;

    private Function<Socketable, String> installedShipLookup;
    private SocketWorkbenchPanel workbenchPanel;
    private SocketWorkbenchPanel fadingWorkbenchPanel;
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
    private int movedFromCargo;
    private String disassembledNotice;
    private final SocketableHoverTooltip hoverTooltip;
    private SocketType kindFilter;

    private SocketStoragePanel(CustomPanelAPI hostPanel, Function<Socketable, String> installedShipLookup, Listener storageListener,
                               SocketType kindFilter) {
        super(hostPanel, SocketStoragePanel.class);
        this.kindFilter = kindFilter;
        this.hoverTooltip = new SocketableHoverTooltip(hostPanel);
        this.installedShipLookup = installedShipLookup;
        this.storageListener = storageListener;
    }

    public static SocketStoragePanel open(CustomPanelAPI hostPanel, float panelLeft, float panelTop, float panelHeight,
                                          Function<Socketable, String> installedShipLookup, Listener storageListener) {
        return open(hostPanel, panelLeft, panelTop, panelHeight, installedShipLookup, storageListener, null);
    }

    public static SocketStoragePanel open(CustomPanelAPI hostPanel, float panelLeft, float panelTop, float panelHeight,
                                          Function<Socketable, String> installedShipLookup, Listener storageListener, SocketType kindFilter) {
        SocketStoragePanel storagePanel = new SocketStoragePanel(hostPanel, installedShipLookup, storageListener, kindFilter);
        storagePanel.movedFromCargo = absorbPlayerCargo();
        storagePanel.attach(panelLeft, panelTop, WIDTH, panelHeight);
        return storagePanel;
    }

    private static int absorbPlayerCargo() {
        CargoAPI cargo = playerCargo();
        return cargo == null ? 0 : SocketableStore.get().absorbFrom(cargo);
    }

    @Override
    protected void onClose() {
        hideHoverTooltip();
        if (workbenchPanel != null) {
            workbenchPanel.close();
        }
        storageListener.closed();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement, searchRowElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{controlsElement, gridElement == null ? null : gridComponent(), noticeElement, summaryElement, confirmPanel,
                confirmBlocker};
    }

    private UIComponentAPI gridComponent() {
        return gridElement.getExternalScroller() != null ? gridElement.getExternalScroller() : gridElement;
    }

    public void restrictTo(SocketType socketType) {
        if (socketType == kindFilter) {
            return;
        }
        kindFilter = socketType;
        queue(() -> {
            reloadRows();
            rebuildControls();
        });
    }

    public void setSelected(Socketable socketable) {
        selectedSocketable = socketable;
        if (socketable != null) {
            leaveDisassemblyMode();
        }
        queue(() -> rebuildFooter(SocketStorageQuery.apply(storageRows, storageFilter)));
    }

    public void setTargetingSocket(boolean targeting) {
        if (targeting == targetingSocket) {
            return;
        }
        targetingSocket = targeting;
        if (targeting) {
            leaveDisassemblyMode();
        }
        queue(() -> rebuildFooter(SocketStorageQuery.apply(storageRows, storageFilter)));
    }

    public void refresh(Function<Socketable, String> installedShipLookup) {
        this.installedShipLookup = installedShipLookup;
        queue(() -> {
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
            queue(this::closeConfirm);
            return true;
        }
        if (disassemblyMode) {
            queue(() -> {
                leaveDisassemblyMode();
                rebuildGrid();
            });
            return true;
        }
        if (workbenchPanel != null) {
            queue(workbenchPanel::close);
            return true;
        }
        return false;
    }

    private void leaveDisassemblyMode() {
        disassemblyMode = false;
        markedForDisassembly.clear();
    }

    @Override
    protected void onScrollInside() {
        queue(this::hideHoverTooltip);
    }

    @Override
    protected void onPressInside() {
        storageListener.pressedInside();
    }

    @Override
    protected void advanceOpen(float amount) {
        hoverTooltip.refreshIfExpansionChanged();
        if (searchField == null || confirmPanel != null) {
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
        queue(() -> handleButton(id));
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
        } else if (id instanceof TypeChip chip) {
            storageFilter.toggleSocketType(chip.socketType());
            reloadRows();
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
            queue(() -> {
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
        queue(() -> storageListener.selected(row.socketable() == selectedSocketable ? null : row.socketable()));
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

                    @Override
                    public void pressedInside() {
                        storageListener.pressedInside();
                    }
                });
    }

    private void cellRightClicked(SocketStorageRow row) {
        if (confirmPanel == null && !disassemblyMode && !row.installed()) {
            queue(() -> openConfirm(List.of(row.socketable())));
        }
    }

    private float innerWidth() {
        return panelWidth - PAD * 2f;
    }

    @Override
    protected void build() {
        buildHeader();
        buildSearch();
        reloadRows();
        rebuildControls();
    }

    private void buildHeader() {
        TooltipMakerAPI element = startHeader("ui.socketStorage.title", "ui.socketStorage.close", Control.CLOSE);
        String workbenchLabel = Translation.text("ui.socketStorage.workbench");
        float workbenchButtonWidth = Global.getSettings().computeStringWidth(workbenchLabel, Fonts.ORBITRON_20AA) + BUTTON_TEXT_PADDING;
        element.addButton(workbenchLabel, Control.WORKBENCH, workbenchButtonWidth, HEADER_BUTTON_HEIGHT, 0f).getPosition()
                .inTR(CLOSE_BUTTON_WIDTH + GAP, 0f);
        headerElement = finishHeader(element, headerElement, CLOSE_BUTTON_WIDTH + GAP + workbenchButtonWidth + GAP * 2f);
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
            if (showsKind(owned.get(i).kind())) {
                built.add(SocketStorageRow.of(owned.get(i), i, installedShipLookup));
            }
        }
        storageRows = built;
    }

    private boolean showsKind(SocketType kind) {
        if (kindFilter != null) {
            return kind == kindFilter;
        }
        return storageFilter.socketTypes.isEmpty() || storageFilter.socketTypes.contains(kind);
    }

    private void rebuildControls() {
        if (controlsElement != null) {
            panelRoot.removeComponent(controlsElement);
        }
        List<SocketType> chipTypes = kindFilter == null ? List.of(SocketType.values()) : List.of();
        int typeRows = typeChipRows(chipTypes);
        int controlRows = FIXED_CONTROL_ROWS + typeRows;
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), controlRows * CHIP_HEIGHT + (controlRows - 1) * GAP, false);
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
        chipLeft = 0f;
        int chipRow = FIXED_CONTROL_ROWS;
        for (SocketType socketType : chipTypes) {
            String label = socketType.displayName();
            float chipWidth = chipWidth(label);
            if (chipLeft > 0f && chipLeft + chipWidth > innerWidth()) {
                chipLeft = 0f;
                chipRow++;
            }
            chipLeft += addChip(element, label, new TypeChip(socketType), ChipColors.of(typeColor(socketType)),
                    storageFilter.socketTypes.contains(socketType), chipLeft, chipRow) + GAP;
        }
        float controlsTop = PAD + HEADER_HEIGHT + GAP + FIELD_HEIGHT + GAP;
        panelRoot.addUIElement(element).inTL(PAD, controlsTop);
        controlsElement = element;
        gridTop = controlsTop + controlRows * (CHIP_HEIGHT + GAP) + NOTICE_HEIGHT;
        rebuildGrid();
    }

    private static float chipWidth(String label) {
        return Global.getSettings().computeStringWidth(label, Fonts.ORBITRON_12) + CHIP_TEXT_PADDING;
    }

    private int typeChipRows(List<SocketType> chipTypes) {
        if (chipTypes.isEmpty()) {
            return 0;
        }
        int rows = 1;
        float chipLeft = 0f;
        for (SocketType socketType : chipTypes) {
            float chipWidth = chipWidth(socketType.displayName());
            if (chipLeft > 0f && chipLeft + chipWidth > innerWidth()) {
                chipLeft = 0f;
                rows++;
            }
            chipLeft += chipWidth + GAP;
        }
        return rows;
    }

    private static Color typeColor(SocketType socketType) {
        return socketType.isFramework() ? FrameworkSocketFlair.colorOf(socketType) : Misc.getBasePlayerColor();
    }

    private static float addChip(TooltipMakerAPI element, String label, Object chipId, ChipColors colors, boolean checked, float chipLeft,
                                 int chipRow) {
        float chipWidth = chipWidth(label);
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
            String emptyText;
            if (storageRows.isEmpty() && kindFilter != null) {
                emptyText = Translation.msg("ui.socketStorage.emptyType").arg("type", kindFilter.displayName()).text();
            } else {
                emptyText = Translation.text(storageRows.isEmpty() && storageFilter.socketTypes.isEmpty() ? "ui.socketStorage.empty" : "ui.socketStorage.noMatches");
            }
            element.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), emptyText);
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
            String targetHint = kindFilter == null ? Translation.text("ui.socketStorage.targetHint")
                    : Translation.msg("ui.socketStorage.frameworkTargetHint").arg("type", kindFilter.displayName()).text();
            newNotice.addPara("%s", 0f, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(), targetHint);
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
        confirmBlocker = Global.getSettings().createCustom(panelWidth, panelHeight, new InputBlocker());
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
        queue(() -> showHoverTooltip(row, cellPosition));
    }

    private void cellLeft(SocketStorageRow row) {
        queue(() -> {
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
}
