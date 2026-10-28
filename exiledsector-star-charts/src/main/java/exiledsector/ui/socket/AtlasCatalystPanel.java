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
import exiledlib.i18n.Translation;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasCatalystUse;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.items.AtlasCatalystItems;
import exiledsector.items.Consumables;
import exiledsector.items.MaterialItems;
import exiledsector.items.UseVerdict;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.VanillaText;

import java.util.ArrayList;
import java.util.List;

public final class AtlasCatalystPanel extends HoloPanel {

    public interface Listener {

        void selected(AtlasCatalysts.Catalyst catalyst);

        void used(AtlasCell cell);

        void closed();

        void pressedInside();
    }

    public static final float PANEL_WIDTH = 380f;
    private static final float FAMILY_HEIGHT = 22f;
    private static final float NOTICE_HEIGHT = 40f;
    private static final float COUNT_HEIGHT = 16f;
    private static final float COUNT_INSET = 4f;
    private static final float TOOLTIP_WIDTH = 340f;
    private static final int MIN_GRID_ROWS = 2;
    private static final float CONFIRM_HEIGHT = 150f;

    private enum Control {CLOSE, CONFIRM_USE, CANCEL_USE}

    private final Listener catalystListener;
    private String selectedItemId;
    private String noticeText;
    private boolean noticeIsWarning;
    private AtlasCatalystUse pendingUse;
    private AtlasCell pendingCell;
    private String pendingSystem;
    private UIComponentAPI headerElement;
    private TooltipMakerAPI gridElement;
    private UIComponentAPI noticeElement;

    private AtlasCatalystPanel(CustomPanelAPI hostPanel, Listener catalystListener) {
        super(hostPanel);
        this.catalystListener = catalystListener;
    }

    public static AtlasCatalystPanel open(CustomPanelAPI hostPanel, float panelLeft, float panelTop, float maxHeight, Listener catalystListener) {
        AtlasCatalystPanel catalystPanel = new AtlasCatalystPanel(hostPanel, catalystListener);
        float height = Math.min(maxHeight, preferredHeight());
        SocketPanelPlacement placement = new SocketPanelPlacement(panelLeft, panelTop, PANEL_WIDTH, height, false);
        catalystPanel.attach(placement.panelLeft(), placement.panelTop(), placement.panelWidth(), placement.panelHeight(), placement.instantly());
        return catalystPanel;
    }

    static float preferredHeight() {
        int columns = ItemGrid.columnsFor(PANEL_WIDTH);
        float cellSize = ItemGrid.cellSize(PANEL_WIDTH - PAD * 2f, columns);
        float gridHeight = 0f;
        for (AtlasCatalysts.Family family : AtlasCatalysts.Family.values()) {
            long inFamily = AtlasCatalysts.all().stream().filter(catalyst -> catalyst.family() == family).count();
            int rows = (int) Math.max(1, (inFamily + columns - 1) / columns);
            gridHeight += FAMILY_HEIGHT + rows * (cellSize + GAP);
        }
        float minimum = MIN_GRID_ROWS * (cellSize + GAP) + FAMILY_HEIGHT;
        return PAD + HEADER_HEIGHT + GAP + Math.max(minimum, gridHeight) + NOTICE_HEIGHT + PAD;
    }

    public AtlasCatalysts.Catalyst selected() {
        return AtlasCatalysts.byItemId(selectedItemId);
    }

    public void refresh() {
        queue(() -> {
            if (selectedItemId != null && playerMaterials().count(selectedItemId) < 1) {
                selectedItemId = null;
                catalystListener.selected(null);
            }
            rebuildGrid();
        });
    }

    public boolean confirming() {
        return isConfirmOpen();
    }

    public void requestUse(AtlasCatalystUse use, AtlasCell cell, String systemName) {
        queue(() -> {
            if (isConfirmOpen() || !use.itemId().equals(selectedItemId)) {
                return;
            }
            pendingUse = use;
            pendingCell = cell;
            pendingSystem = systemName;
            openConfirm(CONFIRM_HEIGHT, this::writeConfirm, "catalyst.confirm.use", Control.CONFIRM_USE, "catalyst.confirm.cancel", Control.CANCEL_USE);
        });
    }

    public void showRefusal(String text) {
        queue(() -> showNotice(text, true));
    }

    public boolean escape() {
        if (isConfirmOpen()) {
            queue(this::cancelUse);
            return true;
        }
        if (selectedItemId == null) {
            return false;
        }
        queue(() -> select(null));
        return true;
    }

    public void hideTooltip() {
        queue(hoverTooltip::hide);
    }

    @Override
    protected void onClose() {
        hoverTooltip.hide();
        catalystListener.closed();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{placedComponent(gridElement), noticeElement};
    }

    @Override
    protected void onScrollInside() {
        queue(hoverTooltip::hide);
    }

    @Override
    protected void onPressInside() {
        catalystListener.pressedInside();
    }

    @Override
    protected void advanceOpen(float amount) {
        hoverTooltip.refreshIfExpansionChanged();
    }

    @Override
    public void buttonPressed(Object id) {
        if (id == Control.CLOSE) {
            queue(this::close);
        } else if (id == Control.CONFIRM_USE) {
            queue(this::confirmUse);
        } else if (id == Control.CANCEL_USE) {
            queue(this::cancelUse);
        }
    }

    private void writeConfirm(TooltipMakerAPI element) {
        AtlasCatalysts.Catalyst catalyst = pendingUse.catalyst();
        String anomaly = AtlasCatalystUse.anomalyName(catalyst.anomalyId());
        VanillaText.plainPara(element, Translation.msg("catalyst.confirm.title").arg("name", MaterialItems.displayName(catalyst.itemId()))
                .arg("system", pendingSystem).text(), 0f, AtlasCatalystItems.color(catalyst.family()));
        VanillaText.plainPara(element, Translation.msg("catalyst.confirm.body").arg("anomaly", anomaly).arg("system", pendingSystem).text(),
                LINE_PAD * 2f, Misc.getTextColor());
    }

    private void cancelUse() {
        closeConfirm();
        clearPending();
    }

    private void clearPending() {
        pendingUse = null;
        pendingCell = null;
        pendingSystem = null;
    }

    private void confirmUse() {
        AtlasCatalystUse use = pendingUse;
        AtlasCell cell = pendingCell;
        String systemName = pendingSystem;
        closeConfirm();
        clearPending();
        if (use == null) {
            return;
        }
        AtlasAnomalies.Insertion insertion = Consumables.use(use, cell, playerMaterials());
        if (insertion == null) {
            SkillTreeSounds.refused();
            UseVerdict verdict = use.check(cell);
            showNotice(verdict.allowed() ? Translation.text("catalyst.window.failed")
                    : verdict.text(MaterialItems.displayName(use.itemId()), systemName), true);
            return;
        }
        SkillTreeSounds.socketed();
        noticeText = Translation.msg("catalyst.window.used").arg("anomaly", AtlasCatalystUse.anomalyName(use.catalyst().anomalyId()))
                .arg("system", systemName).text();
        noticeIsWarning = false;
        if (playerMaterials().count(use.itemId()) < 1) {
            selectedItemId = null;
            catalystListener.selected(null);
        }
        rebuildGrid();
        catalystListener.used(cell);
    }

    private void showNotice(String text, boolean warning) {
        noticeText = text;
        noticeIsWarning = warning;
        rebuildNotice();
        refreshOpacity();
    }

    @Override
    protected void build() {
        rebuildHeader();
        rebuildGrid();
    }

    @Override
    protected void rebuildHeader() {
        headerElement = finishHeader(startHeader("catalyst.window.title", "catalyst.window.close", Control.CLOSE), headerElement);
        refreshOpacity();
    }

    private float gridTop() {
        return PAD + HEADER_HEIGHT + GAP;
    }

    private void rebuildGrid() {
        hoverTooltip.hide();
        List<AtlasCatalystItems.Carried> carried = AtlasCatalystItems.carried(playerMaterials());
        int columns = ItemGrid.columnsFor(panelWidth);
        float cellSize = ItemGrid.cellSize(innerWidth(), columns);
        float gridHeight = panelHeight - gridTop() - NOTICE_HEIGHT - PAD;
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), gridHeight, true);
        if (carried.isEmpty()) {
            VanillaText.plainPara(element, Translation.text("catalyst.window.empty"), 0f, Misc.getGrayColor());
        }
        for (AtlasCatalysts.Family family : AtlasCatalysts.Family.values()) {
            List<AtlasCatalystItems.Carried> inFamily = carried.stream().filter(entry -> entry.catalyst().family() == family).toList();
            if (inFamily.isEmpty()) {
                continue;
            }
            element.setParaFont(Fonts.ORBITRON_12);
            VanillaText.plainPara(element, AtlasCatalystItems.familyName(family), element.getHeightSoFar() > 0f ? GAP : 0f,
                    AtlasCatalystItems.color(family));
            element.setParaFontDefault();
            for (int start = 0; start < inFamily.size(); start += columns) {
                element.addCustom(rowPanel(inFamily.subList(start, Math.min(inFamily.size(), start + columns)), cellSize), GAP);
            }
        }
        gridElement = replaceElement(placedComponent(gridElement), element, PAD, gridTop());
        rebuildNotice();
        refreshOpacity();
    }

    private CustomPanelAPI rowPanel(List<AtlasCatalystItems.Carried> row, float cellSize) {
        CustomPanelAPI rowPanel = Global.getSettings().createCustom(innerWidth() - ItemGrid.SCROLLBAR_ROOM, cellSize, null);
        for (int index = 0; index < row.size(); index++) {
            AtlasCatalystItems.Carried entry = row.get(index);
            AtlasCatalysts.Catalyst catalyst = entry.catalyst();
            float cellLeft = index * (cellSize + GAP);
            CatalystTile tile = new CatalystTile(catalyst.iconPath(), AtlasCatalystItems.look(catalyst.family()),
                    () -> catalyst.itemId().equals(selectedItemId), tileHandler(entry));
            rowPanel.addComponent(Global.getSettings().createCustom(cellSize, cellSize, tile)).inTL(cellLeft, 0f);
            TooltipMakerAPI countElement = rowPanel.createUIElement(cellSize - COUNT_INSET * 2f, COUNT_HEIGHT, false);
            countElement.setParaFont(Fonts.ORBITRON_12);
            LabelAPI countLabel = VanillaText.plainPara(countElement, String.valueOf(entry.count()), 0f, Misc.getBrightPlayerColor());
            countLabel.setAlignment(Alignment.RMID);
            countElement.setParaFontDefault();
            rowPanel.addUIElement(countElement).inTL(cellLeft + COUNT_INSET, cellSize - COUNT_HEIGHT - COUNT_INSET);
        }
        return rowPanel;
    }

    private TileInput.Handler tileHandler(AtlasCatalystItems.Carried entry) {
        String itemId = entry.catalyst().itemId();
        return new TileInput.Handler() {
            @Override
            public void entered(PositionAPI tilePosition) {
                queue(() -> showTooltip(entry, tilePosition));
            }

            @Override
            public void left() {
                queue(() -> {
                    if (hoverTooltip.isShowing(itemId)) {
                        hoverTooltip.hide();
                    }
                });
            }

            @Override
            public boolean clicked() {
                queue(() -> select(itemId.equals(selectedItemId) ? null : entry.catalyst()));
                return true;
            }
        };
    }

    private void select(AtlasCatalysts.Catalyst catalyst) {
        selectedItemId = catalyst == null ? null : catalyst.itemId();
        noticeText = null;
        catalystListener.selected(catalyst);
        rebuildNotice();
        refreshOpacity();
    }

    private void showTooltip(AtlasCatalystItems.Carried entry, PositionAPI anchor) {
        if (isClosing()) {
            hoverTooltip.hide();
            return;
        }
        hoverTooltip.show(entry.catalyst().itemId(), TOOLTIP_WIDTH, (element, expanded) -> writeTooltip(element, entry), anchor,
                ItemHoverTooltip.Placement.BELOW, PAD);
    }

    private void writeTooltip(TooltipMakerAPI element, AtlasCatalystItems.Carried entry) {
        AtlasCatalysts.Catalyst catalyst = entry.catalyst();
        element.setParaFont(Fonts.ORBITRON_20AA);
        VanillaText.plainPara(element, MaterialItems.displayName(catalyst.itemId()), 0f, AtlasCatalystItems.color(catalyst.family()));
        element.setParaFontDefault();
        VanillaText.plainPara(element, Translation.text("catalyst.category"), LINE_PAD, Misc.getGrayColor());
        VanillaText.plainPara(element, AtlasCatalystItems.addsLine(catalyst), GAP, Misc.getTextColor());
        VanillaText.plainPara(element, Translation.text(AtlasCatalystItems.flavourKey(catalyst.family())), GAP, Misc.getGrayColor());
        VanillaText.plainPara(element, Translation.msg("catalyst.cell.inCargo").arg("count", entry.count()).text(), GAP, Misc.getGrayColor());
        String hintKey = catalyst.itemId().equals(selectedItemId) ? "catalyst.cell.picked" : "catalyst.cell.pick";
        VanillaText.plainPara(element, Translation.text(hintKey), GAP, Misc.getGrayColor());
    }

    private void rebuildNotice() {
        TooltipMakerAPI notice = panelRoot.createUIElement(innerWidth(), NOTICE_HEIGHT, false);
        AtlasCatalysts.Catalyst selected = selected();
        if (noticeText != null) {
            VanillaText.plainPara(notice, noticeText, 0f, noticeIsWarning ? Misc.getNegativeHighlightColor() : Misc.getPositiveHighlightColor());
        } else {
            String text = selected == null ? Translation.text("catalyst.window.hint")
                    : Translation.msg("catalyst.window.picked").arg("name", MaterialItems.displayName(selected.itemId())).text();
            VanillaText.plainPara(notice, text, 0f, selected == null ? Misc.getGrayColor() : Misc.getPositiveHighlightColor());
        }
        noticeElement = replaceElement(noticeElement, notice, PAD, panelHeight - PAD - NOTICE_HEIGHT);
    }
}
