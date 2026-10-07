import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FrameworkIcons {

    private static final int WORK_SIZE = 512;
    private static final int ICON_SIZE = 128;
    private static final int GRID_SPACING = 24;
    private static final String DEFAULT_SHIPS_DIR = "E:/Dev/Starsector/starsector-core/graphics/ships";
    private static final String DEFAULT_OUTPUT_DIR = "graphics/icons/frameworks";
    private static final String[] HULL_SIZES = {"frigate", "destroyer", "cruiser", "capital"};
    private static final String[] DEFAULT_SPRITES = {
            "wolf/wolf_base.png", "hammerhead/hammerhead_base.png", "dominator/dominator_base.png", "onslaught/onslaught_base.png"};
    private static final Map<String, Color> RARITY_COLORS = new LinkedHashMap<>();

    static {
        RARITY_COLORS.put("common", new Color(120, 150, 255));
        RARITY_COLORS.put("rare", new Color(255, 225, 90));
        RARITY_COLORS.put("unique", new Color(255, 140, 40));
    }

    private final float[] tint;

    private FrameworkIcons(Color rarityColor) {
        this.tint = rarityColor.getRGBColorComponents(null);
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> options = parseOptions(args);
        File shipsDir = new File(options.getOrDefault("ships", DEFAULT_SHIPS_DIR));
        File outputDir = new File(options.getOrDefault("out", DEFAULT_OUTPUT_DIR));
        boolean overwrite = options.containsKey("overwrite");
        outputDir.mkdirs();

        List<BufferedImage> previewIcons = new ArrayList<>();
        for (Map.Entry<String, Color> rarity : RARITY_COLORS.entrySet()) {
            FrameworkIcons generator = new FrameworkIcons(rarity.getValue());
            for (int sizeIndex = 0; sizeIndex < HULL_SIZES.length; sizeIndex++) {
                String hullSize = HULL_SIZES[sizeIndex];
                File spriteFile = resolveSprite(shipsDir, options.getOrDefault(hullSize, DEFAULT_SPRITES[sizeIndex]));
                BufferedImage sprite = ImageIO.read(spriteFile);
                if (sprite == null) {
                    throw new IOException("Not a readable image: " + spriteFile);
                }
                BufferedImage icon = downsample(generator.render(sprite, sizeIndex + 1), WORK_SIZE / ICON_SIZE);
                File iconFile = new File(outputDir, "framework_" + hullSize + "_" + rarity.getKey() + ".png");
                if (iconFile.exists() && !overwrite) {
                    throw new IOException(iconFile + " already exists; pass --overwrite to replace it");
                }
                ImageIO.write(icon, "png", iconFile);
                previewIcons.add(icon);
                System.out.println("Wrote " + iconFile + " from " + spriteFile);
            }
        }
        if (options.containsKey("preview")) {
            File previewFile = new File(options.get("preview"));
            ImageIO.write(previewSheet(previewIcons), "png", previewFile);
            System.out.println("Wrote " + previewFile);
        }
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument " + args[i] + "\n" + usage());
            }
            String name = args[i].substring(2);
            if (name.equals("overwrite")) {
                options.put(name, "true");
            } else if (name.equals("help")) {
                System.out.println(usage());
                System.exit(0);
            } else if (i + 1 < args.length) {
                options.put(name, args[++i]);
            } else {
                throw new IllegalArgumentException("Missing value for --" + name + "\n" + usage());
            }
        }
        return options;
    }

    private static String usage() {
        return "java tools/FrameworkIcons.java [--ships <dir>] [--out <dir>] [--frigate <sprite>] [--destroyer <sprite>]\n"
                + "    [--cruiser <sprite>] [--capital <sprite>] [--preview <file.png>] [--overwrite]\n"
                + "Sprites are relative to --ships (default " + DEFAULT_SHIPS_DIR + ") or absolute.\n"
                + "Defaults: " + String.join(", ", DEFAULT_SPRITES) + ". Output: " + DEFAULT_OUTPUT_DIR + ".";
    }

    private static File resolveSprite(File shipsDir, String spritePath) {
        File spriteFile = new File(spritePath);
        return spriteFile.isAbsolute() ? spriteFile : new File(shipsDir, spritePath);
    }

    private static BufferedImage previewSheet(List<BufferedImage> icons) {
        int columns = HULL_SIZES.length;
        int rows = (icons.size() + columns - 1) / columns;
        int gap = 10;
        BufferedImage sheet = new BufferedImage(columns * (ICON_SIZE + gap) + gap, rows * (ICON_SIZE + gap) + gap, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sheetGraphics = sheet.createGraphics();
        sheetGraphics.setColor(new Color(20, 24, 30));
        sheetGraphics.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        for (int i = 0; i < icons.size(); i++) {
            sheetGraphics.drawImage(icons.get(i), gap + (i % columns) * (ICON_SIZE + gap), gap + (i / columns) * (ICON_SIZE + gap), null);
        }
        sheetGraphics.dispose();
        return sheet;
    }

    private BufferedImage render(BufferedImage sprite, int sizePips) {
        BufferedImage hull = placeHull(sprite);
        float[] alpha = new float[WORK_SIZE * WORK_SIZE];
        float[] luminance = new float[WORK_SIZE * WORK_SIZE];
        for (int y = 0; y < WORK_SIZE; y++) {
            for (int x = 0; x < WORK_SIZE; x++) {
                int argb = hull.getRGB(x, y);
                float pixelAlpha = (argb >>> 24) / 255f;
                float red = ((argb >> 16) & 255) / 255f;
                float green = ((argb >> 8) & 255) / 255f;
                float blue = (argb & 255) / 255f;
                alpha[y * WORK_SIZE + x] = pixelAlpha;
                luminance[y * WORK_SIZE + x] = (0.299f * red + 0.587f * green + 0.114f * blue) * pixelAlpha;
            }
        }
        float[] panelLines = normalise(sobel(blur(luminance, 1)), 0.97f);
        float[] outline = normalise(sobel(alpha), 0.995f);
        float[] glow = blur(blur(outline, 6), 6);
        float[] solid = new float[alpha.length];
        for (int i = 0; i < alpha.length; i++) {
            solid[i] = smoothstep(0.35f, 0.65f, alpha[i]);
        }

        BufferedImage canvas = new BufferedImage(WORK_SIZE, WORK_SIZE, BufferedImage.TYPE_INT_ARGB);
        float panelInset = 10f;
        RoundRectangle2D panelShape = new RoundRectangle2D.Float(panelInset, panelInset, WORK_SIZE - 2 * panelInset, WORK_SIZE - 2 * panelInset, 44f, 44f);
        Graphics2D panelGraphics = antialiased(canvas);
        panelGraphics.setPaint(new GradientPaint(0, panelInset, tinted(0.16f, 0f, 240), 0, WORK_SIZE - panelInset, tinted(0.06f, 0f, 240)));
        panelGraphics.fill(panelShape);
        panelGraphics.dispose();

        for (int y = 0; y < WORK_SIZE; y++) {
            for (int x = 0; x < WORK_SIZE; x++) {
                if ((canvas.getRGB(x, y) >>> 24) == 0) {
                    continue;
                }
                int index = y * WORK_SIZE + x;
                boolean onGrid = (x - WORK_SIZE / 2) % GRID_SPACING == 0 || (y - WORK_SIZE / 2) % GRID_SPACING == 0;
                float grid = onGrid ? 0.05f + 0.20f * solid[index] : 0f;
                float intensity = 0.13f * solid[index] + grid
                        + 0.55f * smoothstep(0.18f, 0.7f, panelLines[index]) * solid[index]
                        + 1.10f * smoothstep(0.08f, 0.5f, outline[index])
                        + 0.55f * Math.min(1f, glow[index] * 2.2f);
                addLight(canvas, x, y, intensity);
            }
        }
        drawChrome(canvas, panelShape, sizePips);
        return canvas;
    }

    private static BufferedImage placeHull(BufferedImage sprite) {
        float hullTop = 0.10f * WORK_SIZE;
        float hullBottom = 0.82f * WORK_SIZE;
        float boxSize = Math.min(hullBottom - hullTop, 0.80f * WORK_SIZE);
        float scale = Math.min(boxSize / sprite.getWidth(), boxSize / sprite.getHeight());
        int drawnWidth = Math.round(sprite.getWidth() * scale);
        int drawnHeight = Math.round(sprite.getHeight() * scale);
        BufferedImage hull = new BufferedImage(WORK_SIZE, WORK_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D hullGraphics = hull.createGraphics();
        hullGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        hullGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        hullGraphics.drawImage(sprite, (WORK_SIZE - drawnWidth) / 2, Math.round(hullTop + (hullBottom - hullTop - drawnHeight) / 2f),
                drawnWidth, drawnHeight, null);
        hullGraphics.dispose();
        return hull;
    }

    private void drawChrome(BufferedImage canvas, RoundRectangle2D panelShape, int sizePips) {
        Graphics2D chrome = antialiased(canvas);
        chrome.setColor(tinted(1f, 0f, 120));
        chrome.setStroke(new BasicStroke(4f));
        chrome.draw(panelShape);
        chrome.setColor(tinted(1f, 0.35f, 235));
        chrome.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float bracketInset = 30f;
        float bracketLength = 62f;
        float farEdge = WORK_SIZE - bracketInset;
        drawBracket(chrome, bracketInset, bracketInset, 1, 1, bracketLength);
        drawBracket(chrome, farEdge, bracketInset, -1, 1, bracketLength);
        drawBracket(chrome, bracketInset, farEdge, 1, -1, bracketLength);
        drawBracket(chrome, farEdge, farEdge, -1, -1, bracketLength);
        float pipRadius = 15f;
        float pipGap = 40f;
        float pipY = 0.905f * WORK_SIZE;
        for (int pip = 0; pip < HULL_SIZES.length; pip++) {
            float pipX = WORK_SIZE / 2f - (HULL_SIZES.length - 1) * pipGap / 2f + pip * pipGap;
            Path2D diamond = new Path2D.Float();
            diamond.moveTo(pipX, pipY - pipRadius);
            diamond.lineTo(pipX + pipRadius, pipY);
            diamond.lineTo(pipX, pipY + pipRadius);
            diamond.lineTo(pipX - pipRadius, pipY);
            diamond.closePath();
            if (pip < sizePips) {
                chrome.setColor(tinted(1f, 0.45f, 245));
                chrome.fill(diamond);
            } else {
                chrome.setColor(tinted(1f, 0f, 110));
                chrome.setStroke(new BasicStroke(4f));
                chrome.draw(diamond);
            }
        }
        chrome.dispose();
    }

    private static Graphics2D antialiased(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return graphics;
    }

    private static void drawBracket(Graphics2D graphics, float cornerX, float cornerY, int directionX, int directionY, float length) {
        Path2D bracket = new Path2D.Float();
        bracket.moveTo(cornerX + directionX * length, cornerY);
        bracket.lineTo(cornerX, cornerY);
        bracket.lineTo(cornerX, cornerY + directionY * length);
        graphics.draw(bracket);
    }

    private Color tinted(float brightness, float whiteMix, int alpha) {
        return new Color(channel(tint[0], brightness, whiteMix), channel(tint[1], brightness, whiteMix), channel(tint[2], brightness, whiteMix), alpha);
    }

    private static int channel(float value, float brightness, float whiteMix) {
        return Math.round(Math.min(1f, lerp(value, 1f, whiteMix) * brightness) * 255f);
    }

    private void addLight(BufferedImage canvas, int x, int y, float intensity) {
        int argb = canvas.getRGB(x, y);
        int pixelAlpha = argb >>> 24;
        float whiten = smoothstep(0.9f, 1.8f, intensity);
        float light = Math.min(intensity, 1.4f);
        float red = Math.min(1f, ((argb >> 16) & 255) / 255f + light * lerp(tint[0], 1f, whiten));
        float green = Math.min(1f, ((argb >> 8) & 255) / 255f + light * lerp(tint[1], 1f, whiten));
        float blue = Math.min(1f, (argb & 255) / 255f + light * lerp(tint[2], 1f, whiten));
        canvas.setRGB(x, y, (pixelAlpha << 24) | (Math.round(red * 255) << 16) | (Math.round(green * 255) << 8) | Math.round(blue * 255));
    }

    private static float[] sobel(float[] source) {
        float[] magnitude = new float[source.length];
        for (int y = 1; y < WORK_SIZE - 1; y++) {
            for (int x = 1; x < WORK_SIZE - 1; x++) {
                float topLeft = source[(y - 1) * WORK_SIZE + x - 1];
                float top = source[(y - 1) * WORK_SIZE + x];
                float topRight = source[(y - 1) * WORK_SIZE + x + 1];
                float left = source[y * WORK_SIZE + x - 1];
                float right = source[y * WORK_SIZE + x + 1];
                float bottomLeft = source[(y + 1) * WORK_SIZE + x - 1];
                float bottom = source[(y + 1) * WORK_SIZE + x];
                float bottomRight = source[(y + 1) * WORK_SIZE + x + 1];
                float horizontal = topRight + 2 * right + bottomRight - topLeft - 2 * left - bottomLeft;
                float vertical = bottomLeft + 2 * bottom + bottomRight - topLeft - 2 * top - topRight;
                magnitude[y * WORK_SIZE + x] = (float) Math.sqrt(horizontal * horizontal + vertical * vertical);
            }
        }
        return magnitude;
    }

    private static float[] blur(float[] source, int radius) {
        float[] horizontalPass = new float[source.length];
        float[] result = new float[source.length];
        for (int y = 0; y < WORK_SIZE; y++) {
            for (int x = 0; x < WORK_SIZE; x++) {
                horizontalPass[y * WORK_SIZE + x] = averageAlong(source, x, y, radius, true);
            }
        }
        for (int y = 0; y < WORK_SIZE; y++) {
            for (int x = 0; x < WORK_SIZE; x++) {
                result[y * WORK_SIZE + x] = averageAlong(horizontalPass, x, y, radius, false);
            }
        }
        return result;
    }

    private static float averageAlong(float[] source, int x, int y, int radius, boolean horizontal) {
        float sum = 0f;
        int count = 0;
        for (int offset = -radius; offset <= radius; offset++) {
            int sampleX = horizontal ? x + offset : x;
            int sampleY = horizontal ? y : y + offset;
            if (sampleX >= 0 && sampleX < WORK_SIZE && sampleY >= 0 && sampleY < WORK_SIZE) {
                sum += source[sampleY * WORK_SIZE + sampleX];
                count++;
            }
        }
        return sum / count;
    }

    private static float[] normalise(float[] values, float percentile) {
        float[] nonZero = new float[values.length];
        int nonZeroCount = 0;
        for (float value : values) {
            if (value > 1e-4f) {
                nonZero[nonZeroCount++] = value;
            }
        }
        Arrays.sort(nonZero, 0, nonZeroCount);
        float reference = nonZeroCount == 0 ? 1f : nonZero[Math.min(nonZeroCount - 1, (int) (nonZeroCount * percentile))];
        float[] result = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = Math.min(1f, values[i] / reference);
        }
        return result;
    }

    private static BufferedImage downsample(BufferedImage source, int factor) {
        int width = source.getWidth() / factor;
        int height = source.getHeight() / factor;
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float alphaSum = 0f;
                float redSum = 0f;
                float greenSum = 0f;
                float blueSum = 0f;
                for (int offsetY = 0; offsetY < factor; offsetY++) {
                    for (int offsetX = 0; offsetX < factor; offsetX++) {
                        int argb = source.getRGB(x * factor + offsetX, y * factor + offsetY);
                        float sampleAlpha = (argb >>> 24) / 255f;
                        alphaSum += sampleAlpha;
                        redSum += ((argb >> 16) & 255) * sampleAlpha;
                        greenSum += ((argb >> 8) & 255) * sampleAlpha;
                        blueSum += (argb & 255) * sampleAlpha;
                    }
                }
                int outputAlpha = Math.round(alphaSum / (factor * factor) * 255f);
                if (alphaSum > 0f) {
                    redSum /= alphaSum;
                    greenSum /= alphaSum;
                    blueSum /= alphaSum;
                }
                result.setRGB(x, y, (outputAlpha << 24) | (Math.round(redSum) << 16) | (Math.round(greenSum) << 8) | Math.round(blueSum));
            }
        }
        return result;
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Math.max(0f, Math.min(1f, (value - edge0) / (edge1 - edge0)));
        return t * t * (3f - 2f * t);
    }

    private static float lerp(float from, float to, float amount) {
        return from + (to - from) * amount;
    }
}
