package exiledsector.ui;

import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.GLDraw;
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

    private final LazyFont.DrawableString heading;
    private final List<LazyFont.DrawableString> headers = new ArrayList<>();
    private final List<List<LazyFont.DrawableString>> rows = new ArrayList<>();
    private final float[] columnWidths;

    private SkillTreeTooltipTable(LazyFont.DrawableString heading, float[] columnWidths) {
        this.heading = heading;
        this.columnWidths = columnWidths;
    }

    public static SkillTreeTooltipTable measure(LazyFont font, TooltipTable table) {
        int columns = table.headers().size();
        float[] widths = new float[columns];
        for (int i = 0; i < columns; i++) {
            widths[i] = font.calcWidth(table.headers().get(i), FONT_SIZE);
            for (TooltipTable.Row row : table.rows()) {
                widths[i] = Math.max(widths[i], font.calcWidth(row.cells().get(i), FONT_SIZE));
            }
            widths[i] += CELL_PADDING * 2f;
        }

        SkillTreeTooltipTable measured = new SkillTreeTooltipTable(
                centered(font, table.heading(), HEADING_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR), widths);
        for (String header : table.headers()) {
            measured.headers.add(centered(font, header, FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR));
        }
        for (TooltipTable.Row row : table.rows()) {
            Color color = row.highlighted() ? Misc.getHighlightColor() : Misc.getGrayColor();
            List<LazyFont.DrawableString> cells = new ArrayList<>();
            for (String cell : row.cells()) {
                cells.add(centered(font, cell, FONT_SIZE, color));
            }
            measured.rows.add(cells);
        }
        return measured;
    }

    private static LazyFont.DrawableString centered(LazyFont font, String text, float size, Color color) {
        return SkillTreePanelStyle.buildSimpleText(font, text, size, color, LazyFont.TextAnchor.TOP_CENTER);
    }

    public float width() {
        float total = 0f;
        for (float width : columnWidths) {
            total += width;
        }
        return total;
    }

    public float height() {
        return HEADING_HEIGHT + HEADING_GAP + ROW_HEIGHT * (1 + rows.size());
    }

    public void draw(float x, float topY, float width, Color accent, float alphaMult) {
        GLDraw.fillQuad(x, topY - HEADING_HEIGHT, width, HEADING_HEIGHT, accent, HEADING_FILL_ALPHA * alphaMult);
        heading.draw(x + width / 2f, topY - (HEADING_HEIGHT - HEADING_FONT_SIZE) / 2f);

        float[] widths = stretched(width);
        float tableTop = topY - HEADING_HEIGHT - HEADING_GAP;
        float tableHeight = ROW_HEIGHT * (1 + rows.size());
        GLDraw.fillQuad(x, tableTop - ROW_HEIGHT, width, ROW_HEIGHT, accent, HEADER_FILL_ALPHA * alphaMult);
        GLDraw.strokeQuad(x, tableTop - tableHeight, width, tableHeight, accent, BORDER_THICKNESS, alphaMult);

        drawRow(headers, x, tableTop, widths);
        for (int i = 0; i < rows.size(); i++) {
            drawRow(rows.get(i), x, tableTop - ROW_HEIGHT * (i + 1), widths);
        }
    }

    private static void drawRow(List<LazyFont.DrawableString> cells, float x, float rowTop, float[] widths) {
        float textTop = rowTop - (ROW_HEIGHT - FONT_SIZE) / 2f;
        float cellLeft = x;
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).draw(cellLeft + widths[i] / 2f, textTop);
            cellLeft += widths[i];
        }
    }

    private float[] stretched(float width) {
        float extra = Math.max(0f, width - width()) / columnWidths.length;
        float[] widths = new float[columnWidths.length];
        for (int i = 0; i < columnWidths.length; i++) {
            widths[i] = columnWidths[i] + extra;
        }
        return widths;
    }
}
