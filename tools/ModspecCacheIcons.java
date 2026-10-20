import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModspecCacheIcons {

    private static final String DEFAULT_GAME_CORE = "E:/Dev/Starsector/starsector-core";
    private static final String ROCK_ICON = "graphics/asteroids/asteroid4.png";
    private static final String MODSPEC_ICON = "graphics/icons/cargo/modspec.png";
    private static final String DEFAULT_OUTPUT = "graphics/icons/socketables/exiledSector_modspec_cache.png";
    private static final Color DEFAULT_COLOR = new Color(255, 75, 70);
    private static final Color ROCK_TINT = new Color(150, 142, 140);
    private static final int DESIGN_SIZE = 80;
    private static final int SUPERSAMPLE = 4;
    private static final float[][] SEAM = {{0.53f, 0f}, {0.49f, 0.1f}, {0.56f, 0.22f}, {0.51f, 0.31f}, {0.47f, 0.45f}, {0.55f, 0.57f}, {0.5f, 0.66f},
            {0.46f, 0.8f}, {0.51f, 0.9f}, {0.49f, 1f}};
    private static final float ROCK_WIDTH = 50f;
    private static final float ROCK_HEIGHT = ROCK_WIDTH * 83f / 88f;
    private static final float ROCK_CENTRE_Y = -10f;
    private static final float HALF_TILT_DEGREES = 9f;
    private static final float HALF_SLIDE = 2.2f;
    private static final float STILL_OPEN = 0.85f;
    private static final float MOUTH_V = 0.12f;
    private static final float CHIP_X = 0f;
    private static final float CHIP_Y = 22f;
    private static final float CHIP_RADIUS = 9f;
    private static final int CHIP_SCANLINES = 5;

    private ModspecCacheIcons() {
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> options = parseOptions(args);
        File gameCore = new File(options.getOrDefault("game", DEFAULT_GAME_CORE));
        File output = new File(options.getOrDefault("out", DEFAULT_OUTPUT));
        Color color = options.containsKey("color") ? Color.decode(options.get("color")) : DEFAULT_COLOR;
        BufferedImage rock = read(new File(gameCore, ROCK_ICON));
        BufferedImage modspec = read(new File(gameCore, MODSPEC_ICON));
        if (output.exists() && !options.containsKey("overwrite")) {
            throw new IOException(output + " already exists; pass --overwrite to replace it");
        }
        ImageIO.write(still(rock, modspec, color), "png", output);
        System.out.println("Wrote " + output);
    }

    private static String usage() {
        return "java tools/ModspecCacheIcons.java [--game <starsector-core>] [--out <png>] [--color #rrggbb] [--overwrite]\n"
                + "Bakes the Contraband Modspec Cache still: vanilla asteroid4.png split open along the seam CacheIconFx uses,\n"
                + "lit from inside, projecting a vanilla modspec hologram. Defaults: game " + DEFAULT_GAME_CORE + ", output " + DEFAULT_OUTPUT
                + ", colour the corruption red.";
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

    private static BufferedImage read(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Not a readable image: " + file);
        }
        return image;
    }

    static BufferedImage still(BufferedImage rock, BufferedImage modspec, Color color) {
        int size = DESIGN_SIZE * SUPERSAMPLE;
        BufferedImage canvas = layer(size);
        Graphics2D g = graphics(canvas);
        Color hot = lighten(color, 0.8f);
        Color light = lighten(color, 0.45f);
        Point2D.Float mouth = point(0f, rockY(MOUTH_V));
        Point2D.Float chip = point(CHIP_X, CHIP_Y);
        float chipRadius = CHIP_RADIUS * SUPERSAMPLE;
        glow(g, point(0f, ROCK_CENTRE_Y + 4f), 30f * SUPERSAMPLE, color, 110);
        Path2D.Float cone = new Path2D.Float();
        cone.moveTo(mouth.x, mouth.y);
        cone.lineTo(chip.x - chipRadius, chip.y);
        cone.lineTo(chip.x + chipRadius, chip.y);
        cone.closePath();
        g.setPaint(new GradientPaint(mouth, withAlpha(light, 90), chip, withAlpha(light, 10)));
        g.fill(cone);
        BufferedImage scaled = scaledRock(rock);
        g.drawImage(gap(scaled, color, hot, size), 0, 0, null);
        BufferedImage tinted = tint(scaled, ROCK_TINT);
        for (int side = -1; side <= 1; side += 2) {
            g.drawImage(half(tinted, side, color, hot, size), 0, 0, null);
        }
        drawChip(g, modspec, chip, chipRadius, color, light);
        g.dispose();
        return downscale(canvas);
    }

    private static BufferedImage gap(BufferedImage scaled, Color color, Color hot, int size) {
        BufferedImage mask = layer(size);
        Graphics2D maskGraphics = graphics(mask);
        maskGraphics.drawImage(scaled, rockTransform(), null);
        maskGraphics.dispose();
        BufferedImage gap = layer(size);
        Graphics2D g = graphics(gap);
        Path2D.Float outer = new Path2D.Float();
        Path2D.Float inner = new Path2D.Float();
        float[][] left = transformedSeam(-1);
        float[][] right = transformedSeam(1);
        for (int i = 0; i < SEAM.length; i++) {
            float midX = (left[i][0] + right[i][0]) / 2f;
            float midY = (left[i][1] + right[i][1]) / 2f;
            lineOrMove(outer, left[i][0], left[i][1], i);
            lineOrMove(inner, midX + (left[i][0] - midX) * 0.55f, midY + (left[i][1] - midY) * 0.55f, i);
        }
        for (int i = SEAM.length - 1; i >= 0; i--) {
            float midX = (left[i][0] + right[i][0]) / 2f;
            float midY = (left[i][1] + right[i][1]) / 2f;
            outer.lineTo(right[i][0], right[i][1]);
            inner.lineTo(midX + (right[i][0] - midX) * 0.55f, midY + (right[i][1] - midY) * 0.55f);
        }
        outer.closePath();
        inner.closePath();
        g.setColor(color);
        g.fill(outer);
        g.setColor(hot);
        g.fill(inner);
        g.setComposite(AlphaComposite.DstIn);
        g.drawImage(mask, 0, 0, null);
        g.dispose();
        return gap;
    }

    private static BufferedImage half(BufferedImage tinted, int side, Color color, Color hot, int size) {
        BufferedImage half = layer(size);
        Graphics2D g = graphics(half);
        AffineTransform open = openTransform(side);
        Path2D.Float shape = new Path2D.Float();
        float outerU = side < 0 ? 0f : 1f;
        for (int i = 0; i < SEAM.length; i++) {
            Point2D.Float edge = point(rockX(outerU), rockY(SEAM[i][1]));
            lineOrMove(shape, edge.x, edge.y, i);
        }
        for (int i = SEAM.length - 1; i >= 0; i--) {
            Point2D.Float seam = point(rockX(SEAM[i][0]), rockY(SEAM[i][1]));
            shape.lineTo(seam.x, seam.y);
        }
        shape.closePath();
        g.setClip(open.createTransformedShape(shape));
        AffineTransform placed = new AffineTransform(open);
        placed.concatenate(rockTransform());
        g.drawImage(tinted, placed, null);
        g.setComposite(AlphaComposite.SrcAtop);
        Path2D.Float seamLine = new Path2D.Float();
        float[][] seam = transformedSeam(side);
        for (int i = 0; i < seam.length; i++) {
            lineOrMove(seamLine, seam[i][0], seam[i][1], i);
        }
        g.setStroke(new BasicStroke(4f * SUPERSAMPLE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(color, 80));
        g.draw(seamLine);
        g.setStroke(new BasicStroke(0.9f * SUPERSAMPLE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(hot, 150));
        g.draw(seamLine);
        g.dispose();
        return half;
    }

    private static void drawChip(Graphics2D g, BufferedImage modspec, Point2D.Float chip, float radius, Color color, Color light) {
        glow(g, chip, radius * 1.7f, color, 120);
        Ellipse2D.Float disc = new Ellipse2D.Float(chip.x - radius, chip.y - radius, radius * 2f, radius * 2f);
        BufferedImage hologram = monochrome(modspec, light);
        Graphics2D chipGraphics = (Graphics2D) g.create();
        chipGraphics.setClip(disc);
        chipGraphics.setColor(withAlpha(color, 70));
        chipGraphics.fill(disc);
        float iconSize = radius * 1.55f;
        chipGraphics.drawImage(hologram, Math.round(chip.x - iconSize / 2f), Math.round(chip.y - iconSize / 2f), Math.round(iconSize),
                Math.round(iconSize), null);
        chipGraphics.setColor(withAlpha(lighten(light, 0.4f), 60));
        chipGraphics.setStroke(new BasicStroke(SUPERSAMPLE * 0.6f));
        for (int line = 0; line < CHIP_SCANLINES; line++) {
            float lineY = chip.y - radius + (line + 0.5f) * 2f * radius / CHIP_SCANLINES;
            chipGraphics.draw(new Line2D.Float(chip.x - radius, lineY, chip.x + radius, lineY));
        }
        chipGraphics.dispose();
        g.setStroke(new BasicStroke(SUPERSAMPLE * 1.1f));
        g.setColor(withAlpha(light, 230));
        g.draw(disc);
    }

    private static void glow(Graphics2D g, Point2D.Float centre, float radius, Color color, int alpha) {
        g.setPaint(new RadialGradientPaint(centre, radius, new float[]{0f, 1f}, new Color[]{withAlpha(color, alpha), withAlpha(color, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fill(new Ellipse2D.Float(centre.x - radius, centre.y - radius, radius * 2f, radius * 2f));
    }

    private static AffineTransform rockTransform() {
        AffineTransform transform = new AffineTransform();
        Point2D.Float topLeft = point(rockX(0f), rockY(0f));
        transform.translate(topLeft.x, topLeft.y);
        return transform;
    }

    private static AffineTransform openTransform(int side) {
        Point2D.Float hinge = point(rockX(SEAM[SEAM.length - 1][0]), rockY(1f));
        AffineTransform transform = new AffineTransform();
        transform.translate(side * HALF_SLIDE * STILL_OPEN * SUPERSAMPLE, 0);
        transform.rotate(Math.toRadians(side * HALF_TILT_DEGREES * STILL_OPEN), hinge.x, hinge.y);
        return transform;
    }

    private static float[][] transformedSeam(int side) {
        AffineTransform open = openTransform(side);
        float[][] points = new float[SEAM.length][2];
        for (int i = 0; i < SEAM.length; i++) {
            Point2D.Float seam = point(rockX(SEAM[i][0]), rockY(SEAM[i][1]));
            Point2D placed = open.transform(seam, null);
            points[i][0] = (float) placed.getX();
            points[i][1] = (float) placed.getY();
        }
        return points;
    }

    private static void lineOrMove(Path2D.Float path, float x, float y, int index) {
        if (index == 0) {
            path.moveTo(x, y);
        } else {
            path.lineTo(x, y);
        }
    }

    private static float rockX(float u) {
        return (u - 0.5f) * ROCK_WIDTH;
    }

    private static float rockY(float v) {
        return ROCK_CENTRE_Y + (0.5f - v) * ROCK_HEIGHT;
    }

    private static Point2D.Float point(float localX, float localY) {
        return new Point2D.Float((localX + DESIGN_SIZE / 2f) * SUPERSAMPLE, (DESIGN_SIZE / 2f - localY) * SUPERSAMPLE);
    }

    private static BufferedImage tint(BufferedImage source, Color tint) {
        BufferedImage scaled = layer(source.getWidth(), source.getHeight());
        Graphics2D g = scaled.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        for (int y = 0; y < scaled.getHeight(); y++) {
            for (int x = 0; x < scaled.getWidth(); x++) {
                int argb = scaled.getRGB(x, y);
                int red = ((argb >> 16) & 255) * tint.getRed() / 255;
                int green = ((argb >> 8) & 255) * tint.getGreen() / 255;
                int blue = (argb & 255) * tint.getBlue() / 255;
                scaled.setRGB(x, y, (argb & 0xFF000000) | red << 16 | green << 8 | blue);
            }
        }
        return scaled;
    }

    private static BufferedImage scaledRock(BufferedImage source) {
        int width = Math.round(ROCK_WIDTH * SUPERSAMPLE);
        int height = Math.round(ROCK_HEIGHT * SUPERSAMPLE);
        BufferedImage scaled = layer(width, height);
        Graphics2D g = graphics(scaled);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    private static BufferedImage monochrome(BufferedImage source, Color light) {
        BufferedImage out = layer(source.getWidth(), source.getHeight());
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                float luminance = (((argb >> 16) & 255) * 0.3f + ((argb >> 8) & 255) * 0.59f + (argb & 255) * 0.11f) / 255f;
                float level = Math.min(1f, 0.35f + luminance * 1.1f);
                int red = Math.round(light.getRed() * level);
                int green = Math.round(light.getGreen() * level);
                int blue = Math.round(light.getBlue() * level);
                int alpha = Math.round((argb >>> 24) * 0.85f);
                out.setRGB(x, y, alpha << 24 | red << 16 | green << 8 | blue);
            }
        }
        return out;
    }

    private static BufferedImage downscale(BufferedImage canvas) {
        Image smooth = canvas.getScaledInstance(DESIGN_SIZE, DESIGN_SIZE, Image.SCALE_AREA_AVERAGING);
        BufferedImage out = layer(DESIGN_SIZE);
        Graphics2D g = out.createGraphics();
        g.drawImage(smooth, 0, 0, null);
        g.dispose();
        return out;
    }

    private static BufferedImage layer(int size) {
        return layer(size, size);
    }

    private static BufferedImage layer(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static Color lighten(Color color, float amount) {
        return new Color(Math.round(color.getRed() + (255 - color.getRed()) * amount), Math.round(color.getGreen() + (255 - color.getGreen()) * amount),
                Math.round(color.getBlue() + (255 - color.getBlue()) * amount));
    }
}
