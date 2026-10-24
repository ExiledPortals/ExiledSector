import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public final class NebulaLayers {

    private static final int STAR_MEDIAN_RADIUS = 4;
    private static final float STAR_THRESHOLD = 14f;
    private static final float BLACK_FLOOR = 12f;
    private static final float FADE_START = 0.42f;
    private static final float FADE_END = 0.88f;
    private static final int GLOW_BLUR_RADIUS = 22;
    private static final int CLOUD_BLUR_RADIUS = 7;
    private static final float GLOW_GAIN = 1.0f;
    private static final float CLOUD_GAIN = 1.1f;
    private static final float FILAMENT_GAIN = 1.2f;

    private final int imageWidth;
    private final int imageHeight;

    private NebulaLayers(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println("usage: java tools/NebulaLayers.java <source.png> <glow.png> <clouds.png> <filaments.png>");
            System.exit(1);
        }
        BufferedImage source = ImageIO.read(new File(args[0]));
        NebulaLayers layers = new NebulaLayers(source.getWidth(), source.getHeight());
        float[][] nebula = layers.feathered(layers.withoutStars(channels(source)), alphas(source));
        int pixels = nebula[0].length;
        float[][] glow = new float[3][];
        float[][] clouds = new float[3][pixels];
        float[][] filaments = new float[3][pixels];
        for (int channel = 0; channel < 3; channel++) {
            glow[channel] = layers.blur(nebula[channel], GLOW_BLUR_RADIUS);
            float[] cloudBlur = layers.blur(nebula[channel], CLOUD_BLUR_RADIUS);
            for (int i = 0; i < pixels; i++) {
                clouds[channel][i] = Math.max(0f, cloudBlur[i] - glow[channel][i]) * CLOUD_GAIN;
                filaments[channel][i] = Math.max(0f, nebula[channel][i] - cloudBlur[i]) * FILAMENT_GAIN;
                glow[channel][i] *= GLOW_GAIN;
            }
        }
        ImageIO.write(layers.brightnessAsAlpha(glow), "png", new File(args[1]));
        ImageIO.write(layers.brightnessAsAlpha(clouds), "png", new File(args[2]));
        ImageIO.write(layers.brightnessAsAlpha(filaments), "png", new File(args[3]));
    }

    private static float[][] channels(BufferedImage image) {
        int width = image.getWidth();
        float[][] rgb = new float[3][width * image.getHeight()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                rgb[0][y * width + x] = (pixel >> 16) & 255;
                rgb[1][y * width + x] = (pixel >> 8) & 255;
                rgb[2][y * width + x] = pixel & 255;
            }
        }
        return rgb;
    }

    private static float[] alphas(BufferedImage image) {
        int width = image.getWidth();
        float[] alpha = new float[width * image.getHeight()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < width; x++) {
                alpha[y * width + x] = (image.getRGB(x, y) >>> 24) / 255f;
            }
        }
        return alpha;
    }

    private float[][] withoutStars(float[][] rgb) {
        float[][] median = new float[3][];
        for (int channel = 0; channel < 3; channel++) median[channel] = median(rgb[channel]);
        boolean[] star = new boolean[rgb[0].length];
        for (int i = 0; i < star.length; i++) {
            star[i] = brightness(rgb, i) - brightness(median, i) > STAR_THRESHOLD;
        }
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                if (!nearStar(star, x, y)) continue;
                for (int channel = 0; channel < 3; channel++) rgb[channel][y * imageWidth + x] = median[channel][y * imageWidth + x];
            }
        }
        return rgb;
    }

    private boolean nearStar(boolean[] star, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < imageWidth && ny < imageHeight && star[ny * imageWidth + nx]) return true;
            }
        }
        return false;
    }

    private float[][] feathered(float[][] rgb, float[] alpha) {
        double centerX = imageWidth / 2.0;
        double centerY = imageHeight / 2.0;
        double radius = Math.min(imageWidth, imageHeight) / 2.0;
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int i = y * imageWidth + x;
                double offsetX = (x - centerX) / radius;
                double offsetY = (y - centerY) / radius;
                double angle = Math.atan2(offsetY, offsetX);
                double wobble = 1 + 0.10 * Math.sin(3 * angle + 0.7) + 0.06 * Math.sin(7 * angle + 2.1) + 0.04 * Math.sin(13 * angle + 4.0);
                float edge = (1f - smoothstep(FADE_START, FADE_END, (float) (Math.hypot(offsetX, offsetY) * wobble))) * alpha[i];
                for (int channel = 0; channel < 3; channel++) {
                    rgb[channel][i] = Math.max(0f, rgb[channel][i] - BLACK_FLOOR) * 255f / (255f - BLACK_FLOOR) * edge;
                }
            }
        }
        return rgb;
    }

    private BufferedImage brightnessAsAlpha(float[][] rgb) {
        BufferedImage out = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int i = y * imageWidth + x;
                float peak = brightness(rgb, i);
                if (peak < 0.5f) continue;
                int alpha = Math.min(255, Math.round(peak));
                int red = Math.min(255, Math.round(rgb[0][i] * 255f / peak));
                int green = Math.min(255, Math.round(rgb[1][i] * 255f / peak));
                int blue = Math.min(255, Math.round(rgb[2][i] * 255f / peak));
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

    private float[] blur(float[] values, int radius) {
        float[] blurred = values;
        for (int pass = 0; pass < 3; pass++) blurred = boxBlur(boxBlur(blurred, radius, 1, 0), radius, 0, 1);
        return blurred;
    }

    private float[] boxBlur(float[] values, int radius, int stepX, int stepY) {
        float[] out = new float[values.length];
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                float sum = 0f;
                int count = 0;
                for (int step = -radius; step <= radius; step++) {
                    int nx = x + step * stepX;
                    int ny = y + step * stepY;
                    if (nx < 0 || ny < 0 || nx >= imageWidth || ny >= imageHeight) continue;
                    sum += values[ny * imageWidth + nx];
                    count++;
                }
                out[y * imageWidth + x] = sum / count;
            }
        }
        return out;
    }
}
