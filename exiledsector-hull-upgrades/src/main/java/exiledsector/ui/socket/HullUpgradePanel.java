package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
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
import exiledsector.items.HullUpgradePoints;
import exiledsector.items.HullUpgradeRules;
import exiledsector.items.HullUpgradeTooltip;
import exiledsector.items.HullUpgradesSetting;
import exiledsector.items.MasterSchematic;
import exiledsector.items.RefitSchematic;
import exiledsector.items.ShipSection;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.VanillaText;
import exiledsector.ui.section.SectionFlair;

import java.awt.Color;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class HullUpgradePanel extends HoloPanel {

    public interface Listener {

        void applySchematic(RefitSchematic schematic);

        void applyMasterSchematic();

        void toggleSection(ShipSection section);

        void closed();
    }

    public enum SectionState {UNLOCKED, AVAILABLE, RESTRICTED, NO_POINTS}

    public record SectionEntry(ShipSection section, SectionState state, String requirementText, String itemIconPath) {
    }

    public record SchematicEntry(RefitSchematic schematic, int inStorage, HullUpgradeRules.UpgradeBlock block) {
    }

    public record MasterSchematicEntry(int inStorage, HullUpgradeRules.MasterSchematicBlock block) {
    }

    public record ChartProgress(boolean chartsEnabled, int nextTier) {
    }

    public record View(int points, int unspentPoints, int maxPoints, List<SchematicEntry> schematics, ChartProgress chartProgress,
                       List<SectionEntry> sections, MasterSchematicEntry masterSchematic) {
    }

    public static final float WIDTH = ItemStoragePanel.WIDTH;
    private static final int SECTION_COLUMNS = 3;
    private static final float UPGRADE_TILE_SIZE = 64f;
    private static final float POINTS_WIDTH = 80f;
    private static final float SCROLLBAR_ROOM = 14f;
    private static final float SECTION_NAME_HEIGHT = 36f;
    private static final float STATE_HEIGHT = 20f;
    private static final float TOOLTIP_WIDTH = 520f;
    private static final Color BLOCKED_TEXT_COLOR = new Color(130, 130, 130);
    private static final float CONFIRM_HEIGHT = 170f;
    private static final float POINTS_LINE_HEIGHT = 30f;

    private enum Control {CLOSE, CONFIRM_INSTALL, CANCEL_INSTALL, CONFIRM_MASTER_SCHEMATIC}

    private final Listener panelListener;
    private View view;
    private RefitSchematic confirmingSchematic;
    private UIComponentAPI headerElement;
    private TooltipMakerAPI listElement;

    private HullUpgradePanel(CustomPanelAPI hostPanel, View view, Listener panelListener) {
        super(hostPanel);
        this.view = view;
        this.panelListener = panelListener;
    }

    public static HullUpgradePanel open(CustomPanelAPI hostPanel, SocketPanelPlacement placement, View view, Listener panelListener,
                                        Consumer<SocketPanelTab> tabListener, Set<SocketPanelTab> shownTabs) {
        HullUpgradePanel panel = new HullUpgradePanel(hostPanel, view, panelListener);
        panel.setTabListener(tabListener);
        panel.setShownTabs(shownTabs);
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
                RefitSchematic confirmed = confirmingSchematic;
                closeConfirm();
                if (confirmed != null) {
                    panelListener.applySchematic(confirmed);
                }
            } else if (id == Control.CONFIRM_MASTER_SCHEMATIC) {
                closeConfirm();
                panelListener.applyMasterSchematic();
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

    private void openInstallConfirm(SchematicEntry entry) {
        RefitSchematic schematic = entry.schematic();
        openCurrencyConfirm(schematic.displayName(), schematic.color(), Translation.msg("framework.confirm.body")
                .arg("left", entry.inStorage() - 1).arg("points", schematic.points()).arg("max", view.maxPoints()).text(), Control.CONFIRM_INSTALL);
        confirmingSchematic = schematic;
    }

    private void openMasterSchematicConfirm(MasterSchematicEntry entry) {
        openCurrencyConfirm(MasterSchematic.INSTANCE.displayName(), MasterSchematic.COLOR,
                Translation.msg("masterSchematic.confirm.body").arg("left", entry.inStorage() - 1).text(), Control.CONFIRM_MASTER_SCHEMATIC);
        confirmingSchematic = null;
    }

    private void openCurrencyConfirm(String name, Color color, String body, Control confirmControl) {
        openConfirm(CONFIRM_HEIGHT, element -> {
            VanillaText.plainPara(element, Translation.msg("framework.confirm.title").arg("name", name).text(), 0f, color);
            VanillaText.plainPara(element, body, LINE_PAD * 2f, Misc.getTextColor());
        }, "framework.confirm.install", confirmControl, "framework.confirm.cancel", Control.CANCEL_INSTALL);
    }

    private void rebuildList() {
        float restoredScroll = listScrollOffset();
        hoverTooltip.hide();
        closeConfirm();
        float listWidth = panelWidth - PAD * 2f;
        float listTop = PAD + HEADER_HEIGHT + GAP;
        float listHeight = panelHeight - listTop - PAD;
        TooltipMakerAPI element = panelRoot.createUIElement(listWidth, listHeight, true);
        float gridWidth = listWidth - SCROLLBAR_ROOM;
        boolean masterSchematicShown = view.masterSchematic() != null;
        if (masterSchematicShown) {
            element.addSectionHeading(Translation.text("masterSchematic.panel.heading"), Alignment.MID, 0f);
            element.addCustom(masterSchematicRow(view.masterSchematic(), gridWidth), GAP * 2f);
        }
        element.addSectionHeading(Translation.text("framework.panel.upgrades"), Alignment.MID, masterSchematicShown ? GAP * 3f : 0f);
        boolean schematicsShown = view.chartProgress() == null;
        element.addCustom(schematicsShown ? schematicRow(gridWidth) : chartProgressRow(gridWidth), GAP * 2f);
        String hint = (schematicsShown ? Translation.msg("framework.panel.hint") : Translation.msg("framework.panel.hintCharts"))
                .arg("max", view.maxPoints()).text();
        VanillaText.plainPara(element, hint, GAP * 2f, Misc.getGrayColor());
        element.addSectionHeading(Translation.text("framework.panel.sockets"), Alignment.MID, GAP * 3f);
        float columnWidth = (gridWidth - GAP * (SECTION_COLUMNS - 1)) / SECTION_COLUMNS;
        float cellHeight = columnWidth + SECTION_NAME_HEIGHT + STATE_HEIGHT;
        List<SectionEntry> sections = view.sections();
        for (int start = 0; start < sections.size(); start += SECTION_COLUMNS) {
            int rowCount = Math.min(sections.size(), start + SECTION_COLUMNS) - start;
            float rowLeft = (gridWidth - rowCount * columnWidth - (rowCount - 1) * GAP) / 2f;
            CustomPanelAPI rowPanel = Global.getSettings().createCustom(gridWidth, cellHeight, null);
            for (int i = 0; i < rowCount; i++) {
                rowPanel.addComponent(sectionCell(sections.get(start + i), columnWidth, columnWidth))
                        .inTL(rowLeft + i * (columnWidth + GAP), 0f);
            }
            element.addCustom(rowPanel, GAP * 2f);
        }
        listElement = replaceElement(placedComponent(listElement), element, PAD, listTop);
        if (restoredScroll > 0f && element.getExternalScroller() != null) {
            element.getExternalScroller().setYOffset(Math.min(restoredScroll, Math.max(0f, element.getHeightSoFar() - listHeight)));
        }
        refreshOpacity();
    }

    private float listScrollOffset() {
        if (listElement == null || listElement.getExternalScroller() == null) {
            return 0f;
        }
        return listElement.getExternalScroller().getYOffset();
    }

    private CustomPanelAPI schematicRow(float gridWidth) {
        List<SchematicEntry> schematics = view.schematics();
        float tilesWidth = gridWidth - POINTS_WIDTH - GAP * 2f;
        float cellWidth = (tilesWidth - GAP * (schematics.size() - 1)) / schematics.size();
        float tileSize = Math.min(UPGRADE_TILE_SIZE, cellWidth);
        float rowHeight = tileSize + STATE_HEIGHT;
        CustomPanelAPI rowPanel = Global.getSettings().createCustom(gridWidth, rowHeight, null);
        for (int i = 0; i < schematics.size(); i++) {
            rowPanel.addComponent(schematicCell(schematics.get(i), cellWidth, tileSize)).inTL(i * (cellWidth + GAP), 0f);
        }
        addPointsLabel(rowPanel, gridWidth, rowHeight);
        return rowPanel;
    }

    private CustomPanelAPI schematicCell(SchematicEntry entry, float cellWidth, float tileSize) {
        RefitSchematic schematic = entry.schematic();
        IconTileCell.Hooks hooks = new IconTileCell.Hooks(() -> queue(() -> openInstallConfirm(entry)),
                tilePosition -> queue(() -> showSchematicTooltip(entry, tilePosition)), () -> queue(() -> hideTooltipOf(entry)));
        return currencyCell(schematic.iconPath(), schematic.color(), entry.block() != null, entry.inStorage(), cellWidth, tileSize, hooks);
    }

    private CustomPanelAPI currencyCell(String iconPath, Color color, boolean blocked, int inStorage, float cellWidth, float tileSize,
                                        IconTileCell.Hooks hooks) {
        CustomPanelAPI cellPanel = Global.getSettings().createCustom(cellWidth, tileSize + STATE_HEIGHT, null);
        cellPanel.addComponent(Global.getSettings().createCustom(tileSize, tileSize, new IconTileCell(iconPath, color, blocked, hooks)))
                .inTL((cellWidth - tileSize) / 2f, 0f);
        TooltipMakerAPI labelElement = cellPanel.createUIElement(cellWidth, STATE_HEIGHT, false);
        addLabel(labelElement, Translation.msg("framework.panel.inStorage").arg("count", inStorage).text(),
                blocked ? BLOCKED_TEXT_COLOR : color, Alignment.MID, cellWidth, 0f, STATE_HEIGHT);
        cellPanel.addUIElement(labelElement).inTL(0f, tileSize);
        return cellPanel;
    }

    private CustomPanelAPI masterSchematicRow(MasterSchematicEntry entry, float gridWidth) {
        float tileSize = UPGRADE_TILE_SIZE;
        float cellWidth = tileSize + GAP * 4f;
        CustomPanelAPI rowPanel = Global.getSettings().createCustom(gridWidth, tileSize + STATE_HEIGHT, null);
        IconTileCell.Hooks hooks = new IconTileCell.Hooks(() -> queue(() -> openMasterSchematicConfirm(entry)),
                tilePosition -> queue(() -> showMasterSchematicTooltip(entry, tilePosition)), () -> queue(() -> hideTooltipOf(entry)));
        rowPanel.addComponent(currencyCell(MasterSchematic.ICON_PATH, MasterSchematic.COLOR, entry.block() != null, entry.inStorage(), cellWidth,
                tileSize, hooks)).inTL(0f, 0f);
        float textLeft = cellWidth + GAP * 2f;
        TooltipMakerAPI textElement = rowPanel.createUIElement(gridWidth - textLeft, tileSize, false);
        VanillaText.plainPara(textElement, Translation.text("masterSchematic.panel.locked"), 0f, Misc.getTextColor());
        rowPanel.addUIElement(textElement).inTL(textLeft, 0f);
        return rowPanel;
    }

    private String masterSchematicStateText(MasterSchematicEntry entry) {
        HullUpgradeRules.MasterSchematicBlock block = entry.block();
        if (block == null) {
            return Translation.text("masterSchematic.panel.ready");
        }
        return switch (block) {
            case NOT_REQUIRED -> Translation.text("masterSchematic.tooltip.notRequired");
            case ALREADY_UNLOCKED -> Translation.text("masterSchematic.panel.already");
            case NONE_IN_STORAGE -> Translation.text("masterSchematic.panel.none");
        };
    }

    private void showMasterSchematicTooltip(MasterSchematicEntry entry, PositionAPI tilePosition) {
        if (isConfirmOpen()) {
            return;
        }
        hoverTooltip.show(entry, TOOLTIP_WIDTH, (tooltip, expanded) -> HullUpgradeTooltip.writeMasterSchematic(tooltip,
                () -> List.of(StyledText.of(masterSchematicStateText(entry)))), tilePosition);
    }

    private CustomPanelAPI chartProgressRow(float gridWidth) {
        CustomPanelAPI rowPanel = Global.getSettings().createCustom(gridWidth, UPGRADE_TILE_SIZE, null);
        float textWidth = gridWidth - POINTS_WIDTH - GAP * 2f;
        TooltipMakerAPI textElement = rowPanel.createUIElement(textWidth, UPGRADE_TILE_SIZE, false);
        VanillaText.plainPara(textElement, chartProgressText(view.chartProgress()), 0f, Misc.getTextColor());
        rowPanel.addUIElement(textElement).inTL(0f, 0f);
        addPointsLabel(rowPanel, gridWidth, UPGRADE_TILE_SIZE);
        return rowPanel;
    }

    private String chartProgressText(ChartProgress chartProgress) {
        if (!chartProgress.chartsEnabled()) {
            return Translation.text("framework.panel.chartsOff");
        }
        if (chartProgress.nextTier() <= 0) {
            return Translation.msg("framework.panel.chartsMax").arg("max", view.maxPoints()).text();
        }
        return Translation.msg("framework.panel.chartsNext").arg("tier", chartProgress.nextTier())
                .arg("points", HullUpgradePoints.chartPoints(chartProgress.nextTier())).text();
    }

    private void addPointsLabel(CustomPanelAPI rowPanel, float gridWidth, float rowHeight) {
        TooltipMakerAPI pointsElement = rowPanel.createUIElement(POINTS_WIDTH, POINTS_LINE_HEIGHT, false);
        pointsElement.setParaFont(Fonts.ORBITRON_24AABOLD);
        long unlockedSections = view.sections().stream().filter(entry -> entry.state() == SectionState.UNLOCKED).count();
        String points = unlockedSections + "/" + view.points();
        addLabel(pointsElement, points, SkillTreePanelStyle.OP_TEXT_COLOR, Alignment.RMID, POINTS_WIDTH, 0f, POINTS_LINE_HEIGHT);
        rowPanel.addUIElement(pointsElement).inTL(gridWidth - POINTS_WIDTH, (rowHeight - POINTS_LINE_HEIGHT) / 2f);
    }

    private String schematicStateText(SchematicEntry entry) {
        RefitSchematic schematic = entry.schematic();
        HullUpgradeRules.UpgradeBlock block = entry.block();
        if (block == null) {
            return Translation.msg("framework.panel.upgradeReady").arg("points", schematic.points()).text();
        }
        return switch (block) {
            case STAR_CHART_PROGRESS -> Translation.text("upgrade.tooltip.chartMode");
            case AT_MAX_POINTS -> Translation.msg("framework.panel.upgradeAtMax").arg("max", HullUpgradePoints.MOST_POINTS).text();
            case NOT_UPGRADABLE, AT_OR_ABOVE_TIER -> Translation.msg("framework.panel.upgradeAlready").arg("points", view.points()).text();
            case NONE_IN_STORAGE -> Translation.msg("framework.panel.upgradeNone").arg("level", HullUpgradesSetting.unlockLevel()).text();
        };
    }

    private CustomPanelAPI sectionCell(SectionEntry entry, float columnWidth, float tileWidth) {
        CustomPanelAPI cellPanel = Global.getSettings().createCustom(columnWidth, tileWidth + SECTION_NAME_HEIGHT + STATE_HEIGHT, null);
        IconTileCell.Hooks hooks = new IconTileCell.Hooks(() -> queue(() -> panelListener.toggleSection(entry.section())),
                tilePosition -> queue(() -> showSectionTooltip(entry, tilePosition)), () -> queue(() -> hideTooltipOf(entry)));
        cellPanel.addComponent(Global.getSettings().createCustom(tileWidth, tileWidth, new SectionTile(entry.section(), entry.state(),
                entry.itemIconPath(), hooks))).inTL((columnWidth - tileWidth) / 2f, 0f);
        TooltipMakerAPI labelElement = cellPanel.createUIElement(columnWidth, SECTION_NAME_HEIGHT + STATE_HEIGHT, false);
        boolean blocked = entry.state() != SectionState.UNLOCKED;
        addLabel(labelElement, entry.section().displayName(), blocked ? BLOCKED_TEXT_COLOR : SectionFlair.colorOf(entry.section()),
                Alignment.MID, columnWidth, 0f, SECTION_NAME_HEIGHT);
        String stateText = stateText(entry.state());
        if (stateText != null) {
            addLabel(labelElement, stateText, stateColor(entry.state()), Alignment.MID, columnWidth, SECTION_NAME_HEIGHT, STATE_HEIGHT);
        }
        cellPanel.addUIElement(labelElement).inTL(0f, tileWidth);
        return cellPanel;
    }

    private static String stateText(SectionState state) {
        return switch (state) {
            case UNLOCKED -> Translation.text("framework.socketState.unlocked");
            case AVAILABLE -> Translation.text("framework.socketState.available");
            case RESTRICTED, NO_POINTS -> null;
        };
    }

    private static Color stateColor(SectionState state) {
        return state == SectionState.UNLOCKED ? Misc.getHighlightColor() : Misc.getPositiveHighlightColor();
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

    private void showSchematicTooltip(SchematicEntry entry, PositionAPI tilePosition) {
        if (isConfirmOpen()) {
            return;
        }
        hoverTooltip.show(entry, TOOLTIP_WIDTH, (tooltip, expanded) -> HullUpgradeTooltip.write(tooltip, entry.schematic(),
                () -> List.of(StyledText.of(schematicStateText(entry)))), tilePosition);
    }

    private void showSectionTooltip(SectionEntry entry, PositionAPI tilePosition) {
        if (isConfirmOpen()) {
            return;
        }
        hoverTooltip.show(entry, TOOLTIP_WIDTH, (tooltip, expanded) -> I18n.forGameText(() -> {
            tooltip.setTitleOrbitronVeryLarge();
            tooltip.addTitle(entry.section().displayName(), SectionFlair.colorOf(entry.section()));
            VanillaText.addPara(tooltip, Translation.styled("framework.socketDescription." + entry.section().id()), PAD, Misc.getTextColor());
            if (entry.section().requirementTag() != null) {
                VanillaText.addPara(tooltip, HullUpgradeTooltip.sectionLine(entry.section()), LINE_PAD, Misc.getGrayColor());
            }
            String hintKey = switch (entry.state()) {
                case UNLOCKED -> "framework.socketHint.unlocked";
                case AVAILABLE -> "framework.socketHint.available";
                case RESTRICTED -> "framework.socketHint.restricted";
                case NO_POINTS -> view.chartProgress() == null ? "framework.socketHint.noPoints" : "framework.socketHint.noPointsCharts";
            };
            VanillaText.addPara(tooltip, Translation.styled(hintKey), PAD, Misc.getGrayColor());
        }), tilePosition);
    }
}
