package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.VanillaText;

import java.awt.Color;
import java.util.List;

final class SectionedTileGrid {

    record Tile(CustomUIPanelPlugin plugin, int count) {
    }

    record Section(String title, Color titleColor, List<Tile> tiles) {
    }

    private static final float SECTION_HEIGHT = 22f;
    private static final float COUNT_HEIGHT = 16f;
    private static final float COUNT_INSET = 4f;

    private SectionedTileGrid() {
    }

    static float height(float panelWidth, List<Integer> sectionSizes) {
        int columns = ItemGrid.columnsFor(panelWidth);
        float cellSize = ItemGrid.cellSize(panelWidth - HoloPanel.PAD * 2f, columns);
        float gridHeight = 0f;
        for (int sectionSize : sectionSizes) {
            int rows = Math.max(1, (sectionSize + columns - 1) / columns);
            gridHeight += SECTION_HEIGHT + rows * (cellSize + HoloPanel.GAP);
        }
        return gridHeight;
    }

    static TooltipMakerAPI build(CustomPanelAPI panelRoot, float panelWidth, float gridHeight, List<Section> sections) {
        float gridWidth = panelWidth - HoloPanel.PAD * 2f;
        int columns = ItemGrid.columnsFor(panelWidth);
        float cellSize = ItemGrid.cellSize(gridWidth, columns);
        TooltipMakerAPI element = panelRoot.createUIElement(gridWidth, gridHeight, true);
        for (Section section : sections) {
            element.setParaFont(Fonts.ORBITRON_12);
            VanillaText.plainPara(element, section.title(), element.getHeightSoFar() > 0f ? HoloPanel.GAP : 0f, section.titleColor());
            element.setParaFontDefault();
            List<Tile> tiles = section.tiles();
            for (int start = 0; start < tiles.size(); start += columns) {
                element.addCustom(rowPanel(tiles.subList(start, Math.min(tiles.size(), start + columns)), gridWidth, cellSize), HoloPanel.GAP);
            }
        }
        return element;
    }

    private static CustomPanelAPI rowPanel(List<Tile> row, float gridWidth, float cellSize) {
        CustomPanelAPI rowPanel = Global.getSettings().createCustom(gridWidth - ItemGrid.SCROLLBAR_ROOM, cellSize, null);
        for (int index = 0; index < row.size(); index++) {
            Tile tile = row.get(index);
            float cellLeft = index * (cellSize + HoloPanel.GAP);
            rowPanel.addComponent(Global.getSettings().createCustom(cellSize, cellSize, tile.plugin())).inTL(cellLeft, 0f);
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
