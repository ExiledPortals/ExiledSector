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
    static final int THROUGH_PROBES = 12;
    static final float THROUGH_REACH_SHARE = 0.25f;
    static final float MAX_TILT_DEGREES = 35f;
    static final float END_SHIFT_SHARE = 0.3f;
    static final int BORDER_SEARCH_CELLS = 12;
    static final float MAX_DETOUR = 1.8f;
    static final float MIN_VIEW_CROSSING_SHARE = 0.5f;
    static final int CROWDING_CELLS = 4;
    static final float CROWDING_COST = 1.5f;
    static final float SPACING_WIDTHS = 0.5f;
    static final int RELAX_ROUNDS = 4;
    static final int RELAX_PASSES = 25;
    static final float RELAX_SHARE = 0.5f;
    static final float MIN_RELAX_SHARE = 0.05f;
    private static final float DIAGONAL = (float) Math.sqrt(2.0);
    private static final int[][] STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private CorridorSearch() {
    }

    public static SlipstreamRoute route(CorridorGrid grid, ViewWindow view, float width, Random random) {
        int[] cellsToBlocked = grid.cellsToBlocked();
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            Vector2f through = freeThroughPoint(grid, view, random);
            if (through == null) {
                continue;
            }
            double heading = heading(view, random);
            int entry = borderCellNear(grid, endPoint(grid, through, heading + Math.PI, random));
            int exit = borderCellNear(grid, endPoint(grid, through, heading, random));
            int middle = grid.cellAt(through.x, through.y);
            if (entry < 0 || exit < 0 || middle < 0) {
                continue;
            }
            List<Vector2f> approach = shortestPath(grid, cellsToBlocked, entry, middle);
            List<Vector2f> departure = approach == null ? null : shortestPath(grid, cellsToBlocked, middle, exit);
            if (departure == null) {
                continue;
            }
            List<Vector2f> legs = new ArrayList<>(pulled(grid, approach));
            List<Vector2f> secondLeg = pulled(grid, departure);
            legs.addAll(secondLeg.subList(1, secondLeg.size()));
            List<Vector2f> relaxed = relaxed(grid, legs, width * SPACING_WIDTHS);
            if (isAcceptable(relaxed, view, width)) {
                return new SlipstreamRoute(relaxed, width);
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
        float sampleSpacing = Math.max(1f, width * SPACING_WIDTHS);
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
        float longSide = Math.max(view.width(), view.height());
        return straight > 0f && length <= straight * MAX_DETOUR && lengthInView >= longSide * MIN_VIEW_CROSSING_SHARE;
    }

    private static Vector2f freeThroughPoint(CorridorGrid grid, ViewWindow view, Random random) {
        float centerX = (view.minX() + view.maxX()) / 2f;
        float centerY = (view.minY() + view.maxY()) / 2f;
        for (int probe = 0; probe < THROUGH_PROBES; probe++) {
            float x = centerX + (random.nextFloat() * 2f - 1f) * THROUGH_REACH_SHARE * view.width();
            float y = centerY + (random.nextFloat() * 2f - 1f) * THROUGH_REACH_SHARE * view.height();
            if (grid.isFree(x, y)) {
                return new Vector2f(x, y);
            }
        }
        return null;
    }

    private static double heading(ViewWindow view, Random random) {
        double along = view.width() >= view.height() ? 0.0 : Math.PI / 2.0;
        if (random.nextBoolean()) {
            along += Math.PI;
        }
        return along + Math.toRadians(MAX_TILT_DEGREES * (random.nextFloat() * 2f - 1f));
    }

    private static Vector2f endPoint(CorridorGrid grid, Vector2f through, double heading, Random random) {
        ViewWindow area = grid.area();
        float directionX = (float) Math.cos(heading);
        float directionY = (float) Math.sin(heading);
        float reach = Float.MAX_VALUE;
        if (directionX > 0f) {
            reach = Math.min(reach, (area.maxX() - through.x) / directionX);
        } else if (directionX < 0f) {
            reach = Math.min(reach, (area.minX() - through.x) / directionX);
        }
        if (directionY > 0f) {
            reach = Math.min(reach, (area.maxY() - through.y) / directionY);
        } else if (directionY < 0f) {
            reach = Math.min(reach, (area.minY() - through.y) / directionY);
        }
        float shift = (random.nextFloat() * 2f - 1f) * END_SHIFT_SHARE * Math.min(area.width(), area.height());
        return new Vector2f(through.x + directionX * reach - directionY * shift, through.y + directionY * reach + directionX * shift);
    }

    private static int borderCellNear(CorridorGrid grid, Vector2f point) {
        ViewWindow area = grid.area();
        int column = Math.max(0, Math.min(grid.columns() - 1, (int) Math.floor((point.x - area.minX()) / grid.cellSize())));
        int row = Math.max(0, Math.min(grid.rows() - 1, (int) Math.floor((point.y - area.minY()) / grid.cellSize())));
        int toLeft = column;
        int toRight = grid.columns() - 1 - column;
        int toBottom = row;
        int toTop = grid.rows() - 1 - row;
        boolean onSide = Math.min(toLeft, toRight) <= Math.min(toBottom, toTop);
        if (onSide) {
            column = toLeft <= toRight ? 0 : grid.columns() - 1;
        } else {
            row = toBottom <= toTop ? 0 : grid.rows() - 1;
        }
        for (int offset = 0; offset <= BORDER_SEARCH_CELLS; offset++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                int probeColumn = onSide ? column : column + sign * offset;
                int probeRow = onSide ? row + sign * offset : row;
                if (probeColumn >= 0 && probeColumn < grid.columns() && probeRow >= 0 && probeRow < grid.rows()
                        && !grid.isBlocked(probeColumn, probeRow)) {
                    return probeRow * grid.columns() + probeColumn;
                }
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
        return pulled;
    }

    static List<Vector2f> relaxed(CorridorGrid grid, List<Vector2f> path, float spacing) {
        List<Vector2f> points = resampled(path, spacing);
        for (int round = 0; round < RELAX_ROUNDS; round++) {
            for (int pass = 0; pass < RELAX_PASSES; pass++) {
                for (int i = 1; i < points.size() - 1; i++) {
                    Vector2f previous = points.get(i - 1);
                    Vector2f point = points.get(i);
                    Vector2f next = points.get(i + 1);
                    float pullX = (previous.x + next.x) / 2f - point.x;
                    float pullY = (previous.y + next.y) / 2f - point.y;
                    for (float share = RELAX_SHARE; share >= MIN_RELAX_SHARE; share /= 2f) {
                        float x = point.x + pullX * share;
                        float y = point.y + pullY * share;
                        if (grid.isClearLine(previous.x, previous.y, x, y) && grid.isClearLine(x, y, next.x, next.y)) {
                            point.set(x, y);
                            break;
                        }
                    }
                }
            }
            points = resampled(points, spacing);
        }
        return points;
    }

    static List<Vector2f> resampled(List<Vector2f> path, float spacing) {
        List<Vector2f> points = new ArrayList<>();
        points.add(new Vector2f(path.get(0)));
        float untilNext = spacing;
        for (int i = 1; i < path.size(); i++) {
            Vector2f from = path.get(i - 1);
            Vector2f to = path.get(i);
            float segmentLength = Vector2f.sub(to, from, null).length();
            float along = untilNext;
            while (along <= segmentLength) {
                float t = along / segmentLength;
                points.add(new Vector2f(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t));
                along += spacing;
            }
            untilNext = along - segmentLength;
        }
        Vector2f last = path.get(path.size() - 1);
        if (Vector2f.sub(last, points.get(points.size() - 1), null).lengthSquared() > 0f) {
            points.add(new Vector2f(last));
        }
        if (points.size() < 2) {
            points.add(new Vector2f(last));
        }
        return points;
    }
}
