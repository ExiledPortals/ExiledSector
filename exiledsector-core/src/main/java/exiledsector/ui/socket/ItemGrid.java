package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.function.Function;

final class ItemGrid {

    static final float SCROLLBAR_ROOM = 14f;
    static final float MIN_CELL_SIZE = 72f;
    private static final float COLUMN_FIT_SLACK = 0.5f;
    private static final float BADGE_INSET = 5f;
    private static final float BADGE_HEIGHT = 18f;

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
        return build(root, gridWidth, gridHeight, columns, items, cellFor, item -> null, emptyText);
    }

    static <T> TooltipMakerAPI build(CustomPanelAPI root, float gridWidth, float gridHeight, int columns, List<T> items,
                                     Function<T, CustomUIPanelPlugin> cellFor, Function<T, String> badgeFor, String emptyText) {
        float cellSize = cellSize(gridWidth, columns);
        TooltipMakerAPI element = root.createUIElement(gridWidth, gridHeight, true);
        if (items.isEmpty()) {
            VanillaText.plainPara(element, emptyText, 0f, Misc.getGrayColor());
        }
        for (int start = 0; start < items.size(); start += columns) {
            CustomPanelAPI rowPanel = Global.getSettings().createCustom(gridWidth - SCROLLBAR_ROOM, cellSize, null);
            for (int i = start; i < Math.min(items.size(), start + columns); i++) {
                float cellLeft = (i - start) * (cellSize + HoloPanel.GAP);
                CustomPanelAPI cellPanel = Global.getSettings().createCustom(cellSize, cellSize, cellFor.apply(items.get(i)));
                rowPanel.addComponent(cellPanel).inTL(cellLeft, 0f);
                String badge = badgeFor.apply(items.get(i));
                if (badge != null) {
                    addBadge(rowPanel, badge, cellLeft, cellSize);
                }
            }
            element.addCustom(rowPanel, start == 0 ? 0f : HoloPanel.GAP);
        }
        return element;
    }

    private static void addBadge(CustomPanelAPI rowPanel, String badge, float cellLeft, float cellSize) {
        float badgeWidth = cellSize - BADGE_INSET * 2f;
        TooltipMakerAPI badgeElement = rowPanel.createUIElement(badgeWidth, BADGE_HEIGHT, false);
        LabelAPI label = VanillaText.plainPara(badgeElement, badge, 0f, Misc.getHighlightColor());
        label.setAlignment(Alignment.RMID);
        label.getPosition().setSize(badgeWidth, BADGE_HEIGHT);
        rowPanel.addUIElement(badgeElement).inTL(cellLeft + BADGE_INSET, cellSize - BADGE_INSET - BADGE_HEIGHT);
    }
}
