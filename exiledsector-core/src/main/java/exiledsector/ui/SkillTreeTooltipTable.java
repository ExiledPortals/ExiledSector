package exiledsector.ui;

import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.TextLabel;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final TextLabel headingText;
    private final List<TextLabel> headerCells = new ArrayList<>();
    private final List<List<TextLabel>> bodyRows = new ArrayList<>();
    private final float[] columnWidths;

    private SkillTreeTooltipTable(TextLabel headingText, float[] columnWidths) {
        this.headingText = headingText;
        this.columnWidths = columnWidths;
    }

    public static SkillTreeTooltipTable measure(LazyFont font, TooltipTable table) {
        return build(table, contentWidths(font, table));
    }

    public static List<SkillTreeTooltipTable> measureAligned(LazyFont font, List<TooltipTable> tables) {
        float[] sharedWidths = new float[0];
        for (TooltipTable table : tables) {
            float[] widths = contentWidths(font, table);
            if (widths.length > sharedWidths.length) {
                sharedWidths = Arrays.copyOf(sharedWidths, widths.length);
            }
            for (int i = 0; i < widths.length; i++) {
                sharedWidths[i] = Math.max(sharedWidths[i], widths[i]);
            }
        }
        List<SkillTreeTooltipTable> measured = new ArrayList<>();
        for (TooltipTable table : tables) {
            measured.add(build(table, Arrays.copyOf(sharedWidths, table.columnCount())));
        }
        return measured;
    }

    private static float[] contentWidths(LazyFont font, TooltipTable table) {
        int columnCount = table.columnCount();
        float[] measuredWidths = new float[columnCount];
        for (int i = 0; i < columnCount; i++) {
            measuredWidths[i] = table.headers().isEmpty() ? 0f : font.calcWidth(table.headers().get(i), FONT_SIZE);
            for (TooltipTable.Row row : table.rows()) {
                measuredWidths[i] = Math.max(measuredWidths[i], font.calcWidth(row.cells().get(i), FONT_SIZE));
            }
            measuredWidths[i] += CELL_PADDING * 2f;
        }
        return measuredWidths;
    }

    private static SkillTreeTooltipTable build(TooltipTable table, float[] columnWidths) {
        SkillTreeTooltipTable measured = new SkillTreeTooltipTable(
                centered(table.heading(), HEADING_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR), columnWidths);
        for (String header : table.headers()) {
            measured.headerCells.add(centered(header, FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR));
        }
        for (TooltipTable.Row row : table.rows()) {
            Color color = color(row.tone());
            List<TextLabel> cells = new ArrayList<>();
            for (String cell : row.cells()) {
                cells.add(centered(cell, FONT_SIZE, color));
            }
            measured.bodyRows.add(cells);
        }
        return measured;
    }

    private static Color color(TooltipTable.Tone tone) {
        return switch (tone) {
            case PLAIN -> Misc.getGrayColor();
            case HIGHLIGHTED -> Misc.getHighlightColor();
            case TOTAL -> SkillTreePanelStyle.TOOLTIP_TITLE_COLOR;
            case DETAIL -> Misc.getDarkHighlightColor();
        };
    }

    private static TextLabel centered(String text, float size, Color color) {
        return new TextLabel(size, color, LazyFont.TextAnchor.TOP_CENTER).set(text);
    }

    public float width() {
        return Math.max(columnsWidth(), headingText.width() + CELL_PADDING * 2f);
    }

    private float columnsWidth() {
        float totalWidth = 0f;
        for (float width : columnWidths) {
            totalWidth += width;
        }
        return totalWidth;
    }

    public float height() {
        return HEADING_HEIGHT + HEADING_GAP + ROW_HEIGHT * rowCount();
    }

    private int rowCount() {
        return (headerCells.isEmpty() ? 0 : 1) + bodyRows.size();
    }

    public void draw(float x, float topY, float tableWidth, Color accent, float alphaMult) {
        GLDraw.fillQuad(x, topY - HEADING_HEIGHT, tableWidth, HEADING_HEIGHT, accent, HEADING_FILL_ALPHA * alphaMult);
        headingText.setAlpha(alphaMult).draw(x + tableWidth / 2f, topY - (HEADING_HEIGHT - HEADING_FONT_SIZE) / 2f);

        float[] stretchedWidths = stretched(tableWidth);
        float tableTop = topY - HEADING_HEIGHT - HEADING_GAP;
        float tableHeight = ROW_HEIGHT * rowCount();
        GLDraw.strokeQuad(x, tableTop - tableHeight, tableWidth, tableHeight, accent, BORDER_THICKNESS, alphaMult);

        float rowTop = tableTop;
        if (!headerCells.isEmpty()) {
            GLDraw.fillQuad(x, tableTop - ROW_HEIGHT, tableWidth, ROW_HEIGHT, accent, HEADER_FILL_ALPHA * alphaMult);
            drawRow(headerCells, x, rowTop, stretchedWidths, alphaMult);
            rowTop -= ROW_HEIGHT;
        }
        for (List<TextLabel> bodyRow : bodyRows) {
            drawRow(bodyRow, x, rowTop, stretchedWidths, alphaMult);
            rowTop -= ROW_HEIGHT;
        }
    }

    private static void drawRow(List<TextLabel> cells, float x, float rowTop, float[] widths, float alphaMult) {
        float textTop = rowTop - (ROW_HEIGHT - FONT_SIZE) / 2f;
        float cellLeft = x;
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).setAlpha(alphaMult).draw(cellLeft + widths[i] / 2f, textTop);
            cellLeft += widths[i];
        }
    }

    private float[] stretched(float tableWidth) {
        float extra = Math.max(0f, tableWidth - columnsWidth()) / columnWidths.length;
        float[] stretchedWidths = new float[columnWidths.length];
        for (int i = 0; i < columnWidths.length; i++) {
            stretchedWidths[i] = columnWidths[i] + extra;
        }
        return stretchedWidths;
    }
}
