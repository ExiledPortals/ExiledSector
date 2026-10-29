package exiledsector.ui.stats;

import exiledsector.i18n.StyledText;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.stats.ShipStatSheet.Breakdown;
import exiledsector.ui.stats.ShipStatSheet.Cell;
import exiledsector.ui.stats.ShipStatSheet.EffectRow;
import exiledsector.ui.stats.ShipStatSheet.Grid;
import exiledsector.ui.stats.ShipStatSheet.GridRow;
import exiledsector.ui.stats.ShipStatSheet.Section;
import exiledsector.ui.stats.ShipStatSheet.Trend;
import exiledsector.ui.stats.ShipStatSheet.ValueRow;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.TextLabel;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class StatSheetLayout {

    public record Region(float left, float top, float width, float height, Breakdown breakdown) {
    }

    private record Placed(TextLabel label, float x, float y) {
    }

    private record Bar(float x, float y, float width, float height, Color color, float alpha) {
    }

    private record Block(List<Placed> labels, List<Bar> bars, List<Region> regions, float height) {
    }

    private static final float HEADER_FONT_SIZE = 18f;
    private static final float ROW_FONT_SIZE = 15f;
    private static final float COLUMN_HEADER_FONT_SIZE = 12f;
    private static final float ROW_GAP = 3f;
    private static final float HEADER_GAP = 6f;
    private static final float HEADER_RULE_HEIGHT = 1f;
    private static final float HEADER_RULE_ALPHA = 0.5f;
    private static final float SECTION_GAP = 14f;
    private static final float GRID_GAP = 6f;
    private static final float COLUMN_GAP = 24f;
    private static final float CHANGE_WIDTH = 66f;
    private static final float VALUE_GAP = 10f;
    private static final float MAX_GRID_CELL_WIDTH = 84f;
    private static final float GRID_AREA_SHARE = 0.68f;
    private static final float BULLET_SIZE = 3f;
    private static final float BULLET_INDENT = 12f;
    private static final float MAX_EFFECT_HEIGHT = 400f;
    private static final float HOVER_FILL_ALPHA = 0.18f;

    static final Color LABEL_COLOR = new Color(0xCB, 0xF5, 0xFF);
    static final Color VALUE_COLOR = new Color(0xFF, 0xD2, 0x00);
    private static final Color HEADER_COLOR = new Color(0xCB, 0xF5, 0xFF);
    private static final Color UNCHANGED_COLOR = new Color(0x80, 0x80, 0x80);
    private static final Color COLUMN_HEADER_COLOR = new Color(0x9A, 0xB8, 0xC4);

    private final List<Placed> labels;
    private final List<Bar> bars;
    private final List<Region> regions;
    private final float width;
    private final float height;
    private boolean disposed;

    private StatSheetLayout(List<Placed> labels, List<Bar> bars, List<Region> regions, float width, float height) {
        this.labels = labels;
        this.bars = bars;
        this.regions = regions;
        this.width = width;
        this.height = height;
    }

    public static StatSheetLayout build(ShipStatSheet sheet, float width, int columnCount) {
        int columns = Math.max(1, columnCount);
        float columnWidth = (width - COLUMN_GAP * (columns - 1)) / columns;
        float[] columnHeights = new float[columns];
        List<Placed> labels = new ArrayList<>();
        List<Bar> bars = new ArrayList<>();
        List<Region> regions = new ArrayList<>();
        for (Section section : sheet.sections()) {
            Block block = section(section, columnWidth);
            int column = shortest(columnHeights);
            float offsetX = column * (columnWidth + COLUMN_GAP);
            float offsetY = columnHeights[column] == 0f ? 0f : columnHeights[column] + SECTION_GAP;
            block.labels().forEach(placed -> labels.add(new Placed(placed.label(), placed.x() + offsetX, placed.y() + offsetY)));
            block.bars().forEach(bar -> bars.add(new Bar(bar.x() + offsetX, bar.y() + offsetY, bar.width(), bar.height(), bar.color(), bar.alpha())));
            block.regions().forEach(region -> regions.add(new Region(region.left() + offsetX, region.top() + offsetY, region.width(),
                    region.height(), region.breakdown())));
            columnHeights[column] = offsetY + block.height();
        }
        float tallest = 0f;
        for (float columnHeight : columnHeights) {
            tallest = Math.max(tallest, columnHeight);
        }
        return new StatSheetLayout(List.copyOf(labels), List.copyOf(bars), List.copyOf(regions), width, tallest);
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public void render(float left, float top, float alphaMult, Region hovered) {
        if (disposed) {
            return;
        }
        if (hovered != null) {
            GLDraw.fillQuad(left + hovered.left(), top - hovered.top() - hovered.height(), hovered.width(), hovered.height(),
                    SkillTreePanelStyle.GLOW_COLOR, HOVER_FILL_ALPHA * alphaMult);
        }
        for (Bar bar : bars) {
            GLDraw.fillQuad(left + bar.x(), top - bar.y() - bar.height(), bar.width(), bar.height(), bar.color(), bar.alpha() * alphaMult);
        }
        for (Placed placed : labels) {
            placed.label().setAlpha(alphaMult).draw(left + placed.x(), top - placed.y());
        }
    }

    public Region regionAt(float left, float top, float x, float y) {
        float localX = x - left;
        float localY = top - y;
        for (Region region : regions) {
            boolean inside = localX >= region.left() && localX <= region.left() + region.width()
                    && localY >= region.top() && localY <= region.top() + region.height();
            if (inside && region.breakdown() != null && !region.breakdown().isEmpty()) {
                return region;
            }
        }
        return null;
    }

    public void dispose() {
        disposed = true;
        labels.forEach(placed -> placed.label().dispose());
    }

    private static int shortest(float[] columnHeights) {
        int shortest = 0;
        for (int i = 1; i < columnHeights.length; i++) {
            if (columnHeights[i] < columnHeights[shortest]) {
                shortest = i;
            }
        }
        return shortest;
    }

    private static Block section(Section section, float columnWidth) {
        List<Placed> labels = new ArrayList<>();
        List<Bar> bars = new ArrayList<>();
        List<Region> regions = new ArrayList<>();
        TextLabel header = new TextLabel(HEADER_FONT_SIZE, HEADER_COLOR, LazyFont.TextAnchor.TOP_CENTER).set(section.group().displayName());
        labels.add(new Placed(header, columnWidth / 2f, 0f));
        float cursorY = HEADER_FONT_SIZE + HEADER_GAP / 2f;
        bars.add(new Bar(0f, cursorY, columnWidth, HEADER_RULE_HEIGHT, SkillTreePanelStyle.GLOW_COLOR, HEADER_RULE_ALPHA));
        cursorY += HEADER_GAP;
        float rowHeight = ROW_FONT_SIZE + ROW_GAP;
        for (ValueRow row : section.values()) {
            labels.add(new Placed(new TextLabel(ROW_FONT_SIZE, LABEL_COLOR).set(row.label()), 0f, cursorY));
            float valueRight = row.change().isEmpty() ? columnWidth : columnWidth - CHANGE_WIDTH - VALUE_GAP;
            labels.add(new Placed(rightAligned(row.value() + row.marker(), trendColor(row.trend())), valueRight, cursorY));
            if (!row.change().isEmpty()) {
                labels.add(new Placed(rightAligned(row.change(), trendColor(row.trend())), columnWidth, cursorY));
            }
            regions.add(new Region(0f, cursorY - ROW_GAP / 2f, columnWidth, rowHeight, row.breakdown()));
            cursorY += rowHeight;
        }
        for (Grid grid : section.grids()) {
            cursorY += GRID_GAP;
            cursorY = grid(grid, columnWidth, cursorY, labels, regions);
        }
        if (!section.effects().isEmpty()) {
            cursorY += section.values().isEmpty() && section.grids().isEmpty() ? 0f : GRID_GAP;
            for (EffectRow row : section.effects()) {
                StyledText text = row.marker().isEmpty() ? row.line().display() : row.line().display().append(" " + row.marker());
                TextLabel label = new TextLabel(ROW_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_BODY_COLOR)
                        .setParagraphs(List.of(text), columnWidth - BULLET_INDENT, MAX_EFFECT_HEIGHT, SkillTreePanelStyle::standardHighlightColor);
                float labelHeight = Math.max(ROW_FONT_SIZE, label.height());
                bars.add(new Bar(BULLET_INDENT / 2f - BULLET_SIZE / 2f, cursorY + ROW_FONT_SIZE / 2f - BULLET_SIZE / 2f, BULLET_SIZE, BULLET_SIZE,
                        LABEL_COLOR, 1f));
                labels.add(new Placed(label, BULLET_INDENT, cursorY));
                regions.add(new Region(0f, cursorY - ROW_GAP / 2f, columnWidth, labelHeight + ROW_GAP, row.breakdown()));
                cursorY += labelHeight + ROW_GAP;
            }
        }
        return new Block(labels, bars, regions, cursorY);
    }

    private static float grid(Grid grid, float columnWidth, float topY, List<Placed> labels, List<Region> regions) {
        int columns = grid.columns().size();
        float cellWidth = Math.min(MAX_GRID_CELL_WIDTH, columnWidth * GRID_AREA_SHARE / Math.max(1, columns));
        float labelWidth = columnWidth - cellWidth * columns;
        float cursorY = topY;
        for (int column = 0; column < columns; column++) {
            float cellRight = labelWidth + cellWidth * (column + 1);
            labels.add(new Placed(new TextLabel(COLUMN_HEADER_FONT_SIZE, COLUMN_HEADER_COLOR, LazyFont.TextAnchor.TOP_RIGHT)
                    .set(grid.columns().get(column)), cellRight, cursorY));
        }
        cursorY += COLUMN_HEADER_FONT_SIZE + ROW_GAP;
        float rowHeight = ROW_FONT_SIZE + ROW_GAP;
        for (GridRow row : grid.rows()) {
            labels.add(new Placed(new TextLabel(ROW_FONT_SIZE, LABEL_COLOR).set(row.label()), 0f, cursorY));
            for (int column = 0; column < row.cells().size(); column++) {
                Cell cell = row.cells().get(column);
                float cellLeft = labelWidth + cellWidth * column;
                if (!cell.text().isEmpty()) {
                    labels.add(new Placed(rightAligned(cell.text() + cell.marker(), trendColor(cell.trend())), cellLeft + cellWidth, cursorY));
                }
                regions.add(new Region(cellLeft, cursorY - ROW_GAP / 2f, cellWidth, rowHeight, cell.breakdown()));
            }
            cursorY += rowHeight;
        }
        return cursorY;
    }

    private static TextLabel rightAligned(String text, Color color) {
        return new TextLabel(ROW_FONT_SIZE, color, LazyFont.TextAnchor.TOP_RIGHT).set(text);
    }

    static Color trendColor(Trend trend) {
        return switch (trend) {
            case BETTER -> SkillTreePanelStyle.POSITIVE_STAT_COLOR;
            case WORSE -> SkillTreePanelStyle.NEGATIVE_STAT_COLOR;
            case UNCHANGED -> UNCHANGED_COLOR;
            case NEUTRAL -> VALUE_COLOR;
        };
    }
}
