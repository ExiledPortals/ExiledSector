package exiledsector.ui.slipstream;

import java.util.ArrayDeque;
import java.util.Arrays;

public final class CorridorGrid {

    static final float AREA_MARGIN_WIDTHS = 2f;
    static final int MAX_CELLS_PER_SIDE = 120;
    static final float CELLS_PER_CLEARANCE = 2f;
    static final float LINE_SAMPLES_PER_CELL = 3f;
    static final float CURVE_SAMPLES_PER_CELL = 2f;

    private final ViewWindow area;
    private final float clearance;
    private final float cellSize;
    private final int columns;
    private final int rows;
    private final boolean[] blocked;

    CorridorGrid(ViewWindow area, float clearance) {
        this.area = area;
        this.clearance = clearance;
        cellSize = Math.max(clearance / CELLS_PER_CLEARANCE, Math.max(area.width(), area.height()) / MAX_CELLS_PER_SIDE);
        columns = Math.max(1, (int) Math.ceil(area.width() / cellSize));
        rows = Math.max(1, (int) Math.ceil(area.height() / cellSize));
        blocked = new boolean[columns * rows];
    }

    public static CorridorGrid covering(ViewWindow view, float streamWidth) {
        return new CorridorGrid(view.expanded(streamWidth * AREA_MARGIN_WIDTHS), streamWidth * Slipstream.CLEARANCE_WIDTHS);
    }

    public boolean reaches(float minX, float minY, float maxX, float maxY) {
        return area.expanded(clearance).overlaps(minX, minY, maxX, maxY);
    }

    public void blockCircle(float x, float y, float radius) {
        float reach = radius + clearance;
        if (!reaches(x - radius, y - radius, x + radius, y + radius)) {
            return;
        }
        int firstColumn = Math.max(0, columnOf(x - reach));
        int lastColumn = Math.min(columns - 1, columnOf(x + reach));
        int firstRow = Math.max(0, rowOf(y - reach));
        int lastRow = Math.min(rows - 1, rowOf(y + reach));
        for (int row = firstRow; row <= lastRow; row++) {
            float cellBottom = area.minY() + row * cellSize;
            float nearestY = Math.max(cellBottom, Math.min(y, cellBottom + cellSize));
            for (int column = firstColumn; column <= lastColumn; column++) {
                float cellLeft = area.minX() + column * cellSize;
                float nearestX = Math.max(cellLeft, Math.min(x, cellLeft + cellSize));
                float dx = nearestX - x;
                float dy = nearestY - y;
                if (dx * dx + dy * dy <= reach * reach) {
                    blocked[row * columns + column] = true;
                }
            }
        }
    }

    public void blockLine(float fromX, float fromY, float toX, float toY, float radius) {
        blockCurve(fromX, fromY, (fromX + toX) / 2f, (fromY + toY) / 2f, toX, toY, radius);
    }

    public void blockCurve(float fromX, float fromY, float controlX, float controlY, float toX, float toY, float radius) {
        if (!reaches(Math.min(Math.min(fromX, toX), controlX) - radius, Math.min(Math.min(fromY, toY), controlY) - radius,
                Math.max(Math.max(fromX, toX), controlX) + radius, Math.max(Math.max(fromY, toY), controlY) + radius)) {
            return;
        }
        float netLength = (float) (Math.hypot(controlX - fromX, controlY - fromY) + Math.hypot(toX - controlX, toY - controlY));
        int steps = Math.max(1, (int) Math.ceil(netLength * CURVE_SAMPLES_PER_CELL / cellSize));
        float stepRadius = radius + netLength / steps / 2f;
        for (int step = 0; step <= steps; step++) {
            float t = step / (float) steps;
            float u = 1f - t;
            blockCircle(u * u * fromX + 2f * u * t * controlX + t * t * toX, u * u * fromY + 2f * u * t * controlY + t * t * toY, stepRadius);
        }
    }

    public boolean isFree(float x, float y) {
        int column = columnOf(x);
        int row = rowOf(y);
        return column >= 0 && column < columns && row >= 0 && row < rows && !blocked[row * columns + column];
    }

    boolean isFreeLine(float fromX, float fromY, float toX, float toY) {
        float length = (float) Math.hypot(toX - fromX, toY - fromY);
        int steps = Math.max(1, (int) Math.ceil(length * LINE_SAMPLES_PER_CELL / cellSize));
        for (int step = 0; step <= steps; step++) {
            float t = step / (float) steps;
            if (!isFree(fromX + (toX - fromX) * t, fromY + (toY - fromY) * t)) {
                return false;
            }
        }
        return true;
    }

    int[] cellsToBlocked() {
        int[] distance = new int[blocked.length];
        Arrays.fill(distance, Integer.MAX_VALUE);
        ArrayDeque<Integer> frontier = new ArrayDeque<>();
        for (int cell = 0; cell < blocked.length; cell++) {
            if (blocked[cell]) {
                distance[cell] = 0;
                frontier.add(cell);
            }
        }
        while (!frontier.isEmpty()) {
            int cell = frontier.poll();
            int column = cell % columns;
            int row = cell / columns;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int neighbourColumn = column + dx;
                    int neighbourRow = row + dy;
                    if (neighbourColumn < 0 || neighbourColumn >= columns || neighbourRow < 0 || neighbourRow >= rows) {
                        continue;
                    }
                    int neighbour = neighbourRow * columns + neighbourColumn;
                    if (distance[neighbour] > distance[cell] + 1) {
                        distance[neighbour] = distance[cell] + 1;
                        frontier.add(neighbour);
                    }
                }
            }
        }
        return distance;
    }

    boolean isBlocked(int column, int row) {
        return blocked[row * columns + column];
    }

    int columns() {
        return columns;
    }

    int rows() {
        return rows;
    }

    float cellSize() {
        return cellSize;
    }

    float centerX(int column) {
        return area.minX() + (column + 0.5f) * cellSize;
    }

    float centerY(int row) {
        return area.minY() + (row + 0.5f) * cellSize;
    }

    private int columnOf(float x) {
        return (int) Math.floor((x - area.minX()) / cellSize);
    }

    private int rowOf(float y) {
        return (int) Math.floor((y - area.minY()) / cellSize);
    }
}
