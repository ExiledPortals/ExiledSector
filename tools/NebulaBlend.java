import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;

public final class NebulaBlend {

    private static final int STAR_MEDIAN_RADIUS = 4;
    private static final float STAR_CONTRAST = Float.parseFloat(System.getProperty("starContrast", "28"));
    private static final int STAR_MAX_PIXELS = Integer.getInteger("starMaxPixels", 30);
    private static final float FADE_START = Float.parseFloat(System.getProperty("fadeStart", "0.6"));
    private static final float FADE_END = Float.parseFloat(System.getProperty("fadeEnd", "0.96"));
    private static final int LANCZOS_LOBES = 3;

    private final int imageWidth;
    private final int imageHeight;

    private NebulaBlend(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("usage: java tools/NebulaBlend.java <source.png> <out.png> <size> [starMask.png]");
            System.exit(1);
        }
        BufferedImage source = ImageIO.read(new File(args[0]));
        int outputSize = Integer.parseInt(args[2]);
        NebulaBlend sourceGrid = new NebulaBlend(source.getWidth(), source.getHeight());
        float[][] premultiplied = premultiplied(source);
        boolean[] starMask = sourceGrid.starMask(premultiplied);
        if (args.length > 3) ImageIO.write(sourceGrid.maskPreview(premultiplied, starMask), "png", new File(args[3]));
        sourceGrid.removeStars(premultiplied, starMask);

        NebulaBlend outputGrid = new NebulaBlend(outputSize, outputSize);
        float[][] scaled = new float[4][];
        for (int channel = 0; channel < 4; channel++) {
            scaled[channel] = outputGrid.resized(premultiplied[channel], source.getWidth(), source.getHeight());
        }
        outputGrid.fadeRim(scaled);
        ImageIO.write(outputGrid.brightnessAsAlpha(scaled), "png", new File(args[1]));
    }

    private static float[][] premultiplied(BufferedImage image) {
        int width = image.getWidth();
        float[][] rgba = new float[4][width * image.getHeight()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int i = y * width + x;
                float alpha = (pixel >>> 24) / 255f;
                rgba[0][i] = ((pixel >> 16) & 255) * alpha;
                rgba[1][i] = ((pixel >> 8) & 255) * alpha;
                rgba[2][i] = (pixel & 255) * alpha;
                rgba[3][i] = alpha;
            }
        }
        return rgba;
    }

    private boolean[] starMask(float[][] rgba) {
        float[] brightness = new float[imageWidth * imageHeight];
        for (int i = 0; i < brightness.length; i++) brightness[i] = brightness(rgba, i);
        float[] backdrop = median(brightness);
        boolean[] candidate = new boolean[brightness.length];
        for (int i = 0; i < candidate.length; i++) candidate[i] = brightness[i] - backdrop[i] > STAR_CONTRAST;

        boolean[] mask = new boolean[candidate.length];
        boolean[] visited = new boolean[candidate.length];
        int[] component = new int[candidate.length];
        ArrayDeque<Integer> frontier = new ArrayDeque<>();
        for (int start = 0; start < candidate.length; start++) {
            if (!candidate[start] || visited[start]) continue;
            int size = 0;
            visited[start] = true;
            frontier.add(start);
            while (!frontier.isEmpty()) {
                int i = frontier.poll();
                component[size++] = i;
                for (int neighbour : neighbours(i)) {
                    if (neighbour >= 0 && candidate[neighbour] && !visited[neighbour]) {
                        visited[neighbour] = true;
                        frontier.add(neighbour);
                    }
                }
            }
            if (size > STAR_MAX_PIXELS) continue;
            for (int k = 0; k < size; k++) {
                mask[component[k]] = true;
                for (int neighbour : neighbours(component[k])) if (neighbour >= 0) mask[neighbour] = true;
            }
        }
        return mask;
    }

    private int[] neighbours(int i) {
        int x = i % imageWidth;
        int y = i / imageWidth;
        int[] out = new int[8];
        int count = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                out[count++] = nx >= 0 && ny >= 0 && nx < imageWidth && ny < imageHeight ? ny * imageWidth + nx : -1;
            }
        }
        return out;
    }

    private void removeStars(float[][] rgba, boolean[] mask) {
        float[][] filled = new float[3][];
        for (int channel = 0; channel < 3; channel++) filled[channel] = maskedMedian(rgba[channel], mask);
        for (int i = 0; i < mask.length; i++) {
            if (!mask[i]) continue;
            for (int channel = 0; channel < 3; channel++) rgba[channel][i] = filled[channel][i];
        }
    }

    private float[] maskedMedian(float[] values, boolean[] mask) {
        float[] out = values.clone();
        float[] window = new float[(2 * STAR_MEDIAN_RADIUS + 1) * (2 * STAR_MEDIAN_RADIUS + 1)];
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                if (!mask[y * imageWidth + x]) continue;
                int count = 0;
                for (int dy = -STAR_MEDIAN_RADIUS; dy <= STAR_MEDIAN_RADIUS; dy++) {
                    for (int dx = -STAR_MEDIAN_RADIUS; dx <= STAR_MEDIAN_RADIUS; dx++) {
                        int nx = Math.min(imageWidth - 1, Math.max(0, x + dx));
                        int ny = Math.min(imageHeight - 1, Math.max(0, y + dy));
                        if (!mask[ny * imageWidth + nx]) window[count++] = values[ny * imageWidth + nx];
                    }
                }
                if (count == 0) continue;
                Arrays.sort(window, 0, count);
                out[y * imageWidth + x] = window[count / 2];
            }
        }
        return out;
    }

    private float[] median(float[] values) {
        float[] out = new float[values.length];
        float[] window = new float[(2 * STAR_MEDIAN_RADIUS + 1) * (2 * STAR_MEDIAN_RADIUS + 1)];
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int count = 0;
                for (int dy = -STAR_MEDIAN_RADIUS; dy <= STAR_MEDIAN_RADIUS; dy++) {
                    for (int dx = -STAR_MEDIAN_RADIUS; dx <= STAR_MEDIAN_RADIUS; dx++) {
                        int nx = Math.min(imageWidth - 1, Math.max(0, x + dx));
                        int ny = Math.min(imageHeight - 1, Math.max(0, y + dy));
                        window[count++] = values[ny * imageWidth + nx];
                    }
                }
                Arrays.sort(window, 0, count);
                out[y * imageWidth + x] = window[count / 2];
            }
        }
        return out;
    }

    private BufferedImage maskPreview(float[][] rgba, boolean[] mask) {
        BufferedImage out = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < mask.length; i++) {
            int grey = Math.min(255, Math.round(brightness(rgba, i) * 0.6f));
            out.setRGB(i % imageWidth, i / imageWidth, mask[i] ? 0xFF00FF : grey << 16 | grey << 8 | grey);
        }
        return out;
    }

    private float[] resized(float[] values, int sourceWidth, int sourceHeight) {
        float[] horizontal = new float[imageWidth * sourceHeight];
        float scaleX = (float) sourceWidth / imageWidth;
        for (int x = 0; x < imageWidth; x++) {
            Taps taps = taps((x + 0.5f) * scaleX - 0.5f, scaleX, sourceWidth);
            for (int y = 0; y < sourceHeight; y++) {
                float sum = 0f;
                for (int k = 0; k < taps.indices.length; k++) sum += values[y * sourceWidth + taps.indices[k]] * taps.weights[k];
                horizontal[y * imageWidth + x] = sum;
            }
        }
        float[] out = new float[imageWidth * imageHeight];
        float scaleY = (float) sourceHeight / imageHeight;
        for (int y = 0; y < imageHeight; y++) {
            Taps taps = taps((y + 0.5f) * scaleY - 0.5f, scaleY, sourceHeight);
            for (int x = 0; x < imageWidth; x++) {
                float sum = 0f;
                for (int k = 0; k < taps.indices.length; k++) sum += horizontal[taps.indices[k] * imageWidth + x] * taps.weights[k];
                out[y * imageWidth + x] = Math.max(0f, sum);
            }
        }
        return out;
    }

    private record Taps(int[] indices, float[] weights) {
    }

    private static Taps taps(float center, float scale, int length) {
        float support = LANCZOS_LOBES * Math.max(1f, scale);
        int first = (int) Math.floor(center - support) + 1;
        int last = (int) Math.floor(center + support);
        int[] indices = new int[last - first + 1];
        float[] weights = new float[indices.length];
        float total = 0f;
        for (int k = 0; k < indices.length; k++) {
            int index = first + k;
            float weight = lanczos((index - center) / Math.max(1f, scale));
            indices[k] = Math.min(length - 1, Math.max(0, index));
            weights[k] = weight;
            total += weight;
        }
        for (int k = 0; k < weights.length; k++) weights[k] /= total;
        return new Taps(indices, weights);
    }

    private static float lanczos(float distance) {
        if (distance == 0f) return 1f;
        if (Math.abs(distance) >= LANCZOS_LOBES) return 0f;
        double angle = Math.PI * distance;
        return (float) (LANCZOS_LOBES * Math.sin(angle) * Math.sin(angle / LANCZOS_LOBES) / (angle * angle));
    }

    private void fadeRim(float[][] rgba) {
        double centerX = imageWidth / 2.0;
        double centerY = imageHeight / 2.0;
        double radius = Math.min(imageWidth, imageHeight) / 2.0;
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int i = y * imageWidth + x;
                double offsetX = (x + 0.5 - centerX) / radius;
                double offsetY = (y + 0.5 - centerY) / radius;
                double angle = Math.atan2(offsetY, offsetX);
                double wobble = 1 + 0.06 * Math.sin(3 * angle + 0.7) + 0.035 * Math.sin(7 * angle + 2.1) + 0.02 * Math.sin(13 * angle + 4.0);
                float edge = 1f - smoothstep(FADE_START, FADE_END, (float) (Math.hypot(offsetX, offsetY) * wobble));
                for (int channel = 0; channel < 3; channel++) rgba[channel][i] *= edge;
            }
        }
    }

    private BufferedImage brightnessAsAlpha(float[][] rgb) {
        BufferedImage out = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int i = y * imageWidth + x;
                float peak = Math.min(255f, brightness(rgb, i));
                if (peak < 0.5f) continue;
                int alpha = Math.round(peak);
                int red = Math.min(255, Math.round(rgb[0][i] * 255f / alpha));
                int green = Math.min(255, Math.round(rgb[1][i] * 255f / alpha));
                int blue = Math.min(255, Math.round(rgb[2][i] * 255f / alpha));
                out.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        return out;
    }

    private static float brightness(float[][] rgb, int i) {
        return Math.max(rgb[0][i], Math.max(rgb[1][i], rgb[2][i]));
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Math.max(0f, Math.min(1f, (value - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }
}
