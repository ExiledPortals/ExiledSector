package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.ui.TooltipTextSize;
import exiledsector.ui.VanillaText;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

final class BenchTrayList {

    record FilterBar(TooltipMakerAPI element, TextFieldAPI field) {
    }

    private record PlacedRow(CustomPanelAPI rowPanel, float rowHeight) {
    }

    static final float FILTER_HEIGHT = 26f;
    static final float MIN_COLUMN_WIDTH = 110f;
    private static final float FILTER_LABEL_WIDTH = 60f;
    private static final float ICON_SIZE = 32f;
    private static final float ROW_INSET = 6f;
    private static final float ICON_GAP = 4f;
    private static final float COST_WIDTH = 64f;
    private static final float BADGE_HEIGHT = 16f;
    private static final float ROW_MIN_HEIGHT = ICON_SIZE + ROW_INSET * 2f;
    private static final BenchTrayRow.Layout ROW_LAYOUT = new BenchTrayRow.Layout(ICON_SIZE, ROW_INSET, ICON_GAP);

    private BenchTrayList() {
    }

    static FilterBar filterBar(CustomPanelAPI panelRoot, float listWidth, String filterText) {
        TooltipMakerAPI element = panelRoot.createUIElement(listWidth, FILTER_HEIGHT, false);
        VanillaText.plainPara(element, Translation.text("ui.craftingBench.tray.filter"), 0f, Misc.getGrayColor())
                .getPosition().inTL(0f, HoloPanel.LINE_PAD * 2f);
        TextFieldAPI field = element.addTextField(listWidth - FILTER_LABEL_WIDTH, FILTER_HEIGHT, Fonts.DEFAULT_SMALL, 0f);
        field.getPosition().inTL(FILTER_LABEL_WIDTH, 0f);
        field.setText(filterText);
        return new FilterBar(element, field);
    }

    static float columnWidth(float listWidth, int columnCount) {
        return (listWidth - ItemGrid.SCROLLBAR_ROOM - HoloPanel.GAP * (columnCount - 1)) / columnCount;
    }

    static TooltipMakerAPI rows(CustomPanelAPI panelRoot, float listWidth, float listHeight, BenchTray tray, BenchTrayContext context,
                                Function<BenchTrayEntry, TileInput.Handler> handlerFor, String filterText) {
        TooltipMakerAPI element = panelRoot.createUIElement(listWidth, listHeight, true);
        element.setParaFont(Fonts.ORBITRON_12);
        VanillaText.plainPara(element, Translation.text(tray.titleKey()), 0f, Misc.getBasePlayerColor());
        element.setParaFontDefault();
        List<String> words = words(filterText);
        List<BenchTray.Column> columns = tray.columns(context, entry -> matches(entry, context, words));
        if (columns.stream().allMatch(column -> column.entries().isEmpty())) {
            String emptyKey = tray.entries(context).isEmpty() ? tray.emptyKey() : "ui.craftingBench.tray.noMatch";
            VanillaText.plainPara(element, Translation.text(emptyKey), HoloPanel.GAP, Misc.getGrayColor());
            return element;
        }
        if (columns.size() == 1) {
            float rowWidth = columnWidth(listWidth, 1);
            for (BenchTrayEntry entry : columns.get(0).entries()) {
                element.addCustom(row(entry, context, rowWidth, false, handlerFor.apply(entry)).rowPanel(), HoloPanel.GAP);
            }
            return element;
        }
        element.addCustom(columnsPanel(columns, context, listWidth, handlerFor), HoloPanel.GAP);
        return element;
    }

    static float tileColumnsWidth(int columnCount, float tileSize) {
        return columnCount * tileSize + (columnCount - 1) * HoloPanel.GAP + ItemGrid.SCROLLBAR_ROOM;
    }

    static TooltipMakerAPI tiles(CustomPanelAPI panelRoot, float listWidth, float listHeight, BenchTray tray, BenchTrayContext context,
                                 Function<BenchTrayEntry, TileInput.Handler> handlerFor, float tileSize) {
        TooltipMakerAPI element = panelRoot.createUIElement(listWidth, listHeight, true);
        element.setParaFont(Fonts.ORBITRON_12);
        VanillaText.plainPara(element, Translation.text(tray.titleKey()), 0f, Misc.getBasePlayerColor());
        element.setParaFontDefault();
        List<BenchTray.Column> columns = tray.columns(context, entry -> true);
        if (columns.stream().allMatch(column -> column.entries().isEmpty())) {
            VanillaText.plainPara(element, Translation.text(tray.emptyKey()), HoloPanel.GAP, Misc.getGrayColor());
            return element;
        }
        float columnsWidth = listWidth - ItemGrid.SCROLLBAR_ROOM;
        CustomPanelAPI columnsPanel = Global.getSettings().createCustom(columnsWidth, 0f, null);
        float tallestColumn = 0f;
        for (int index = 0; index < columns.size(); index++) {
            BenchTray.Column column = columns.get(index);
            float columnLeft = index * (tileSize + HoloPanel.GAP);
            float cursorY = column.headingKey() == null ? 0f : addHeading(columnsPanel, column.headingKey(), columnLeft, tileSize);
            for (BenchTrayEntry entry : column.entries()) {
                addTile(columnsPanel, entry, context, handlerFor.apply(entry), columnLeft, cursorY, tileSize);
                cursorY += tileSize + HoloPanel.GAP;
            }
            tallestColumn = Math.max(tallestColumn, cursorY - HoloPanel.GAP);
        }
        columnsPanel.getPosition().setSize(columnsWidth, tallestColumn);
        element.addCustom(columnsPanel, HoloPanel.GAP);
        return element;
    }

    private static float addHeading(CustomPanelAPI columnsPanel, String headingKey, float columnLeft, float columnWidth) {
        TooltipMakerAPI heading = columnsPanel.createUIElement(columnWidth, 0f, false);
        LabelAPI headingLabel = VanillaText.plainPara(heading, Translation.text(headingKey), 0f, Misc.getBasePlayerColor());
        headingLabel.setAlignment(Alignment.MID);
        float headingHeight = heading.getHeightSoFar();
        heading.getPosition().setSize(columnWidth, headingHeight);
        columnsPanel.addUIElement(heading).inTL(columnLeft, 0f);
        return headingHeight + HoloPanel.GAP;
    }

    private static void addTile(CustomPanelAPI columnsPanel, BenchTrayEntry entry, BenchTrayContext context, TileInput.Handler tileHandler,
                                float tileLeft, float tileTop, float tileSize) {
        boolean available = entry.available(context);
        CraftingBenchTile tile = CraftingBenchTile.entry(BenchTrayEntry.shownIcons(entry.iconPaths()), entry.look(), () -> available,
                () -> entry.lit(context), tileHandler);
        columnsPanel.addComponent(Global.getSettings().createCustom(tileSize, tileSize, tile)).inTL(tileLeft, tileTop);
        Color badgeColor = available ? Misc.getBrightPlayerColor() : Misc.getGrayColor();
        addBadge(columnsPanel, entry.badge(context), tileLeft + ROW_INSET, tileTop + tileSize - BADGE_HEIGHT - ROW_INSET, tileSize - ROW_INSET * 2f,
                Alignment.RMID, badgeColor);
    }

    static boolean matches(BenchTrayEntry entry, BenchTrayContext context, List<String> words) {
        String searched = entry.searchText(context).toLowerCase(Locale.ROOT);
        return words.stream().allMatch(searched::contains);
    }

    static List<String> words(String filterText) {
        if (filterText == null || filterText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(filterText.toLowerCase(Locale.ROOT).trim().split("\\s+")).toList();
    }

    private static CustomPanelAPI columnsPanel(List<BenchTray.Column> columns, BenchTrayContext context, float listWidth,
                                               Function<BenchTrayEntry, TileInput.Handler> handlerFor) {
        float columnWidth = columnWidth(listWidth, columns.size());
        CustomPanelAPI columnsPanel = Global.getSettings().createCustom(listWidth - ItemGrid.SCROLLBAR_ROOM, 0f, null);
        float tallestColumn = 0f;
        for (int index = 0; index < columns.size(); index++) {
            BenchTray.Column column = columns.get(index);
            float columnLeft = index * (columnWidth + HoloPanel.GAP);
            float cursorY = 0f;
            if (column.headingKey() != null) {
                TooltipMakerAPI heading = columnsPanel.createUIElement(columnWidth, 0f, false);
                LabelAPI headingLabel = VanillaText.plainPara(heading, Translation.text(column.headingKey()), 0f, Misc.getBasePlayerColor());
                headingLabel.setAlignment(Alignment.MID);
                float headingHeight = heading.getHeightSoFar();
                heading.getPosition().setSize(columnWidth, headingHeight);
                columnsPanel.addUIElement(heading).inTL(columnLeft, cursorY);
                cursorY += headingHeight + HoloPanel.GAP;
            }
            List<PlacedRow> placedRows = new ArrayList<>();
            for (BenchTrayEntry entry : column.entries()) {
                placedRows.add(row(entry, context, columnWidth, true, handlerFor.apply(entry)));
            }
            for (PlacedRow placed : placedRows) {
                columnsPanel.addComponent(placed.rowPanel()).inTL(columnLeft, cursorY);
                cursorY += placed.rowHeight() + HoloPanel.GAP;
            }
            tallestColumn = Math.max(tallestColumn, cursorY - HoloPanel.GAP);
        }
        columnsPanel.getPosition().setSize(listWidth - ItemGrid.SCROLLBAR_ROOM, tallestColumn);
        return columnsPanel;
    }

    private static PlacedRow row(BenchTrayEntry entry, BenchTrayContext context, float rowWidth, boolean compact, TileInput.Handler rowHandler) {
        boolean available = entry.available(context);
        List<BenchTrayEntry.RowIcon> icons = entry.rowIcons(context);
        CustomPanelAPI rowPanel = Global.getSettings().createCustom(rowWidth, ROW_MIN_HEIGHT,
                new BenchTrayRow(icons, available, () -> entry.lit(context), ROW_LAYOUT, rowHandler));
        float costLeft = ROW_INSET + ICON_SIZE + HoloPanel.GAP;
        float textLeft = compact ? costLeft : costLeft + COST_WIDTH + HoloPanel.GAP;
        float textWidth = rowWidth - textLeft - ROW_INSET;
        TooltipMakerAPI textElement = rowPanel.createUIElement(textWidth, 0f, false);
        TooltipTextSize.applyBodyFont(textElement);
        VanillaText.addPara(textElement, entry.rowText(context), 0f, Misc.getTextColor());
        float textHeight = textElement.getHeightSoFar();
        float stackHeight = BenchTrayRow.stackHeight(icons.size(), ICON_SIZE, ICON_GAP);
        float rowHeight = Math.max(ROW_MIN_HEIGHT, Math.max(stackHeight, textHeight) + ROW_INSET * 2f);
        rowPanel.getPosition().setSize(rowWidth, rowHeight);
        textElement.getPosition().setSize(textWidth, textHeight);
        rowPanel.addUIElement(textElement).inTL(textLeft, (rowHeight - textHeight) / 2f);
        float stackTop = (rowHeight - stackHeight) / 2f;
        for (int i = 0; i < icons.size(); i++) {
            String badge = icons.get(i).badge();
            float iconTop = stackTop + i * (ICON_SIZE + ICON_GAP);
            if (compact) {
                TooltipMakerAPI costElement = costBadge(rowPanel, badge, ICON_SIZE, Alignment.RMID);
                rowPanel.addUIElement(costElement).inTL(ROW_INSET, iconTop + ICON_SIZE - costElement.getPosition().getHeight());
            } else {
                TooltipMakerAPI costElement = costBadge(rowPanel, badge, COST_WIDTH, Alignment.LMID);
                rowPanel.addUIElement(costElement).inTL(costLeft, iconTop + (ICON_SIZE - costElement.getPosition().getHeight()) / 2f);
            }
        }
        return new PlacedRow(rowPanel, rowHeight);
    }

    private static TooltipMakerAPI costBadge(CustomPanelAPI rowPanel, String badge, float badgeWidth, Alignment alignment) {
        TooltipMakerAPI costElement = rowPanel.createUIElement(badgeWidth, 0f, false);
        TooltipTextSize.applyBodyFont(costElement);
        LabelAPI costLabel = VanillaText.plainPara(costElement, badge == null ? "" : badge, 0f, Misc.getTextColor());
        costLabel.setAlignment(alignment);
        float costHeight = costElement.getHeightSoFar();
        costLabel.getPosition().setSize(badgeWidth, costHeight);
        costElement.getPosition().setSize(badgeWidth, costHeight);
        return costElement;
    }

    private static void addBadge(CustomPanelAPI rowPanel, String badge, float badgeLeft, float badgeTop, float badgeWidth, Alignment alignment,
                                 Color badgeColor) {
        TooltipMakerAPI badgeElement = rowPanel.createUIElement(badgeWidth, BADGE_HEIGHT, false);
        badgeElement.setParaFont(Fonts.ORBITRON_12);
        LabelAPI badgeLabel = VanillaText.plainPara(badgeElement, badge == null ? "" : badge, 0f, badgeColor);
        badgeElement.setParaFontDefault();
        badgeLabel.setAlignment(alignment);
        badgeLabel.getPosition().setSize(badgeWidth, BADGE_HEIGHT);
        rowPanel.addUIElement(badgeElement).inTL(badgeLeft, badgeTop);
    }
}
