package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.function.Function;

final class ItemGrid {

    static final float SCROLLBAR_ROOM = 14f;
    static final float MIN_CELL_SIZE = 72f;
    private static final float COLUMN_FIT_SLACK = 0.5f;

    private ItemGrid() {
    }

    static int columnsFor(float panelWidth) {
        float gridWidth = panelWidth - HoloPanel.PAD * 2f - SCROLLBAR_ROOM;
        return Math.max(1, (int) ((gridWidth + HoloPanel.GAP + COLUMN_FIT_SLACK) / (MIN_CELL_SIZE + HoloPanel.GAP)));
    }

    static float cellSize(float gridWidth, int columns) {
        return (gridWidth - SCROLLBAR_ROOM - (columns - 1) * HoloPanel.GAP) / columns;
    }

    static <T> TooltipMakerAPI build(CustomPanelAPI root, float gridWidth, float gridHeight, int columns, List<T> items,
                                     Function<T, CustomUIPanelPlugin> cellFor, String emptyText) {
        float cellSize = cellSize(gridWidth, columns);
        TooltipMakerAPI element = root.createUIElement(gridWidth, gridHeight, true);
        if (items.isEmpty()) {
            VanillaText.plainPara(element, emptyText, 0f, Misc.getGrayColor());
        }
        for (int start = 0; start < items.size(); start += columns) {
            CustomPanelAPI rowPanel = Global.getSettings().createCustom(gridWidth - SCROLLBAR_ROOM, cellSize, null);
            for (int i = start; i < Math.min(items.size(), start + columns); i++) {
                CustomPanelAPI cellPanel = Global.getSettings().createCustom(cellSize, cellSize, cellFor.apply(items.get(i)));
                rowPanel.addComponent(cellPanel).inTL((i - start) * (cellSize + HoloPanel.GAP), 0f);
            }
            element.addCustom(rowPanel, start == 0 ? 0f : HoloPanel.GAP);
        }
        return element;
    }
}
