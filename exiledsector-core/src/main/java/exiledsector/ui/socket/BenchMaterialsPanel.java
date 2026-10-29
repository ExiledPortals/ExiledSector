package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconLook;

import java.awt.Color;
import java.util.List;
import java.util.function.BooleanSupplier;

final class BenchMaterialsPanel extends HoloPanel {

    record Tile(List<String> iconPaths, CargoIconLook look, BooleanSupplier lit, boolean toggle, TileInput.Handler handler, int count) {
    }

    record Section(String title, List<Tile> tiles) {
    }

    private static final float SECTION_HEIGHT = 22f;
    private static final float COUNT_HEIGHT = 16f;
    private static final float COUNT_INSET = 4f;

    private final Runnable scrolled;
    private List<Section> sections;
    private UIComponentAPI headerElement;
    private TooltipMakerAPI gridElement;

    private BenchMaterialsPanel(CustomPanelAPI hostPanel, List<Section> sections, Runnable scrolled) {
        super(hostPanel);
        this.sections = List.copyOf(sections);
        this.scrolled = scrolled;
    }

    static BenchMaterialsPanel open(CustomPanelAPI hostPanel, BenchLayout.Rect area, List<Section> sections, Runnable scrolled) {
        BenchMaterialsPanel materialsPanel = new BenchMaterialsPanel(hostPanel, sections, scrolled);
        float height = Math.min(area.height(), preferredHeight(area.width(), sections));
        materialsPanel.attach(area.left(), area.top(), area.width(), height, false);
        return materialsPanel;
    }

    static float preferredHeight(float width, List<Section> sections) {
        int columns = ItemGrid.columnsFor(width);
        float cellSize = ItemGrid.cellSize(width - PAD * 2f, columns);
        float gridHeight = 0f;
        for (Section section : sections) {
            int rows = Math.max(1, (section.tiles().size() + columns - 1) / columns);
            gridHeight += SECTION_HEIGHT + rows * (cellSize + GAP);
        }
        return PAD + HEADER_HEIGHT + GAP + gridHeight + PAD;
    }

    void show(List<Section> shown) {
        queue(() -> {
            sections = List.copyOf(shown);
            rebuildHeader();
            rebuildGrid();
        });
    }

    @Override
    protected void onClose() {
        scrolled.run();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{placedComponent(gridElement)};
    }

    @Override
    protected void onScrollInside() {
        scrolled.run();
    }

    @Override
    protected void build() {
        rebuildHeader();
        rebuildGrid();
    }

    @Override
    protected void rebuildHeader() {
        TooltipMakerAPI element = startHeader("ui.workbench.materials.title", null, null);
        addPartsCount(element, 0f);
        headerElement = finishHeader(element, headerElement);
        refreshOpacity();
    }

    private float gridTop() {
        return PAD + HEADER_HEIGHT + GAP;
    }

    private void rebuildGrid() {
        int columns = ItemGrid.columnsFor(panelWidth);
        float cellSize = ItemGrid.cellSize(innerWidth(), columns);
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), panelHeight - gridTop() - PAD, true);
        for (Section section : sections) {
            element.setParaFont(Fonts.ORBITRON_12);
            VanillaText.plainPara(element, section.title(), element.getHeightSoFar() > 0f ? GAP : 0f, Misc.getBasePlayerColor());
            element.setParaFontDefault();
            List<Tile> tiles = section.tiles();
            for (int start = 0; start < tiles.size(); start += columns) {
                element.addCustom(rowPanel(tiles.subList(start, Math.min(tiles.size(), start + columns)), cellSize), GAP);
            }
        }
        gridElement = replaceElement(placedComponent(gridElement), element, PAD, gridTop());
        refreshOpacity();
    }

    private CustomPanelAPI rowPanel(List<Tile> row, float cellSize) {
        CustomPanelAPI rowPanel = Global.getSettings().createCustom(innerWidth() - ItemGrid.SCROLLBAR_ROOM, cellSize, null);
        for (int index = 0; index < row.size(); index++) {
            Tile tile = row.get(index);
            float cellLeft = index * (cellSize + GAP);
            WorkbenchTile tilePlugin = new WorkbenchTile(tile.iconPaths(), tile.look(), tile.lit(), tile.toggle(), tile.handler());
            rowPanel.addComponent(Global.getSettings().createCustom(cellSize, cellSize, tilePlugin)).inTL(cellLeft, 0f);
            TooltipMakerAPI countElement = rowPanel.createUIElement(cellSize - COUNT_INSET * 2f, COUNT_HEIGHT, false);
            countElement.setParaFont(Fonts.ORBITRON_12);
            Color countColor = tile.count() > 0 ? Misc.getBrightPlayerColor() : Misc.getGrayColor();
            LabelAPI countLabel = VanillaText.plainPara(countElement, String.valueOf(tile.count()), 0f, countColor);
            countLabel.setAlignment(Alignment.RMID);
            countElement.setParaFontDefault();
            rowPanel.addUIElement(countElement).inTL(cellLeft + COUNT_INSET, cellSize - COUNT_HEIGHT - COUNT_INSET);
        }
        return rowPanel;
    }
}
