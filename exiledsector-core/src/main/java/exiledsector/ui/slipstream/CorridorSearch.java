package exiledsector.ui.slipstream;

import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public final class CorridorSearch {

    static final int ATTEMPTS = 8;
    static final int BORDER_PROBES = 12;
    static final float OPPOSITE_SIDE_CHANCE = 0.7f;
    static final float MAX_DETOUR = 1.8f;
    static final float MIN_SHARE_IN_VIEW = 0.3f;
    static final int CROWDING_CELLS = 4;
    static final float CROWDING_COST = 1.5f;
    static final int SMOOTHING_PASSES = 3;
    static final float VIEW_SAMPLE_SPACING_WIDTHS = 0.5f;
    private static final float DIAGONAL = (float) Math.sqrt(2.0);
    private static final int[][] STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private CorridorSearch() {
    }

    public static SlipstreamRoute route(CorridorGrid grid, ViewWindow view, float width, Random random) {
        int[] cellsToBlocked = grid.cellsToBlocked();
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int entrySide = random.nextInt(4);
            int exitSide = random.nextFloat() < OPPOSITE_SIDE_CHANCE ? entrySide ^ 1 : (entrySide < 2 ? 2 : 0) + random.nextInt(2);
            int entry = freeBorderCell(grid, entrySide, random);
            int exit = freeBorderCell(grid, exitSide, random);
            if (entry < 0 || exit < 0) {
                continue;
            }
            List<Vector2f> cells = shortestPath(grid, cellsToBlocked, entry, exit);
            if (cells == null) {
                continue;
            }
            List<Vector2f> smoothed = smoothed(grid, pulled(grid, cells));
            if (isAcceptable(smoothed, view, width)) {
                return new SlipstreamRoute(smoothed, width);
            }
        }
        return null;
    }

    static boolean isAcceptable(List<Vector2f> points, ViewWindow view, float width) {
        Vector2f first = points.get(0);
        Vector2f last = points.get(points.size() - 1);
        float straight = Vector2f.sub(last, first, null).length();
        float length = 0f;
        float lengthInView = 0f;
        float sampleSpacing = Math.max(1f, width * VIEW_SAMPLE_SPACING_WIDTHS);
        for (int i = 1; i < points.size(); i++) {
            Vector2f from = points.get(i - 1);
            Vector2f to = points.get(i);
            float segmentLength = Vector2f.sub(to, from, null).length();
            length += segmentLength;
            int samples = Math.max(1, (int) Math.ceil(segmentLength / sampleSpacing));
            for (int sample = 0; sample < samples; sample++) {
                float t = (sample + 0.5f) / samples;
                if (view.contains(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)) {
                    lengthInView += segmentLength / samples;
                }
            }
        }
        return straight > 0f && length <= straight * MAX_DETOUR && lengthInView >= length * MIN_SHARE_IN_VIEW;
    }

    private static int freeBorderCell(CorridorGrid grid, int side, Random random) {
        int span = side < 2 ? grid.rows() : grid.columns();
        for (int probe = 0; probe < BORDER_PROBES; probe++) {
            int along = span / 6 + random.nextInt(Math.max(1, span * 2 / 3));
            int column;
            int row;
            switch (side) {
                case 0 -> {
                    column = 0;
                    row = along;
                }
                case 1 -> {
                    column = grid.columns() - 1;
                    row = along;
                }
                case 2 -> {
                    column = along;
                    row = 0;
                }
                default -> {
                    column = along;
                    row = grid.rows() - 1;
                }
            }
            if (column < grid.columns() && row < grid.rows() && !grid.isBlocked(column, row)) {
                return row * grid.columns() + column;
            }
        }
        return -1;
    }

    private static List<Vector2f> shortestPath(CorridorGrid grid, int[] cellsToBlocked, int start, int goal) {
        int columns = grid.columns();
        int cellCount = columns * grid.rows();
        float[] cost = new float[cellCount];
        Arrays.fill(cost, Float.MAX_VALUE);
        int[] cameFrom = new int[cellCount];
        Arrays.fill(cameFrom, -1);
        boolean[] closed = new boolean[cellCount];
        int goalColumn = goal % columns;
        int goalRow = goal / columns;
        PriorityQueue<float[]> open = new PriorityQueue<>((a, b) -> Float.compare(a[0], b[0]));
        cost[start] = 0f;
        open.add(new float[]{estimate(start % columns, start / columns, goalColumn, goalRow), start});
        while (!open.isEmpty()) {
            int cell = (int) open.poll()[1];
            if (closed[cell]) {
                continue;
            }
            if (cell == goal) {
                return cellPath(grid, cameFrom, goal);
            }
            closed[cell] = true;
            int column = cell % columns;
            int row = cell / columns;
            for (int[] step : STEPS) {
                int nextColumn = column + step[0];
                int nextRow = row + step[1];
                if (nextColumn < 0 || nextColumn >= columns || nextRow < 0 || nextRow >= grid.rows() || grid.isBlocked(nextColumn, nextRow)) {
                    continue;
                }
                boolean diagonal = step[0] != 0 && step[1] != 0;
                if (diagonal && (grid.isBlocked(column + step[0], row) || grid.isBlocked(column, row + step[1]))) {
                    continue;
                }
                int next = nextRow * columns + nextColumn;
                float crowding = Math.max(0, CROWDING_CELLS - cellsToBlocked[next]) / (float) CROWDING_CELLS * CROWDING_COST;
                float nextCost = cost[cell] + (diagonal ? DIAGONAL : 1f) * (1f + crowding);
                if (nextCost < cost[next]) {
                    cost[next] = nextCost;
                    cameFrom[next] = cell;
                    open.add(new float[]{nextCost + estimate(nextColumn, nextRow, goalColumn, goalRow), next});
                }
            }
        }
        return null;
    }

    private static float estimate(int column, int row, int goalColumn, int goalRow) {
        int dx = Math.abs(goalColumn - column);
        int dy = Math.abs(goalRow - row);
        return Math.max(dx, dy) + (DIAGONAL - 1f) * Math.min(dx, dy);
    }

    private static List<Vector2f> cellPath(CorridorGrid grid, int[] cameFrom, int goal) {
        List<Vector2f> path = new ArrayList<>();
        for (int cell = goal; cell >= 0; cell = cameFrom[cell]) {
            path.add(new Vector2f(grid.centerX(cell % grid.columns()), grid.centerY(cell / grid.columns())));
        }
        Collections.reverse(path);
        return path;
    }

    static List<Vector2f> pulled(CorridorGrid grid, List<Vector2f> path) {
        List<Vector2f> pulled = new ArrayList<>();
        int anchor = 0;
        pulled.add(path.get(0));
        while (anchor < path.size() - 1) {
            int farthest = anchor + 1;
            for (int candidate = path.size() - 1; candidate > anchor + 1; candidate--) {
                Vector2f from = path.get(anchor);
                Vector2f to = path.get(candidate);
                if (grid.isFreeLine(from.x, from.y, to.x, to.y)) {
                    farthest = candidate;
                    break;
                }
            }
            pulled.add(path.get(farthest));
            anchor = farthest;
        }
        if (pulled.size() == 2) {
            Vector2f first = pulled.get(0);
            Vector2f last = pulled.get(1);
            pulled.add(1, new Vector2f((first.x + last.x) / 2f, (first.y + last.y) / 2f));
        }
        return pulled;
    }

    static List<Vector2f> smoothed(CorridorGrid grid, List<Vector2f> path) {
        List<Vector2f> current = path;
        for (int pass = 0; pass < SMOOTHING_PASSES; pass++) {
            List<Vector2f> next = new ArrayList<>(current.size() * 2);
            next.add(current.get(0));
            for (int i = 0; i < current.size() - 1; i++) {
                Vector2f from = current.get(i);
                Vector2f to = current.get(i + 1);
                next.add(new Vector2f(from.x * 0.75f + to.x * 0.25f, from.y * 0.75f + to.y * 0.25f));
                next.add(new Vector2f(from.x * 0.25f + to.x * 0.75f, from.y * 0.25f + to.y * 0.75f));
            }
            next.add(current.get(current.size() - 1));
            if (!isFreePath(grid, next)) {
                return current;
            }
            current = next;
        }
        return current;
    }

    private static boolean isFreePath(CorridorGrid grid, List<Vector2f> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            Vector2f from = path.get(i);
            Vector2f to = path.get(i + 1);
            if (!grid.isFreeLine(from.x, from.y, to.x, to.y)) {
                return false;
            }
        }
        return true;
    }
}
