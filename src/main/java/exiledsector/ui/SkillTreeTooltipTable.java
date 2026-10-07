package exiledsector.ui;

import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.ReusableText;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class SkillTreeTooltipTable {

    private static final float FONT_SIZE = 16f;
    private static final float HEADING_FONT_SIZE = 18f;
    private static final float HEADING_HEIGHT = 24f;
    private static final float HEADING_GAP = 6f;
    private static final float ROW_HEIGHT = 22f;
    private static final float CELL_PADDING = 10f;
    private static final float BORDER_THICKNESS = 1f;
    private static final float HEADING_FILL_ALPHA = 0.45f;
    private static final float HEADER_FILL_ALPHA = 0.25f;

    private final ReusableText headingText;
    private final List<ReusableText> headerCells = new ArrayList<>();
    private final List<List<ReusableText>> bodyRows = new ArrayList<>();
    private final float[] columnWidths;

    private SkillTreeTooltipTable(ReusableText headingText, float[] columnWidths) {
        this.headingText = headingText;
        this.columnWidths = columnWidths;
    }

    public static SkillTreeTooltipTable measure(LazyFont font, TooltipTable table) {
        int columnCount = table.headers().size();
        float[] measuredWidths = new float[columnCount];
        for (int i = 0; i < columnCount; i++) {
            measuredWidths[i] = font.calcWidth(table.headers().get(i), FONT_SIZE);
            for (TooltipTable.Row row : table.rows()) {
                measuredWidths[i] = Math.max(measuredWidths[i], font.calcWidth(row.cells().get(i), FONT_SIZE));
            }
            measuredWidths[i] += CELL_PADDING * 2f;
        }

        SkillTreeTooltipTable measured = new SkillTreeTooltipTable(
                centered(table.heading(), HEADING_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR), measuredWidths);
        for (String header : table.headers()) {
            measured.headerCells.add(centered(header, FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR));
        }
        for (TooltipTable.Row row : table.rows()) {
            Color color = row.highlighted() ? Misc.getHighlightColor() : Misc.getGrayColor();
            List<ReusableText> cells = new ArrayList<>();
            for (String cell : row.cells()) {
                cells.add(centered(cell, FONT_SIZE, color));
            }
            measured.bodyRows.add(cells);
        }
        return measured;
    }

    private static ReusableText centered(String text, float size, Color color) {
        return new ReusableText(size, color, LazyFont.TextAnchor.TOP_CENTER).set(text);
    }

    public float width() {
        float totalWidth = 0f;
        for (float width : columnWidths) {
            totalWidth += width;
        }
        return totalWidth;
    }

    public float height() {
        return HEADING_HEIGHT + HEADING_GAP + ROW_HEIGHT * (1 + bodyRows.size());
    }

    public void draw(float x, float topY, float tableWidth, Color accent, float alphaMult) {
        GLDraw.fillQuad(x, topY - HEADING_HEIGHT, tableWidth, HEADING_HEIGHT, accent, HEADING_FILL_ALPHA * alphaMult);
        headingText.draw(x + tableWidth / 2f, topY - (HEADING_HEIGHT - HEADING_FONT_SIZE) / 2f);

        float[] stretchedWidths = stretched(tableWidth);
        float tableTop = topY - HEADING_HEIGHT - HEADING_GAP;
        float tableHeight = ROW_HEIGHT * (1 + bodyRows.size());
        GLDraw.fillQuad(x, tableTop - ROW_HEIGHT, tableWidth, ROW_HEIGHT, accent, HEADER_FILL_ALPHA * alphaMult);
        GLDraw.strokeQuad(x, tableTop - tableHeight, tableWidth, tableHeight, accent, BORDER_THICKNESS, alphaMult);

        drawRow(headerCells, x, tableTop, stretchedWidths);
        for (int i = 0; i < bodyRows.size(); i++) {
            drawRow(bodyRows.get(i), x, tableTop - ROW_HEIGHT * (i + 1), stretchedWidths);
        }
    }

    private static void drawRow(List<ReusableText> cells, float x, float rowTop, float[] widths) {
        float textTop = rowTop - (ROW_HEIGHT - FONT_SIZE) / 2f;
        float cellLeft = x;
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).draw(cellLeft + widths[i] / 2f, textTop);
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
