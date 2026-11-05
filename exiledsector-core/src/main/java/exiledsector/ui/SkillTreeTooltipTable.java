package exiledsector.ui;

import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.TextFont;
import exiledsector.ui.util.TextLabel;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class SkillTreeTooltipTable {

    private static final float HEADING_GAP = 6f;
    private static final float CELL_PADDING = 10f;
    private static final float BORDER_THICKNESS = 1f;
    private static final float HEADING_FILL_ALPHA = 0.45f;
    private static final float HEADER_FILL_ALPHA = 0.25f;

    private final TooltipText.Table tableText;
    private final TextLabel headingText;
    private final List<TextLabel> headerCells = new ArrayList<>();
    private final List<List<TextLabel>> bodyRows = new ArrayList<>();
    private final float[] columnWidths;

    private SkillTreeTooltipTable(TooltipText.Table tableText, TextLabel headingText, float[] columnWidths) {
        this.tableText = tableText;
        this.headingText = headingText;
        this.columnWidths = columnWidths;
    }

    public static SkillTreeTooltipTable measure(TooltipText tooltipText, TooltipTable table) {
        TooltipText.Table tableText = tooltipText.table();
        TextFont cellFont = tableText.cell();
        LazyFont font = cellFont.source().get();
        int columnCount = table.headers().size();
        float[] measuredWidths = new float[columnCount];
        for (int i = 0; i < columnCount; i++) {
            measuredWidths[i] = font.calcWidth(table.headers().get(i), cellFont.size());
            for (TooltipTable.Row row : table.rows()) {
                measuredWidths[i] = Math.max(measuredWidths[i], font.calcWidth(row.cells().get(i), cellFont.size()));
            }
            measuredWidths[i] += CELL_PADDING * 2f;
        }

        SkillTreeTooltipTable measured = new SkillTreeTooltipTable(tableText,
                centered(table.heading(), tableText.heading(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR), measuredWidths);
        for (String header : table.headers()) {
            measured.headerCells.add(centered(header, cellFont, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR));
        }
        for (TooltipTable.Row row : table.rows()) {
            Color color = row.highlighted() ? Misc.getHighlightColor() : Misc.getGrayColor();
            List<TextLabel> cells = new ArrayList<>();
            for (String cell : row.cells()) {
                cells.add(centered(cell, cellFont, color));
            }
            measured.bodyRows.add(cells);
        }
        return measured;
    }

    private static TextLabel centered(String text, TextFont font, Color color) {
        return new TextLabel(font, color, LazyFont.TextAnchor.TOP_CENTER).set(text);
    }

    public float width() {
        float totalWidth = 0f;
        for (float width : columnWidths) {
            totalWidth += width;
        }
        return totalWidth;
    }

    public float height() {
        return tableText.headingHeight() + HEADING_GAP + tableText.rowHeight() * (1 + bodyRows.size());
    }

    public void draw(float x, float topY, float tableWidth, Color accent, float alphaMult) {
        float headingHeight = tableText.headingHeight();
        float rowHeight = tableText.rowHeight();
        GLDraw.fillQuad(x, topY - headingHeight, tableWidth, headingHeight, accent, HEADING_FILL_ALPHA * alphaMult);
        headingText.setAlpha(alphaMult).draw(x + tableWidth / 2f, topY - (headingHeight - tableText.heading().size()) / 2f);

        float[] stretchedWidths = stretched(tableWidth);
        float tableTop = topY - headingHeight - HEADING_GAP;
        float tableHeight = rowHeight * (1 + bodyRows.size());
        GLDraw.fillQuad(x, tableTop - rowHeight, tableWidth, rowHeight, accent, HEADER_FILL_ALPHA * alphaMult);
        GLDraw.strokeQuad(x, tableTop - tableHeight, tableWidth, tableHeight, accent, BORDER_THICKNESS, alphaMult);

        drawRow(headerCells, x, tableTop, stretchedWidths, alphaMult);
        for (int i = 0; i < bodyRows.size(); i++) {
            drawRow(bodyRows.get(i), x, tableTop - rowHeight * (i + 1), stretchedWidths, alphaMult);
        }
    }

    private void drawRow(List<TextLabel> cells, float x, float rowTop, float[] widths, float alphaMult) {
        float textTop = rowTop - (tableText.rowHeight() - tableText.cell().size()) / 2f;
        float cellLeft = x;
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).setAlpha(alphaMult).draw(cellLeft + widths[i] / 2f, textTop);
            cellLeft += widths[i];
        }
    }

    private float[] stretched(float tableWidth) {
        float extra = Math.max(0f, tableWidth - width()) / columnWidths.length;
        float[] stretchedWidths = new float[columnWidths.length];
        for (int i = 0; i < columnWidths.length; i++) {
            stretchedWidths[i] = columnWidths[i] + extra;
        }
        return stretchedWidths;
    }
}
