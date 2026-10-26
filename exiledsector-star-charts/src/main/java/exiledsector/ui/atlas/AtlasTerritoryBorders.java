package exiledsector.ui.atlas;

import exiledlib.i18n.I18n;
import exiledlib.skills.skilleffect.render.Fade;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.TextLabel;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

final class AtlasTerritoryBorders {

    static final float INSET_CELLS = 0.02f;
    static final float BAND_CELLS = 0.3f;
    static final float BAND_RADIUS_SHARE = 0.8f;
    static final float BAND_SLOPE = 0.6f;
    static final float UNFOLD_SHRINK = 0.6f;
    static final int MAX_UNFOLD_PASSES = 30;
    static final int CHAIKIN_PASSES = 3;
    static final float ROUGH_CELLS = 0.22f;
    static final float ROUGH_SPACING_CELLS = 1.1f;
    static final float ROUGH_OCTAVE_SHARE = 0.5f;
    static final float SYSTEM_CLEARANCE_CELLS = 0.24f;
    private static final long ROUGH_X_SALT = 0x524f554758L;
    private static final long ROUGH_Y_SALT = 0x524f554759L;
    private static final long ROUGH_OCTAVE_SALT = 0x4f4354L;
    static final int MIN_LABEL_CELLS = 6;
    static final float BAND_ALPHA = 0.22f;
    static final float OUTLINE_ALPHA = 0.8f;
    static final float OUTLINE_WIDTH = 2f;
    static final float OUTLINE_WHITENESS = 0.3f;
    static final float LABEL_ALPHA = 0.55f;
    static final float LABEL_FONT_SIZE = 30f;
    static final float LABEL_ROOM_SHARE = 1.2f;
    static final float FADE_SECONDS = 0.25f;
    private static final int[][] SIDES = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    record Loop(float[] outline, float[] inner, float minX, float minY, float maxX, float maxY) {
    }

    record Region(String groupId, List<Loop> loops, float labelX, float labelY, int cells, float minX, float minY, float maxX, float maxY) {
    }

    private record Edge(int fromX, int fromY, int toX, int toY) {
    }

    private final long seed;
    private final Function<String, Color> colours;
    private final Function<String, String> names;
    private final List<TextLabel> labels = new ArrayList<>();
    private List<Region> regions = List.of();
    private boolean shown;
    private float level;

    AtlasTerritoryBorders(long seed, Function<String, Color> colours, Function<String, String> names) {
        this.seed = seed;
        this.colours = colours;
        this.names = names;
    }

    void attach(Set<Long> known, Function<AtlasCell, String> groupAt, Predicate<AtlasCell> hasSystem) {
        for (TextLabel label : labels) {
            if (label != null) {
                label.dispose();
            }
        }
        labels.clear();
        regions = build(seed, known, groupAt, hasSystem);
        for (int index = 0; index < regions.size(); index++) {
            labels.add(null);
        }
    }

    List<Region> regions() {
        return regions;
    }

    void show(boolean visible, boolean instantly) {
        shown = visible;
        if (instantly) {
            level = visible ? 1f : 0f;
        }
    }

    boolean isShown() {
        return shown;
    }

    void advance(float amount) {
        level = Fade.step(level, shown, amount / FADE_SECONDS);
    }

    void render(TreeViewport view, float alphaMult) {
        float alpha = Fade.smooth(level) * alphaMult;
        if (alpha <= 0f || regions.isEmpty()) {
            return;
        }
        List<Region> onScreen = new ArrayList<>();
        List<Integer> onScreenIndexes = new ArrayList<>();
        for (int index = 0; index < regions.size(); index++) {
            Region region = regions.get(index);
            if (visible(view, region.minX(), region.minY(), region.maxX(), region.maxY())) {
                onScreen.add(region);
                onScreenIndexes.add(index);
            }
        }
        if (onScreen.isEmpty()) {
            return;
        }
        try (GlScope scope = GlScope.flat()) {
            for (Region region : onScreen) {
                Color tint = colour(region);
                for (Loop loop : region.loops()) {
                    if (visible(view, loop.minX(), loop.minY(), loop.maxX(), loop.maxY())) {
                        drawBand(view, loop, tint, BAND_ALPHA * alpha);
                    }
                }
            }
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glLineWidth(OUTLINE_WIDTH);
            for (Region region : onScreen) {
                Color outline = towardWhite(colour(region), OUTLINE_WHITENESS);
                GL11.glColor4f(outline.getRed() / 255f, outline.getGreen() / 255f, outline.getBlue() / 255f, OUTLINE_ALPHA * alpha);
                for (Loop loop : region.loops()) {
                    if (visible(view, loop.minX(), loop.minY(), loop.maxX(), loop.maxY())) {
                        GL11.glBegin(GL11.GL_LINE_LOOP);
                        for (int index = 0; index < loop.outline().length; index += 2) {
                            GL11.glVertex2f(view.screenX(loop.outline()[index]), view.screenY(loop.outline()[index + 1]));
                        }
                        GL11.glEnd();
                    }
                }
            }
        }
        for (int index : onScreenIndexes) {
            drawLabel(view, index, alpha);
        }
    }

    private static void drawBand(TreeViewport view, Loop loop, Color tint, float alpha) {
        float red = tint.getRed() / 255f;
        float green = tint.getGreen() / 255f;
        float blue = tint.getBlue() / 255f;
        float[] outline = loop.outline();
        float[] inner = loop.inner();
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int index = 0; index <= outline.length; index += 2) {
            int at = index % outline.length;
            GL11.glColor4f(red, green, blue, alpha);
            GL11.glVertex2f(view.screenX(outline[at]), view.screenY(outline[at + 1]));
            GL11.glColor4f(red, green, blue, 0f);
            GL11.glVertex2f(view.screenX(inner[at]), view.screenY(inner[at + 1]));
        }
        GL11.glEnd();
    }

    private void drawLabel(TreeViewport view, int index, float alpha) {
        Region region = regions.get(index);
        if (region.cells() < MIN_LABEL_CELLS) {
            return;
        }
        TextLabel label = labels.get(index);
        if (label == null) {
            TextLabel created = new TextLabel(LABEL_FONT_SIZE, towardWhite(colour(region), OUTLINE_WHITENESS), LazyFont.TextAnchor.CENTER);
            I18n.forGameText(() -> created.set(names.apply(region.groupId())));
            labels.set(index, created);
            label = created;
        }
        float roomWidth = (region.maxX() - region.minX()) * view.zoom();
        if (label.width() * LABEL_ROOM_SHARE > roomWidth) {
            return;
        }
        label.setAlpha(LABEL_ALPHA * alpha).draw(view.screenX(region.labelX()), view.screenY(region.labelY()));
    }

    private Color colour(Region region) {
        Color colour = colours.apply(region.groupId());
        return colour == null ? Color.WHITE : colour;
    }

    private static boolean visible(TreeViewport view, float minX, float minY, float maxX, float maxY) {
        return view.overlaps(view.screenX(minX), view.screenY(maxY), view.screenX(maxX), view.screenY(minY));
    }

    static Color towardWhite(Color colour, float share) {
        return new Color(Math.round(colour.getRed() + (255 - colour.getRed()) * share), Math.round(colour.getGreen() + (255 - colour.getGreen()) * share),
                Math.round(colour.getBlue() + (255 - colour.getBlue()) * share));
    }

    static List<Region> build(long seed, Set<Long> known, Function<AtlasCell, String> groupAt, Predicate<AtlasCell> hasSystem) {
        Map<Long, String> groupOf = new HashMap<>();
        List<Long> ordered = new ArrayList<>(known);
        ordered.sort(Comparator.naturalOrder());
        for (long packed : ordered) {
            String groupId = groupAt.apply(AtlasCell.unpack(packed));
            if (groupId != null) {
                groupOf.put(packed, groupId);
            }
        }
        List<Region> built = new ArrayList<>();
        Set<Long> claimed = new HashSet<>();
        for (long packed : ordered) {
            if (!groupOf.containsKey(packed) || !claimed.add(packed)) {
                continue;
            }
            String groupId = groupOf.get(packed);
            Set<Long> patch = new HashSet<>(Set.of(packed));
            ArrayDeque<AtlasCell> queue = new ArrayDeque<>(List.of(AtlasCell.unpack(packed)));
            boolean holdsSystem = false;
            while (!queue.isEmpty()) {
                AtlasCell cell = queue.poll();
                holdsSystem |= hasSystem.test(cell);
                for (int[] side : SIDES) {
                    long next = new AtlasCell(cell.gridX() + side[0], cell.gridY() + side[1]).pack();
                    if (groupId.equals(groupOf.get(next)) && claimed.add(next)) {
                        patch.add(next);
                        queue.add(AtlasCell.unpack(next));
                    }
                }
            }
            if (holdsSystem) {
                built.add(region(seed, groupId, patch));
            }
        }
        return List.copyOf(built);
    }

    private static Region region(long seed, String groupId, Set<Long> patch) {
        List<Loop> loops = new ArrayList<>();
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (List<float[]> corners : trace(patch)) {
            Loop loop = loop(roughen(seed, smooth(corners)));
            loops.add(loop);
            minX = Math.min(minX, loop.minX());
            minY = Math.min(minY, loop.minY());
            maxX = Math.max(maxX, loop.maxX());
            maxY = Math.max(maxY, loop.maxY());
        }
        AtlasCell labelCell = deepest(patch);
        return new Region(groupId, List.copyOf(loops), labelCell.gridX() * AtlasMapCanvas.CELL_PIXELS, labelCell.gridY() * AtlasMapCanvas.CELL_PIXELS,
                patch.size(), minX, minY, maxX, maxY);
    }

    static List<List<float[]>> trace(Set<Long> patch) {
        Map<Long, List<Edge>> outgoing = new HashMap<>();
        List<Edge> edges = new ArrayList<>();
        List<Long> ordered = new ArrayList<>(patch);
        ordered.sort(Comparator.naturalOrder());
        for (long packed : ordered) {
            AtlasCell cell = AtlasCell.unpack(packed);
            int left = cell.gridX() * 2 - 1;
            int right = cell.gridX() * 2 + 1;
            int bottom = cell.gridY() * 2 - 1;
            int top = cell.gridY() * 2 + 1;
            int[][] corners = {{left, bottom}, {right, bottom}, {right, top}, {left, top}};
            for (int side = 0; side < SIDES.length; side++) {
                if (!patch.contains(new AtlasCell(cell.gridX() + SIDES[side][0], cell.gridY() + SIDES[side][1]).pack())) {
                    int[] from = corners[side];
                    int[] to = corners[(side + 1) % corners.length];
                    Edge edge = new Edge(from[0], from[1], to[0], to[1]);
                    edges.add(edge);
                    outgoing.computeIfAbsent(point(from[0], from[1]), key -> new ArrayList<>()).add(edge);
                }
            }
        }
        Set<Edge> used = new HashSet<>();
        List<List<float[]>> loops = new ArrayList<>();
        for (Edge start : edges) {
            if (!used.add(start)) {
                continue;
            }
            List<float[]> corners = new ArrayList<>();
            Edge current = start;
            corners.add(new float[] {start.fromX() / 2f, start.fromY() / 2f});
            while (current.toX() != start.fromX() || current.toY() != start.fromY()) {
                corners.add(new float[] {current.toX() / 2f, current.toY() / 2f});
                Edge next = null;
                int bestTurn = Integer.MIN_VALUE;
                for (Edge candidate : outgoing.getOrDefault(point(current.toX(), current.toY()), List.of())) {
                    int turn = (current.toX() - current.fromX()) * (candidate.toY() - candidate.fromY())
                            - (current.toY() - current.fromY()) * (candidate.toX() - candidate.fromX());
                    if (!used.contains(candidate) && turn > bestTurn) {
                        bestTurn = turn;
                        next = candidate;
                    }
                }
                if (next == null) {
                    break;
                }
                used.add(next);
                current = next;
            }
            loops.add(corners);
        }
        return loops;
    }

    static List<float[]> smooth(List<float[]> corners) {
        List<float[]> points = new ArrayList<>(corners.size());
        for (int index = 0; index < corners.size(); index++) {
            float[] from = corners.get(index);
            float[] to = corners.get((index + 1) % corners.size());
            points.add(new float[] {(from[0] + to[0]) / 2f, (from[1] + to[1]) / 2f});
        }
        for (int pass = 0; pass < CHAIKIN_PASSES; pass++) {
            List<float[]> rounded = new ArrayList<>(points.size() * 2);
            for (int index = 0; index < points.size(); index++) {
                float[] from = points.get(index);
                float[] to = points.get((index + 1) % points.size());
                rounded.add(new float[] {from[0] * 0.75f + to[0] * 0.25f, from[1] * 0.75f + to[1] * 0.25f});
                rounded.add(new float[] {from[0] * 0.25f + to[0] * 0.75f, from[1] * 0.25f + to[1] * 0.75f});
            }
            points = rounded;
        }
        return points;
    }

    static List<float[]> roughen(long seed, List<float[]> points) {
        List<float[]> roughened = new ArrayList<>(points.size());
        for (float[] point : points) {
            float noiseX = point[0] / ROUGH_SPACING_CELLS;
            float noiseY = point[1] / ROUGH_SPACING_CELLS;
            float shiftX = ROUGH_CELLS * roughness(seed, ROUGH_X_SALT, noiseX, noiseY);
            float shiftY = ROUGH_CELLS * roughness(seed, ROUGH_Y_SALT, noiseX, noiseY);
            float room = (float) Math.hypot(point[0] - Math.round(point[0]), point[1] - Math.round(point[1])) - INSET_CELLS - SYSTEM_CLEARANCE_CELLS;
            float shift = (float) Math.hypot(shiftX, shiftY);
            float kept = shift <= 0f ? 0f : Math.max(0f, Math.min(1f, room / shift));
            roughened.add(new float[] {point[0] + shiftX * kept, point[1] + shiftY * kept});
        }
        return roughened;
    }

    private static float roughness(long seed, long salt, float x, float y) {
        float noise = (valueNoise(seed, salt, x, y) + ROUGH_OCTAVE_SHARE * valueNoise(seed, salt ^ ROUGH_OCTAVE_SALT, x * 2f, y * 2f)) / (1f + ROUGH_OCTAVE_SHARE);
        return noise * 2f - 1f;
    }

    private static float valueNoise(long seed, long salt, float x, float y) {
        int cornerX = (int) Math.floor(x);
        int cornerY = (int) Math.floor(y);
        float fractionX = fade(x - cornerX);
        float fractionY = fade(y - cornerY);
        float bottom = lerp(lattice(seed, salt, cornerX, cornerY), lattice(seed, salt, cornerX + 1, cornerY), fractionX);
        float top = lerp(lattice(seed, salt, cornerX, cornerY + 1), lattice(seed, salt, cornerX + 1, cornerY + 1), fractionX);
        return lerp(bottom, top, fractionY);
    }

    private static float lattice(long seed, long salt, int x, int y) {
        return (mix(mix(mix(seed ^ salt) ^ x) ^ ((long) y << 32)) >>> 40) / (float) (1 << 24);
    }

    private static long mix(long value) {
        long mixed = value;
        mixed = (mixed ^ (mixed >>> 30)) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94d049bb133111ebL;
        return mixed ^ (mixed >>> 31);
    }

    private static float fade(float fraction) {
        return fraction * fraction * fraction * (fraction * (fraction * 6f - 15f) + 10f);
    }

    private static float lerp(float from, float to, float fraction) {
        return from + (to - from) * fraction;
    }

    private static Loop loop(List<float[]> points) {
        int count = points.size();
        float[] normals = new float[count * 2];
        float[] outline = new float[count * 2];
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int index = 0; index < count; index++) {
            float[] previous = points.get((index + count - 1) % count);
            float[] here = points.get(index);
            float[] next = points.get((index + 1) % count);
            float normalX = -(here[1] - previous[1]) / length(here, previous) - (next[1] - here[1]) / length(next, here);
            float normalY = (here[0] - previous[0]) / length(here, previous) + (next[0] - here[0]) / length(next, here);
            float normalLength = (float) Math.hypot(normalX, normalY);
            normals[index * 2] = normalLength <= 0f ? 0f : normalX / normalLength;
            normals[index * 2 + 1] = normalLength <= 0f ? 0f : normalY / normalLength;
            float x = (here[0] + normals[index * 2] * INSET_CELLS) * AtlasMapCanvas.CELL_PIXELS;
            float y = (here[1] + normals[index * 2 + 1] * INSET_CELLS) * AtlasMapCanvas.CELL_PIXELS;
            outline[index * 2] = x;
            outline[index * 2 + 1] = y;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        float[] depths = bandDepths(points, outline);
        float[] inner = new float[count * 2];
        for (int attempt = 0; ; attempt++) {
            for (int index = 0; index < count; index++) {
                float depth = depths[index] * AtlasMapCanvas.CELL_PIXELS;
                inner[index * 2] = outline[index * 2] + normals[index * 2] * depth;
                inner[index * 2 + 1] = outline[index * 2 + 1] + normals[index * 2 + 1] * depth;
            }
            if (!unfold(outline, inner, depths) || attempt >= MAX_UNFOLD_PASSES) {
                break;
            }
        }
        return new Loop(outline, inner, minX, minY, maxX, maxY);
    }

    private static float[] bandDepths(List<float[]> points, float[] outline) {
        int count = points.size();
        float[] depths = new float[count];
        for (int index = 0; index < count; index++) {
            float[] previous = points.get((index + count - 1) % count);
            float[] here = points.get(index);
            float[] next = points.get((index + 1) % count);
            float turn = (here[0] - previous[0]) * (next[1] - here[1]) - (here[1] - previous[1]) * (next[0] - here[0]);
            float radius = turn <= 0f ? Float.MAX_VALUE : length(here, previous) * length(next, here) * length(next, previous) / (2f * turn);
            depths[index] = Math.min(BAND_CELLS, BAND_RADIUS_SHARE * radius);
        }
        for (int lap = 0; lap < 2; lap++) {
            for (int step = 1; step <= count; step++) {
                int forward = step % count;
                int forwardPrevious = step - 1;
                depths[forward] = Math.min(depths[forward], depths[forwardPrevious] + BAND_SLOPE * segmentCells(outline, forwardPrevious, forward));
                int backward = count - step;
                int backwardPrevious = (backward + 1) % count;
                depths[backward] = Math.min(depths[backward], depths[backwardPrevious] + BAND_SLOPE * segmentCells(outline, backward, backwardPrevious));
            }
        }
        return depths;
    }

    private static boolean unfold(float[] outline, float[] inner, float[] depths) {
        int count = depths.length;
        boolean folded = false;
        for (int index = 0; index < count; index++) {
            int next = (index + 1) % count;
            if (folds(outline, inner, index, next)) {
                depths[index] *= UNFOLD_SHRINK;
                depths[next] *= UNFOLD_SHRINK;
                folded = true;
            }
        }
        return folded;
    }

    private static boolean folds(float[] outline, float[] inner, int index, int next) {
        float[] quadX = {outline[index * 2], outline[next * 2], inner[next * 2], inner[index * 2]};
        float[] quadY = {outline[index * 2 + 1], outline[next * 2 + 1], inner[next * 2 + 1], inner[index * 2 + 1]};
        for (int corner = 0; corner < quadX.length; corner++) {
            int b = (corner + 1) % quadX.length;
            int c = (corner + 2) % quadX.length;
            double turn = (quadX[b] - quadX[corner]) * (double) (quadY[c] - quadY[b]) - (quadY[b] - quadY[corner]) * (double) (quadX[c] - quadX[b]);
            if (turn < 0.0) {
                return true;
            }
        }
        return false;
    }

    private static float segmentCells(float[] outline, int from, int to) {
        return (float) Math.hypot(outline[to * 2] - outline[from * 2], outline[to * 2 + 1] - outline[from * 2 + 1]) / AtlasMapCanvas.CELL_PIXELS;
    }

    private static AtlasCell deepest(Set<Long> patch) {
        Map<Long, Integer> depth = new HashMap<>();
        ArrayDeque<AtlasCell> queue = new ArrayDeque<>();
        float sumX = 0f;
        float sumY = 0f;
        List<Long> ordered = new ArrayList<>(patch);
        ordered.sort(Comparator.naturalOrder());
        for (long packed : ordered) {
            AtlasCell cell = AtlasCell.unpack(packed);
            sumX += cell.gridX();
            sumY += cell.gridY();
            for (int[] side : SIDES) {
                if (!patch.contains(new AtlasCell(cell.gridX() + side[0], cell.gridY() + side[1]).pack())) {
                    depth.put(packed, 0);
                    queue.add(cell);
                    break;
                }
            }
        }
        while (!queue.isEmpty()) {
            AtlasCell cell = queue.poll();
            int here = depth.get(cell.pack());
            for (int[] side : SIDES) {
                long next = new AtlasCell(cell.gridX() + side[0], cell.gridY() + side[1]).pack();
                if (patch.contains(next) && !depth.containsKey(next)) {
                    depth.put(next, here + 1);
                    queue.add(AtlasCell.unpack(next));
                }
            }
        }
        float centreX = sumX / patch.size();
        float centreY = sumY / patch.size();
        AtlasCell best = null;
        int bestDepth = -1;
        double bestDistance = Double.MAX_VALUE;
        for (long packed : ordered) {
            AtlasCell cell = AtlasCell.unpack(packed);
            int cellDepth = depth.getOrDefault(packed, 0);
            double distance = Math.hypot(cell.gridX() - centreX, cell.gridY() - centreY);
            if (cellDepth > bestDepth || cellDepth == bestDepth && distance < bestDistance) {
                best = cell;
                bestDepth = cellDepth;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static float length(float[] from, float[] to) {
        return Math.max(1e-6f, (float) Math.hypot(from[0] - to[0], from[1] - to[1]));
    }

    private static long point(int x, int y) {
        return new AtlasCell(x, y).pack();
    }
}
