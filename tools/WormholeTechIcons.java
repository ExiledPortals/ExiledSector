import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WormholeTechIcons {

    private static final String DEFAULT_SOURCE = "graphics/unused/wormhole_tech_item_source.png";
    private static final String DEFAULT_SHEET_DIR = "docs/plans/art_options";
    private static final String CARGO_DIR = "graphics/icons/cargo";
    private static final String TECH_DIR = "graphics/icons/tech";
    private static final List<String> ITEM_FILES = List.of("exiledSector_wormhole_blueprint.png", "exiledSector_gateway_upgrade_advanced.png",
            "exiledSector_gateway_upgrade_perfected.png");
    private static final List<String> TREE_FILES = List.of("exiledSector_wormhole_gateway.png", "exiledSector_wormhole_gateway_advanced.png",
            "exiledSector_wormhole_gateway_perfected.png");
    private static final List<Color> TIER_COLOURS = List.of(new Color(235, 240, 250), new Color(255, 205, 70), new Color(255, 80, 70));
    private static final List<String> TIER_NAMES = List.of("white (blueprint)", "yellow (Advanced)", "red (Perfected)");
    private static final int TREE_SIZE = 320;
    private static final int TREE_VARIANTS = 3;
    private static final Color SHEET_BACKGROUND = new Color(20, 24, 32);
    private static final Color GATE_PURPLE = new Color(176, 76, 255);
    private static final Color GATE_LAVENDER = new Color(222, 180, 255);
    private static final Color VORTEX_CORE = new Color(18, 6, 36);
    private static final float[] VORTEX_RADII = {28f, 38f, 48f};

    private WormholeTechIcons() {
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> options = parseOptions(args);
        BufferedImage source = ImageIO.read(new File(options.getOrDefault("source", DEFAULT_SOURCE)));
        if (source == null) {
            throw new IOException("Not a readable image: " + options.getOrDefault("source", DEFAULT_SOURCE));
        }
        if (options.containsKey("install")) {
            int treeVariant = Integer.parseInt(options.getOrDefault("tree", "1"));
            for (int tier = 0; tier < TIER_COLOURS.size(); tier++) {
                write(itemIcon(source, TIER_COLOURS.get(tier)), new File(CARGO_DIR, ITEM_FILES.get(tier)));
                write(treeIcon(tier, treeVariant), new File(TECH_DIR, TREE_FILES.get(tier)));
            }
            return;
        }
        File sheetDir = new File(options.getOrDefault("out", DEFAULT_SHEET_DIR));
        sheetDir.mkdirs();
        write(itemSheet(source), new File(sheetDir, "wormhole_tech_item_icons.png"));
        write(treeSheet(), new File(sheetDir, "wormhole_tech_tree_icons.png"));
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String name = args[i].substring(2);
            if (name.equals("install")) {
                options.put(name, "true");
            } else {
                options.put(name, args[++i]);
            }
        }
        return options;
    }

    private static void write(BufferedImage image, File file) throws IOException {
        file.getParentFile().mkdirs();
        ImageIO.write(image, "png", file);
        System.out.println("Wrote " + file);
    }

    static BufferedImage itemIcon(BufferedImage source, Color tint) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float[] hsb = new float[3];
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int red = (argb >> 16) & 255;
                int green = (argb >> 8) & 255;
                int blue = argb & 255;
                Color.RGBtoHSB(red, green, blue, hsb);
                float hueDegrees = hsb[0] * 360f;
                float purple = smoothStep(0.15f, 0.35f, hsb[1]) * (smoothStep(250f, 270f, hueDegrees) - smoothStep(340f, 355f, hueDegrees));
                float paleBlue = (1f - smoothStep(0.55f, 0.75f, hsb[1])) * (smoothStep(170f, 190f, hueDegrees) - smoothStep(245f, 260f, hueDegrees));
                float white = Math.max(1f - smoothStep(0.2f, 0.4f, hsb[1]), paleBlue) * (1f - purple);
                float[] recoloured = {red, green, blue};
                int[] tintChannels = {tint.getRed(), tint.getGreen(), tint.getBlue()};
                float darkened = hsb[2] * hsb[2] * 40f;
                float glow = Math.min(1f, hsb[2] * 1.3f);
                float highlight = smoothStep(0.9f, 1f, hsb[2]) * (1f - hsb[1]) * 0.5f;
                for (int channel = 0; channel < 3; channel++) {
                    float tinted = tintChannels[channel] * glow + (255f - tintChannels[channel] * glow) * highlight;
                    recoloured[channel] = recoloured[channel] * (1f - purple - white) + tinted * purple + darkened * white;
                }
                out.setRGB(x, y, argb & 0xff000000 | clamp(Math.round(recoloured[0])) << 16 | clamp(Math.round(recoloured[1])) << 8
                        | clamp(Math.round(recoloured[2])));
            }
        }
        return out;
    }

    private static float smoothStep(float edgeLow, float edgeHigh, float value) {
        float t = Math.max(0f, Math.min(1f, (value - edgeLow) / (edgeHigh - edgeLow)));
        return t * t * (3f - 2f * t);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    static BufferedImage treeIcon(int tier, int variant) {
        BufferedImage image = new BufferedImage(TREE_SIZE, TREE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = smooth(image.createGraphics());
        float centreX = 170f;
        float centreY = 170f;
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(18f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        float ringRadius = 128f;
        graphics.draw(new Arc2D.Float(centreX - ringRadius, centreY - ringRadius, ringRadius * 2f, ringRadius * 2f, 165f, 300f, Arc2D.OPEN));
        drawVortex(graphics, centreX, centreY + 10f, VORTEX_RADII[tier], variant == 2);
        drawPillar(graphics, centreX - 58f, centreY + 78f, 32f, 150f, variant == 2);
        drawPillar(graphics, centreX + 58f, centreY + 78f, 32f, 172f, variant == 2);
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(Math.round(centreX - 88f), Math.round(centreY + 82f), Math.round(centreX + 88f), Math.round(centreY + 82f));
        if (variant == 3 && tier > 0) {
            graphics.setFont(new Font(Font.SERIF, Font.BOLD, 60));
            graphics.drawString(tier == 1 ? "II" : "III", 22, 72);
        }
        graphics.dispose();
        return image;
    }

    private static void drawPillar(Graphics2D graphics, float baseX, float baseY, float baseWidth, float height, boolean outline) {
        GeneralPath pillar = new GeneralPath();
        float half = baseWidth / 2f;
        float topY = baseY - height;
        pillar.moveTo(baseX - half, baseY);
        pillar.lineTo(baseX - half * 0.7f, baseY - height * 0.45f);
        pillar.lineTo(baseX - half * 0.45f, baseY - height * 0.5f);
        pillar.lineTo(baseX - half * 0.35f, baseY - height * 0.85f);
        pillar.lineTo(baseX, topY);
        pillar.lineTo(baseX + half * 0.35f, baseY - height * 0.85f);
        pillar.lineTo(baseX + half * 0.45f, baseY - height * 0.5f);
        pillar.lineTo(baseX + half * 0.7f, baseY - height * 0.45f);
        pillar.lineTo(baseX + half, baseY);
        pillar.closePath();
        graphics.setColor(Color.WHITE);
        if (outline) {
            graphics.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.draw(pillar);
        } else {
            graphics.fill(pillar);
        }
    }

    private static void drawVortex(Graphics2D graphics, float centreX, float centreY, float radius, boolean rings) {
        if (rings) {
            graphics.setStroke(new BasicStroke(radius * 0.16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int ring = 0; ring < 3; ring++) {
                float ringRadius = radius * (1f - ring * 0.3f);
                graphics.setColor(ring == 1 ? GATE_LAVENDER : GATE_PURPLE);
                float start = ring * 70f;
                graphics.draw(new Arc2D.Float(centreX - ringRadius, centreY - ringRadius, ringRadius * 2f, ringRadius * 2f, start, 250f, Arc2D.OPEN));
            }
            graphics.setColor(VORTEX_CORE);
            graphics.fill(new Ellipse2D.Float(centreX - radius * 0.18f, centreY - radius * 0.18f, radius * 0.36f, radius * 0.36f));
            return;
        }
        int arms = 5;
        for (int arm = 0; arm < arms; arm++) {
            GeneralPath path = new GeneralPath();
            double base = arm * Math.PI * 2 / arms;
            int steps = 24;
            for (int step = 0; step <= steps; step++) {
                double t = step / (double) steps;
                double angle = base + t * 2.4;
                double r = radius * (0.15 + 0.85 * t);
                double width = 0.42 * (1 - t);
                float x = (float) (centreX + Math.cos(angle - width) * r);
                float y = (float) (centreY + Math.sin(angle - width) * r);
                if (step == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            for (int step = steps; step >= 0; step--) {
                double t = step / (double) steps;
                double angle = base + t * 2.4;
                double r = radius * (0.15 + 0.85 * t);
                double width = 0.42 * (1 - t);
                path.lineTo((float) (centreX + Math.cos(angle + width) * r * 0.82), (float) (centreY + Math.sin(angle + width) * r * 0.82));
            }
            path.closePath();
            graphics.setColor(arm % 2 == 0 ? GATE_PURPLE : GATE_LAVENDER);
            graphics.fill(path);
        }
        graphics.setColor(VORTEX_CORE);
        graphics.fill(new Ellipse2D.Float(centreX - radius * 0.2f, centreY - radius * 0.2f, radius * 0.4f, radius * 0.4f));
    }

    private static BufferedImage itemSheet(BufferedImage source) {
        int cell = 200;
        BufferedImage sheet = new BufferedImage(cell * (TIER_COLOURS.size() + 1), cell + 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = sheet.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setColor(SHEET_BACKGROUND);
        graphics.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        graphics.drawString("source", 20, 26);
        graphics.drawImage(source, 20, 40, 160, 160, null);
        for (int tier = 0; tier < TIER_COLOURS.size(); tier++) {
            graphics.drawString(TIER_NAMES.get(tier), (tier + 1) * cell + 20, 26);
            graphics.drawImage(itemIcon(source, TIER_COLOURS.get(tier)), (tier + 1) * cell + 20, 40, 160, 160, null);
        }
        graphics.dispose();
        return sheet;
    }

    private static BufferedImage treeSheet() {
        int cell = 240;
        int labelWidth = 170;
        BufferedImage sheet = new BufferedImage(labelWidth + 3 * cell, 40 + TREE_VARIANTS * cell, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = smooth(sheet.createGraphics());
        graphics.setColor(SHEET_BACKGROUND);
        graphics.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        String[] columns = {"Wormhole Gateway", "Gateway Array", "Gateway Nexus"};
        String[] rows = {"1 solid spiral", "2 outline rings", "3 numeral badge"};
        for (int column = 0; column < 3; column++) {
            graphics.drawString(columns[column], labelWidth + column * cell + 40, 26);
        }
        for (int variant = 1; variant <= TREE_VARIANTS; variant++) {
            graphics.drawString(rows[variant - 1], 10, 40 + (variant - 1) * cell + cell / 2);
            for (int tier = 0; tier < 3; tier++) {
                graphics.drawImage(treeIcon(tier, variant), labelWidth + tier * cell + 10, 40 + (variant - 1) * cell + 10, 220, 220, null);
            }
        }
        graphics.dispose();
        return sheet;
    }

    private static Graphics2D smooth(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return graphics;
    }
}
