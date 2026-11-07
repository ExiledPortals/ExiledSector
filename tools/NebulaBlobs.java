import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class NebulaBlobs {

    private static final int BLOB_CANVAS = 1024;
    private static final int BLOBS_PER_LAYER = 5;
    private static final double BACK_RADIUS_FRACTION = Double.parseDouble(System.getProperty("backRadius", "0.18"));
    private static final double FRONT_RADIUS_FRACTION = Double.parseDouble(System.getProperty("frontRadius", "0.12"));
    private static final float BACK_BRIGHTNESS = Float.parseFloat(System.getProperty("backBrightness", "0.7"));
    private static final float FRONT_OPACITY = Float.parseFloat(System.getProperty("frontOpacity", "0.9"));
    private static final double FEATHER_FRACTION = 0.14;
    private static final double CANVAS_EDGE_FADE = 24;
    private static final double MAX_RADIUS = BLOB_CANVAS / 2.0 / 1.45;

    record Blob(BufferedImage image, double sourceX, double sourceY, double radius) {
    }

    record Placement(Blob blob, double imageX, double imageY) {
    }

    private final int imageWidth;
    private final int imageHeight;
    private final float[] red;
    private final float[] green;
    private final float[] blue;
    private final float[] alpha;
    private final float[] brightness;
    private final Random random;

    private NebulaBlobs(BufferedImage source, long seed) {
        imageWidth = source.getWidth();
        imageHeight = source.getHeight();
        int pixels = imageWidth * imageHeight;
        red = new float[pixels];
        green = new float[pixels];
        blue = new float[pixels];
        alpha = new float[pixels];
        brightness = new float[pixels];
        for (int i = 0; i < pixels; i++) {
            int pixel = source.getRGB(i % imageWidth, i / imageWidth);
            alpha[i] = (pixel >>> 24) / 255f;
            red[i] = ((pixel >> 16) & 255) / 255f * alpha[i];
            green[i] = ((pixel >> 8) & 255) / 255f * alpha[i];
            blue[i] = (pixel & 255) / 255f * alpha[i];
            brightness[i] = Math.max(red[i], Math.max(green[i], blue[i]));
        }
        random = new Random(seed);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 9) {
            System.err.println("usage: java tools/NebulaBlobs.java <nebula.png> <outputDir> <idPrefix> <seed> <treeX> <treeY> <treeWidth> <rotation> <thickness>");
            System.exit(1);
        }
        NebulaBlobs nebula = new NebulaBlobs(ImageIO.read(new File(args[0])), Long.parseLong(args[3]));
        File outputDir = new File(args[1]);
        outputDir.mkdirs();
        String idPrefix = args[2];

        double backRadius = nebula.imageWidth * BACK_RADIUS_FRACTION;
        List<Blob> backBlobs = new ArrayList<>();
        for (double[] source : nebula.pickSources(BLOBS_PER_LAYER, backRadius, List.of())) {
            backBlobs.add(nebula.cutBlob(source[0], source[1], backRadius * (0.85 + nebula.random.nextDouble() * 0.3), nebula.randomTurn()));
        }
        List<double[]> backSources = backBlobs.stream().map(blob -> new double[]{blob.sourceX(), blob.sourceY()}).toList();
        double frontRadius = nebula.imageWidth * FRONT_RADIUS_FRACTION;
        List<Blob> frontBlobs = new ArrayList<>();
        for (double[] source : nebula.pickSources(BLOBS_PER_LAYER, frontRadius, backSources)) {
            frontBlobs.add(nebula.cutBlob(source[0], source[1], frontRadius * (0.8 + nebula.random.nextDouble() * 0.4), nebula.randomTurn()));
        }
        List<Placement> backPlacements = nebula.placeByColour(backBlobs, 0.15, 0.9, 0.25, List.of());
        List<Placement> frontPlacements = nebula.placeByColour(frontBlobs, 0.3, 1.01, 0.45, backPlacements);

        TreePlacement tree = new TreePlacement(Double.parseDouble(args[4]), Double.parseDouble(args[5]),
                Double.parseDouble(args[6]) / nebula.imageWidth, Double.parseDouble(args[7]), nebula.imageWidth, nebula.imageHeight);
        double thickness = Double.parseDouble(args[8]);
        List<String> entries = new ArrayList<>();
        entries.addAll(write(backPlacements, "back", idPrefix, outputDir, tree, 1 - thickness, image -> scaled(image, BACK_BRIGHTNESS, 1f)));
        entries.addAll(write(frontPlacements, "front", idPrefix, outputDir, tree, 1 + thickness, image -> scaled(image, 1f, FRONT_OPACITY)));
        System.out.println("[\n" + String.join(",\n", entries) + "\n]");
    }

    record TreePlacement(double centerX, double centerY, double unitsPerPixel, double rotationDeg, int imageWidth, int imageHeight) {
        double[] treePoint(double imageX, double imageY) {
            double offsetX = (imageX - imageWidth / 2.0) * unitsPerPixel;
            double offsetY = (imageY - imageHeight / 2.0) * unitsPerPixel;
            double turn = Math.toRadians(rotationDeg);
            return new double[]{
                    centerX + offsetX * Math.cos(turn) - offsetY * Math.sin(turn),
                    centerY + offsetX * Math.sin(turn) + offsetY * Math.cos(turn)};
        }
    }

    interface ImageFinish {
        BufferedImage apply(BufferedImage image);
    }

    private static List<String> write(List<Placement> placements, String layer, String idPrefix, File outputDir, TreePlacement tree,
                                      double parallax, ImageFinish finish) throws IOException {
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < placements.size(); i++) {
            Placement placement = placements.get(i);
            String id = idPrefix + "_" + layer + "_" + (i + 1);
            File file = new File(outputDir, id + ".png");
            ImageIO.write(finish.apply(placement.blob().image()), "png", file);
            double[] point = tree.treePoint(placement.imageX(), placement.imageY());
            double size = BLOB_CANVAS * tree.unitsPerPixel();
            entries.add(String.format(Locale.ROOT,
                    "{\"id\": \"%s\", \"x\": %d, \"y\": %d, \"width\": %d, \"height\": %d, \"file\": \"%s\", \"rotation\": %s, \"parallax\": %s}",
                    id, Math.round(point[0]), Math.round(point[1]), Math.round(size), Math.round(size), file.getName(),
                    trimmed(tree.rotationDeg()), trimmed(parallax)));
        }
        return entries;
    }

    private static String trimmed(double value) {
        return String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static BufferedImage scaled(BufferedImage image, float colourScale, float alphaScale) {
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                int a = Math.round((pixel >>> 24) * alphaScale);
                int r = Math.round(((pixel >> 16) & 255) * colourScale);
                int g = Math.round(((pixel >> 8) & 255) * colourScale);
                int b = Math.round((pixel & 255) * colourScale);
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    private double randomTurn() {
        return Math.toRadians(40 + random.nextDouble() * 280);
    }

    private double[] samplesIn(float[] values, double centerX, double centerY, double radius) {
        List<Double> samples = new ArrayList<>();
        int step = Math.max(2, (int) (radius / 14));
        for (int y = (int) (centerY - radius); y <= centerY + radius; y += step) {
            for (int x = (int) (centerX - radius); x <= centerX + radius; x += step) {
                if ((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY) > radius * radius) continue;
                samples.add(x < 0 || y < 0 || x >= imageWidth || y >= imageHeight ? 0.0 : values[y * imageWidth + x]);
            }
        }
        double[] sorted = samples.stream().mapToDouble(Double::doubleValue).toArray();
        Arrays.sort(sorted);
        return sorted;
    }

    private static double percentile(double[] sorted, double fraction) {
        return sorted.length == 0 ? 0 : sorted[(int) Math.min(sorted.length - 1, sorted.length * fraction)];
    }

    private static double mean(double[] values) {
        double sum = 0;
        for (double value : values) sum += value;
        return values.length == 0 ? 0 : sum / values.length;
    }

    private double[] meanColour(double centerX, double centerY, double radius) {
        return new double[]{mean(samplesIn(red, centerX, centerY, radius)), mean(samplesIn(green, centerX, centerY, radius)), mean(samplesIn(blue, centerX, centerY, radius))};
    }

    private static int dominantChannel(double[] colour) {
        if (colour[0] >= colour[1] && colour[0] >= colour[2]) return 0;
        return colour[1] >= colour[2] ? 1 : 2;
    }

    private List<double[]> pickSources(int count, double radius, List<double[]> avoid) {
        List<double[]> candidates = new ArrayList<>();
        int step = (int) (radius / 3);
        for (int y = step; y < imageHeight; y += step) {
            for (int x = step; x < imageWidth; x += step) {
                if (percentile(samplesIn(alpha, x, y, radius * 0.7), 0.1) < 0.05) continue;
                double[] brightnesses = samplesIn(brightness, x, y, radius * 0.7);
                double score = percentile(brightnesses, 0.2) * 0.6 + mean(brightnesses) * 0.4;
                candidates.add(new double[]{x, y, score, dominantChannel(meanColour(x, y, radius * 0.7))});
            }
        }
        candidates.sort((first, second) -> Double.compare(second[2], first[2]));
        double bestScore = candidates.isEmpty() ? 0 : candidates.get(0)[2];
        List<double[]> chosen = new ArrayList<>();
        boolean added = true;
        while (chosen.size() < count && added) {
            added = false;
            for (int channel = 0; channel < 3 && chosen.size() < count; channel++) {
                for (double[] candidate : candidates) {
                    if (candidate[3] != channel || candidate[2] < bestScore * 0.3) continue;
                    boolean apart = chosen.stream().allMatch(other -> Math.hypot(other[0] - candidate[0], other[1] - candidate[1]) > radius * 1.1)
                            && avoid.stream().allMatch(other -> Math.hypot(other[0] - candidate[0], other[1] - candidate[1]) > radius * 0.6);
                    if (apart) {
                        chosen.add(candidate);
                        added = true;
                        break;
                    }
                }
            }
        }
        return chosen;
    }

    private Blob cutBlob(double sourceX, double sourceY, double requestedRadius, double turn) {
        double radius = Math.min(requestedRadius, MAX_RADIUS);
        int lobeCount = 7 + random.nextInt(4);
        double[][] lobes = new double[lobeCount + 1][];
        lobes[0] = new double[]{0, 0, radius * 0.5};
        for (int i = 1; i <= lobeCount; i++) {
            double direction = random.nextDouble() * Math.PI * 2;
            double distance = radius * (0.2 + random.nextDouble() * 0.35);
            lobes[i] = new double[]{Math.cos(direction) * distance, Math.sin(direction) * distance, radius * (0.18 + random.nextDouble() * 0.22)};
        }
        Noise edgeNoise = new Noise(random.nextLong());
        Noise warpX = new Noise(random.nextLong());
        Noise warpY = new Noise(random.nextLong());
        Noise wisps = new Noise(random.nextLong());
        double peakBrightness = Math.max(1e-3, percentile(samplesIn(brightness, sourceX, sourceY, radius), 0.9));
        double centre = BLOB_CANVAS / 2.0;
        double noiseScale = 4.0 / radius;

        float[] silhouette = new float[BLOB_CANVAS * BLOB_CANVAS];
        for (int y = 0; y < BLOB_CANVAS; y++) {
            for (int x = 0; x < BLOB_CANVAS; x++) {
                double warp = radius * 0.22;
                double warpedX = x - centre + (warpX.fractal(x * noiseScale * 0.5, y * noiseScale * 0.5) - 0.5) * 2 * warp;
                double warpedY = y - centre + (warpY.fractal(x * noiseScale * 0.5, y * noiseScale * 0.5) - 0.5) * 2 * warp;
                double field = 0;
                for (double[] lobe : lobes) {
                    double distance = Math.hypot(warpedX - lobe[0], warpedY - lobe[1]) / lobe[2];
                    field += Math.exp(-distance * distance);
                }
                field += (edgeNoise.fractal(x * noiseScale * 1.5, y * noiseScale * 1.5) - 0.5) * 0.3;
                silhouette[y * BLOB_CANVAS + x] = field > 0.5 ? 1f : 0f;
            }
        }
        float[] mask = blur(silhouette, Math.max(1, (int) Math.round(radius * FEATHER_FRACTION)));

        double cos = Math.cos(turn);
        double sin = Math.sin(turn);
        BufferedImage blob = new BufferedImage(BLOB_CANVAS, BLOB_CANVAS, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < BLOB_CANVAS; y++) {
            for (int x = 0; x < BLOB_CANVAS; x++) {
                double edgeDistance = Math.min(Math.min(x, y), Math.min(BLOB_CANVAS - 1 - x, BLOB_CANVAS - 1 - y));
                double shape = mask[y * BLOB_CANVAS + x] * smoothstep(0, CANVAS_EDGE_FADE, edgeDistance);
                if (shape < 0.003) continue;
                double offsetX = x - centre;
                double offsetY = y - centre;
                double sampleX = sourceX + offsetX * cos + offsetY * sin;
                double sampleY = sourceY - offsetX * sin + offsetY * cos;
                if (sampleX < 0 || sampleY < 0 || sampleX > imageWidth - 1 || sampleY > imageHeight - 1) continue;
                double sourceAlpha = Math.min(1, sample(alpha, sampleX, sampleY));
                if (sourceAlpha <= 0) continue;
                double fringe = 1 - smoothstep(0.2, 0.95, shape);
                double detail = smoothstep(0.1, 0.9, sample(brightness, sampleX, sampleY) / peakBrightness);
                double drift = wisps.fractal(x * noiseScale * 3, y * noiseScale * 3);
                double dispersal = Math.max(0, 1 - fringe * (1 - detail) * 0.6 - fringe * (1 - drift) * 0.6);
                double coverage = Math.min(1, sourceAlpha * shape * dispersal);
                if (coverage <= 0.002) continue;
                int a = (int) Math.round(coverage * 255);
                int r = (int) Math.round(Math.min(1, sample(red, sampleX, sampleY) / sourceAlpha) * 255);
                int g = (int) Math.round(Math.min(1, sample(green, sampleX, sampleY) / sourceAlpha) * 255);
                int b = (int) Math.round(Math.min(1, sample(blue, sampleX, sampleY) / sourceAlpha) * 255);
                blob.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return new Blob(blob, sourceX, sourceY, radius);
    }

    private List<Placement> placeByColour(List<Blob> blobs, double minCoverage, double maxCoverage, double minBrightnessRatio, List<Placement> others) {
        List<Placement> chosen = new ArrayList<>();
        for (Blob blob : blobs) {
            double[] colour = blobColour(blob.image());
            double colourBrightness = Math.max(colour[0], Math.max(colour[1], colour[2]));
            double radius = blob.radius();
            List<double[]> candidates = new ArrayList<>();
            double closestColour = Double.MAX_VALUE;
            for (int attempt = 0; attempt < 1500; attempt++) {
                double x = radius * 0.5 + random.nextDouble() * (imageWidth - radius);
                double y = radius * 0.5 + random.nextDouble() * (imageHeight - radius);
                if (Math.hypot(x - blob.sourceX(), y - blob.sourceY()) < radius * 0.9) continue;
                double coverage = mean(samplesIn(alpha, x, y, radius * 0.6));
                if (coverage < minCoverage || coverage > maxCoverage) continue;
                double[] here = meanColour(x, y, radius * 0.7);
                if (Math.max(here[0], Math.max(here[1], here[2])) < colourBrightness * minBrightnessRatio) continue;
                double colourDistance = chromaDistance(colour, here);
                closestColour = Math.min(closestColour, colourDistance);
                candidates.add(new double[]{x, y, colourDistance});
            }
            double[] best = null;
            double bestSpread = -1;
            for (double[] candidate : candidates) {
                if (candidate[2] > closestColour + 0.05) continue;
                double nearest = imageWidth;
                for (Placement other : chosen) nearest = Math.min(nearest, Math.hypot(other.imageX() - candidate[0], other.imageY() - candidate[1]));
                for (Placement other : others) nearest = Math.min(nearest, Math.hypot(other.imageX() - candidate[0], other.imageY() - candidate[1]) * 1.5);
                if (nearest > bestSpread) {
                    bestSpread = nearest;
                    best = candidate;
                }
            }
            chosen.add(best == null ? new Placement(blob, blob.sourceX(), blob.sourceY()) : new Placement(blob, best[0], best[1]));
        }
        return chosen;
    }

    private static double[] blobColour(BufferedImage image) {
        double[] sum = new double[4];
        for (int y = 0; y < image.getHeight(); y += 2) {
            for (int x = 0; x < image.getWidth(); x += 2) {
                int pixel = image.getRGB(x, y);
                double a = (pixel >>> 24) / 255.0;
                sum[0] += ((pixel >> 16) & 255) / 255.0 * a;
                sum[1] += ((pixel >> 8) & 255) / 255.0 * a;
                sum[2] += (pixel & 255) / 255.0 * a;
                sum[3] += a;
            }
        }
        return new double[]{sum[0] / sum[3], sum[1] / sum[3], sum[2] / sum[3]};
    }

    private static double chromaDistance(double[] first, double[] second) {
        double firstTotal = first[0] + first[1] + first[2] + 1e-6;
        double secondTotal = second[0] + second[1] + second[2] + 1e-6;
        double distance = 0;
        for (int channel = 0; channel < 3; channel++) distance += Math.abs(first[channel] / firstTotal - second[channel] / secondTotal);
        return distance;
    }

    private double sample(float[] values, double x, double y) {
        int column = (int) Math.floor(x);
        int row = (int) Math.floor(y);
        double[] rows = new double[4];
        for (int j = -1; j <= 2; j++) {
            int sampleRow = Math.min(imageHeight - 1, Math.max(0, row + j));
            double[] across = new double[4];
            for (int i = -1; i <= 2; i++) across[i + 1] = values[sampleRow * imageWidth + Math.min(imageWidth - 1, Math.max(0, column + i))];
            rows[j + 1] = cubic(across, x - column);
        }
        return Math.max(0, cubic(rows, y - row));
    }

    private static double cubic(double[] p, double t) {
        return p[1] + 0.5 * t * (p[2] - p[0] + t * (2 * p[0] - 5 * p[1] + 4 * p[2] - p[3] + t * (3 * (p[1] - p[2]) + p[3] - p[0])));
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = Math.max(0, Math.min(1, (value - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    private static float[] blur(float[] values, int radius) {
        float[] blurred = values;
        for (int pass = 0; pass < 3; pass++) blurred = boxBlur(boxBlur(blurred, radius, true), radius, false);
        return blurred;
    }

    private static float[] boxBlur(float[] values, int radius, boolean alongRows) {
        float[] out = new float[values.length];
        for (int line = 0; line < BLOB_CANVAS; line++) {
            double sum = 0;
            for (int i = -radius; i < BLOB_CANVAS + radius; i++) {
                int entering = i + radius;
                int leaving = i - radius - 1;
                if (entering < BLOB_CANVAS) sum += values[index(line, entering, alongRows)];
                if (leaving >= 0 && leaving < BLOB_CANVAS) sum -= values[index(line, leaving, alongRows)];
                if (i >= 0 && i < BLOB_CANVAS) out[index(line, i, alongRows)] = (float) (sum / (2 * radius + 1));
            }
        }
        return out;
    }

    private static int index(int line, int position, boolean alongRows) {
        return alongRows ? line * BLOB_CANVAS + position : position * BLOB_CANVAS + line;
    }

    private static final class Noise {
        private final long seed;

        Noise(long seed) {
            this.seed = seed;
        }

        double fractal(double x, double y) {
            return value(x, y) * 0.55 + value(x * 2.1, y * 2.1) * 0.3 + value(x * 4.3, y * 4.3) * 0.15;
        }

        private double value(double x, double y) {
            int column = (int) Math.floor(x);
            int row = (int) Math.floor(y);
            double across = smoothstep(0, 1, x - column);
            double down = smoothstep(0, 1, y - row);
            double top = hash(column, row) + (hash(column + 1, row) - hash(column, row)) * across;
            double bottom = hash(column, row + 1) + (hash(column + 1, row + 1) - hash(column, row + 1)) * across;
            return top + (bottom - top) * down;
        }

        private double hash(int x, int y) {
            long mixed = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL);
            mixed = (mixed ^ (mixed >>> 31)) * 0xBF58476D1CE4E5B9L;
            mixed ^= mixed >>> 29;
            return (mixed & 0xFFFFFF) / (double) 0xFFFFFF;
        }
    }
}
