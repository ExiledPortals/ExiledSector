package exiledsector.ui.atlas;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class AtlasBubbleField {

    static final int TEXELS_PER_CELL = 16;
    static final int MOST_TEXELS = 1024;
    static final int MARGIN_TEXELS = 2;
    static final int SMOOTHING_PASSES = 2;
    static final float RIM_WIDTH_CELLS = 0.18f;
    static final float SPOT_RADIUS_SHARE = 0.22f;
    static final float SPOT_SHIFT_SHARE = 0.4f;
    static final float NEAR_EDGE_CELLS = 0.75f;
    static final float EDGE_BUCKET_CELLS = 0.5f;
    private static final float FAR = 1e20f;

    private final float originX;
    private final float originY;
    private final float texelSize;
    private final int width;
    private final int height;
    private final float peak;
    private final float[] heightShares;
    private final float[] alphas;
    private final float[] whiteness;

    private AtlasBubbleField(float originX, float originY, float texelSize, int width, int height, float peak, float[] heightShares, float[] alphas,
                             float[] whiteness) {
        this.originX = originX;
        this.originY = originY;
        this.texelSize = texelSize;
        this.width = width;
        this.height = height;
        this.peak = peak;
        this.heightShares = heightShares;
        this.alphas = alphas;
        this.whiteness = whiteness;
    }

    static AtlasBubbleField of(List<float[]> outlines, float minX, float minY, float maxX, float maxY) {
        float spanX = maxX - minX;
        float spanY = maxY - minY;
        float texelSize = Math.max(AtlasMapCanvas.CELL_PIXELS / TEXELS_PER_CELL, Math.max(spanX, spanY) / (MOST_TEXELS - 2 * MARGIN_TEXELS));
        int width = (int) Math.ceil(spanX / texelSize) + 2 * MARGIN_TEXELS;
        int height = (int) Math.ceil(spanY / texelSize) + 2 * MARGIN_TEXELS;
        float originX = minX - MARGIN_TEXELS * texelSize;
        float originY = minY - MARGIN_TEXELS * texelSize;
        boolean[] inside = fill(outlines, originX, originY, texelSize, width, height);
        float[] signed = signedDistances(outlines, inside, originX, originY, texelSize, width, height);
        float[] insideDistances = new float[signed.length];
        for (int texel = 0; texel < signed.length; texel++) {
            insideDistances[texel] = inside[texel] ? Math.max(0f, signed[texel]) : 0f;
        }
        float[] distances = smooth(insideDistances, inside, width, height);
        float peak = 0f;
        int peakTexel = 0;
        for (int texel = 0; texel < distances.length; texel++) {
            if (distances[texel] > peak) {
                peak = distances[texel];
                peakTexel = texel;
            }
        }
        float[] spotCentre = spotCentre(inside, originX, originY, texelSize, width, peakTexel, peak);
        float spotRadius = SPOT_RADIUS_SHARE * peak;
        float rimWidth = RIM_WIDTH_CELLS * AtlasMapCanvas.CELL_PIXELS;
        float[] domeHeights = new float[width * height];
        float[] heightShares = new float[width * height];
        for (int texel = 0; texel < domeHeights.length; texel++) {
            if (inside[texel] && peak > 0f) {
                float below = peak - distances[texel];
                domeHeights[texel] = (float) Math.sqrt(Math.max(0f, peak * peak - below * below));
                heightShares[texel] = domeHeights[texel] / peak;
            }
        }
        float[] alphas = new float[width * height];
        float[] whiteness = new float[width * height];
        float[] shade = new float[2];
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int texel = row * width + column;
                float coverage = Math.max(0f, Math.min(1f, 0.5f + signed[texel] / texelSize));
                if (coverage <= 0f) {
                    continue;
                }
                float slopeX = (domeHeights[row * width + Math.min(width - 1, column + 1)] - domeHeights[row * width + Math.max(0, column - 1)]) / (2f * texelSize);
                float slopeY = (domeHeights[Math.min(height - 1, row + 1) * width + column] - domeHeights[Math.max(0, row - 1) * width + column]) / (2f * texelSize);
                float length = (float) Math.sqrt(slopeX * slopeX + slopeY * slopeY + 1f);
                float edge = (float) Math.exp(-insideDistances[texel] / rimWidth);
                float fromSpotX = originX + (column + 0.5f) * texelSize - spotCentre[0];
                float fromSpotY = originY + (row + 0.5f) * texelSize - spotCentre[1];
                float spot = spotRadius > 0f ? (float) Math.exp(-(fromSpotX * fromSpotX + fromSpotY * fromSpotY) / (spotRadius * spotRadius)) : 0f;
                AtlasBubbleShading.shade(-slopeX / length, slopeY / length, edge, spot, shade);
                alphas[texel] = shade[AtlasBubbleShading.ALPHA] * coverage;
                whiteness[texel] = shade[AtlasBubbleShading.WHITENESS];
            }
        }
        return new AtlasBubbleField(originX, originY, texelSize, width, height, peak, heightShares, alphas, whiteness);
    }

    private static float[] spotCentre(boolean[] inside, float originX, float originY, float texelSize, int width, int peakTexel, float peak) {
        float peakX = originX + (peakTexel % width + 0.5f) * texelSize;
        float peakY = originY + (peakTexel / width + 0.5f) * texelSize;
        float towardLightX = AtlasBubbleShading.LIGHT[0];
        float towardLightY = -AtlasBubbleShading.LIGHT[1];
        float across = (float) Math.hypot(towardLightX, towardLightY);
        float spotX = peakX + towardLightX / across * SPOT_SHIFT_SHARE * peak;
        float spotY = peakY + towardLightY / across * SPOT_SHIFT_SHARE * peak;
        int column = (int) Math.floor((spotX - originX) / texelSize);
        int row = (int) Math.floor((spotY - originY) / texelSize);
        boolean spotInside = column >= 0 && row >= 0 && column < width && row < inside.length / width && inside[row * width + column];
        return spotInside ? new float[] {spotX, spotY} : new float[] {peakX, peakY};
    }

    private static float[] signedDistances(List<float[]> outlines, boolean[] inside, float originX, float originY, float texelSize, int width,
                                           int height) {
        boolean[] outside = new boolean[inside.length];
        for (int texel = 0; texel < inside.length; texel++) {
            outside[texel] = !inside[texel];
        }
        float[] inward = distances(inside, width, height, texelSize);
        float[] outward = distances(outside, width, height, texelSize);
        float near = NEAR_EDGE_CELLS * AtlasMapCanvas.CELL_PIXELS;
        float reach = near + 2f * texelSize;
        EdgeIndex edges = new EdgeIndex(outlines, AtlasMapCanvas.CELL_PIXELS * EDGE_BUCKET_CELLS);
        float[] signed = new float[inside.length];
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int texel = row * width + column;
                float x = originX + (column + 0.5f) * texelSize;
                float y = originY + (row + 0.5f) * texelSize;
                if (inside[texel]) {
                    signed[texel] = inward[texel] < near ? edges.nearest(x, y, reach) : inward[texel];
                } else {
                    signed[texel] = outward[texel] < 2f * texelSize ? -edges.nearest(x, y, reach) : -outward[texel];
                }
            }
        }
        return signed;
    }

    private static final class EdgeIndex {

        private final float bucketSize;
        private final Map<Long, List<float[]>> buckets = new HashMap<>();

        EdgeIndex(List<float[]> outlines, float bucketSize) {
            this.bucketSize = bucketSize;
            for (float[] outline : outlines) {
                for (int index = 0, previous = outline.length - 2; index < outline.length; previous = index, index += 2) {
                    float[] segment = {outline[previous], outline[previous + 1], outline[index], outline[index + 1]};
                    int fromX = bucket(Math.min(segment[0], segment[2]));
                    int toX = bucket(Math.max(segment[0], segment[2]));
                    int fromY = bucket(Math.min(segment[1], segment[3]));
                    int toY = bucket(Math.max(segment[1], segment[3]));
                    for (int bucketY = fromY; bucketY <= toY; bucketY++) {
                        for (int bucketX = fromX; bucketX <= toX; bucketX++) {
                            buckets.computeIfAbsent(key(bucketX, bucketY), key -> new ArrayList<>()).add(segment);
                        }
                    }
                }
            }
        }

        float nearest(float x, float y, float reach) {
            float best = reach;
            for (int bucketY = bucket(y - reach); bucketY <= bucket(y + reach); bucketY++) {
                for (int bucketX = bucket(x - reach); bucketX <= bucket(x + reach); bucketX++) {
                    for (float[] segment : buckets.getOrDefault(key(bucketX, bucketY), List.of())) {
                        best = Math.min(best, segmentDistance(x, y, segment));
                    }
                }
            }
            return best;
        }

        private int bucket(float value) {
            return (int) Math.floor(value / bucketSize);
        }

        private static long key(int bucketX, int bucketY) {
            return ((long) bucketX << 32) ^ (bucketY & 0xffffffffL);
        }

        private static float segmentDistance(float x, float y, float[] segment) {
            float alongX = segment[2] - segment[0];
            float alongY = segment[3] - segment[1];
            float lengthSquared = alongX * alongX + alongY * alongY;
            float share = lengthSquared <= 0f ? 0f : Math.max(0f, Math.min(1f, ((x - segment[0]) * alongX + (y - segment[1]) * alongY) / lengthSquared));
            return (float) Math.hypot(x - segment[0] - alongX * share, y - segment[1] - alongY * share);
        }
    }

    private static boolean[] fill(List<float[]> outlines, float originX, float originY, float texelSize, int width, int height) {
        boolean[] inside = new boolean[width * height];
        List<Float> crossings = new ArrayList<>();
        for (int row = 0; row < height; row++) {
            float y = originY + (row + 0.5f) * texelSize;
            crossings.clear();
            for (float[] outline : outlines) {
                for (int index = 0, previous = outline.length - 2; index < outline.length; previous = index, index += 2) {
                    float ay = outline[index + 1];
                    float by = outline[previous + 1];
                    if ((ay > y) != (by > y)) {
                        float ax = outline[index];
                        float bx = outline[previous];
                        crossings.add(ax + (bx - ax) * (y - ay) / (by - ay));
                    }
                }
            }
            Collections.sort(crossings);
            for (int pair = 0; pair + 1 < crossings.size(); pair += 2) {
                int from = Math.max(0, (int) Math.ceil((crossings.get(pair) - originX) / texelSize - 0.5f));
                int to = Math.min(width - 1, (int) Math.floor((crossings.get(pair + 1) - originX) / texelSize - 0.5f));
                for (int column = from; column <= to; column++) {
                    inside[row * width + column] = true;
                }
            }
        }
        return inside;
    }

    private static float[] distances(boolean[] inside, int width, int height, float texelSize) {
        float[] squared = new float[width * height];
        for (int texel = 0; texel < squared.length; texel++) {
            squared[texel] = inside[texel] ? FAR : 0f;
        }
        float[] line = new float[Math.max(width, height)];
        float[] transformed = new float[line.length];
        int[] parabolas = new int[line.length];
        float[] bounds = new float[line.length + 1];
        for (int column = 0; column < width; column++) {
            for (int row = 0; row < height; row++) {
                line[row] = squared[row * width + column];
            }
            transform(line, height, transformed, parabolas, bounds);
            for (int row = 0; row < height; row++) {
                squared[row * width + column] = transformed[row];
            }
        }
        for (int row = 0; row < height; row++) {
            System.arraycopy(squared, row * width, line, 0, width);
            transform(line, width, transformed, parabolas, bounds);
            System.arraycopy(transformed, 0, squared, row * width, width);
        }
        float[] distances = new float[width * height];
        for (int texel = 0; texel < distances.length; texel++) {
            distances[texel] = inside[texel] ? Math.max(0f, ((float) Math.sqrt(squared[texel]) - 0.5f) * texelSize) : 0f;
        }
        return distances;
    }

    private static void transform(float[] line, int count, float[] out, int[] parabolas, float[] bounds) {
        int hull = 0;
        parabolas[0] = 0;
        bounds[0] = -Float.MAX_VALUE;
        bounds[1] = Float.MAX_VALUE;
        for (int at = 1; at < count; at++) {
            float crossing = crossing(line, parabolas[hull], at);
            while (crossing <= bounds[hull]) {
                hull--;
                crossing = crossing(line, parabolas[hull], at);
            }
            hull++;
            parabolas[hull] = at;
            bounds[hull] = crossing;
            bounds[hull + 1] = Float.MAX_VALUE;
        }
        hull = 0;
        for (int at = 0; at < count; at++) {
            while (bounds[hull + 1] < at) {
                hull++;
            }
            int nearest = parabolas[hull];
            out[at] = (at - nearest) * (at - nearest) + line[nearest];
        }
    }

    private static float crossing(float[] line, int from, int to) {
        return ((line[to] + (float) to * to) - (line[from] + (float) from * from)) / (2f * (to - from));
    }

    private static float[] smooth(float[] distances, boolean[] inside, int width, int height) {
        float[] current = distances;
        for (int pass = 0; pass < SMOOTHING_PASSES; pass++) {
            float[] next = new float[current.length];
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    int texel = row * width + column;
                    if (!inside[texel]) {
                        continue;
                    }
                    float sum = 0f;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int y = Math.max(0, Math.min(height - 1, row + dy));
                            int x = Math.max(0, Math.min(width - 1, column + dx));
                            sum += current[y * width + x];
                        }
                    }
                    next[texel] = sum / 9f;
                }
            }
            current = next;
        }
        return current;
    }

    float peak() {
        return peak;
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    float originX() {
        return originX;
    }

    float originY() {
        return originY;
    }

    float texelSize() {
        return texelSize;
    }

    float heightShareAt(float worldX, float worldY) {
        float column = (worldX - originX) / texelSize - 0.5f;
        float row = (worldY - originY) / texelSize - 0.5f;
        int left = (int) Math.floor(column);
        int bottom = (int) Math.floor(row);
        float across = column - left;
        float up = row - bottom;
        float lower = texel(heightShares, left, bottom) * (1f - across) + texel(heightShares, left + 1, bottom) * across;
        float upper = texel(heightShares, left, bottom + 1) * (1f - across) + texel(heightShares, left + 1, bottom + 1) * across;
        return lower * (1f - up) + upper * up;
    }

    float alphaAt(int column, int row) {
        return texel(alphas, column, row);
    }

    boolean coversAny(float minX, float minY, float maxX, float maxY) {
        int fromColumn = Math.max(0, (int) Math.floor((minX - originX) / texelSize) - 1);
        int toColumn = Math.min(width - 1, (int) Math.ceil((maxX - originX) / texelSize) + 1);
        int fromRow = Math.max(0, (int) Math.floor((minY - originY) / texelSize) - 1);
        int toRow = Math.min(height - 1, (int) Math.ceil((maxY - originY) / texelSize) + 1);
        for (int row = fromRow; row <= toRow; row++) {
            for (int column = fromColumn; column <= toColumn; column++) {
                if (alphas[row * width + column] > 0f) {
                    return true;
                }
            }
        }
        return false;
    }

    void writePixels(Color tint, ByteBuffer pixels, int rowLength) {
        float red = tint.getRed() / 255f;
        float green = tint.getGreen() / 255f;
        float blue = tint.getBlue() / 255f;
        for (int row = 0; row < height; row++) {
            pixels.position(row * rowLength * 4);
            for (int column = 0; column < width; column++) {
                int texel = row * width + column;
                float white = whiteness[texel];
                pixels.put(channel(red + (1f - red) * white));
                pixels.put(channel(green + (1f - green) * white));
                pixels.put(channel(blue + (1f - blue) * white));
                pixels.put(channel(alphas[texel]));
            }
        }
        pixels.position(0);
    }

    private static byte channel(float value) {
        return (byte) Math.round(Math.max(0f, Math.min(1f, value)) * 255f);
    }

    private float texel(float[] values, int column, int row) {
        if (column < 0 || row < 0 || column >= width || row >= height) {
            return 0f;
        }
        return values[row * width + column];
    }
}
