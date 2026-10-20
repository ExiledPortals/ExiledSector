import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WormholeAnchorIcons {

    private static final String DEFAULT_SOURCE = "E:/Dev/Starsector/starsector-core/graphics/icons/cargo/wormhole_anchor.png";
    private static final String DEFAULT_CSV = "data/config/exiledSector/wormhole_anchors.csv";
    private static final String DEFAULT_OUTPUT_DIR = "graphics/icons/cargo";
    private static final String FILE_PREFIX = "exiledSector_wormhole_anchor_";
    private static final float GLOW_HUE_MIN = 265f;
    private static final float GLOW_HUE_MAX = 345f;
    private static final float GLOW_SATURATION_MIN = 0.2f;
    private static final float GLOW_SATURATION_RAMP = 0.25f;

    private WormholeAnchorIcons() {
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> options = parseOptions(args);
        File sourceFile = new File(options.getOrDefault("source", DEFAULT_SOURCE));
        File csvFile = new File(options.getOrDefault("csv", DEFAULT_CSV));
        File outputDir = new File(options.getOrDefault("out", DEFAULT_OUTPUT_DIR));
        boolean overwrite = options.containsKey("overwrite");
        BufferedImage source = ImageIO.read(sourceFile);
        if (source == null) {
            throw new IOException("Not a readable image: " + sourceFile);
        }
        outputDir.mkdirs();
        for (Map<String, String> row : readCsv(csvFile)) {
            String volume = row.getOrDefault("volume", "");
            String colorText = row.getOrDefault("color", "");
            if (volume.isEmpty() || colorText.isEmpty()) {
                System.out.println("Skipped " + row.getOrDefault("id", "a row") + ": it has no volume or no color");
                continue;
            }
            String iconName = FILE_PREFIX + volume.toLowerCase(Locale.ROOT) + ".png";
            if (!row.getOrDefault("icon", "").endsWith("/" + iconName)) {
                System.out.println("Skipped " + row.getOrDefault("id", "a row") + ": its icon is not " + iconName);
                continue;
            }
            File iconFile = new File(outputDir, iconName);
            if (iconFile.exists() && !overwrite) {
                throw new IOException(iconFile + " already exists; pass --overwrite to replace it");
            }
            ImageIO.write(recolour(source, Color.decode(colorText)), "png", iconFile);
            System.out.println("Wrote " + iconFile + " in " + colorText);
        }
    }

    private static String usage() {
        return "java tools/WormholeAnchorIcons.java [--source <png>] [--csv <file>] [--out <dir>] [--overwrite]\n"
                + "Recolours the purple glow of the vanilla wormhole anchor icon to each anchor's color column.\n"
                + "Defaults: source " + DEFAULT_SOURCE + ", csv " + DEFAULT_CSV + ", output " + DEFAULT_OUTPUT_DIR + ".";
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

    static BufferedImage recolour(BufferedImage source, Color target) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] targetChannels = {target.getRed(), target.getGreen(), target.getBlue()};
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                out.setRGB(x, y, recolourPixel(argb, targetChannels));
            }
        }
        return out;
    }

    private static int recolourPixel(int argb, int[] target) {
        int alpha = argb >>> 24;
        int[] channels = {(argb >> 16) & 255, (argb >> 8) & 255, argb & 255};
        if (alpha == 0) {
            return argb;
        }
        float[] hueSaturation = hueSaturation(channels);
        if (hueSaturation[0] <= GLOW_HUE_MIN || hueSaturation[0] >= GLOW_HUE_MAX || hueSaturation[1] <= GLOW_SATURATION_MIN) {
            return argb;
        }
        float weight = Math.min(1f, (hueSaturation[1] - GLOW_SATURATION_MIN) / GLOW_SATURATION_RAMP);
        float luminance = (channels[0] * 0.3f + channels[1] * 0.59f + channels[2] * 0.11f) / 255f;
        int[] mixed = new int[3];
        for (int channel = 0; channel < 3; channel++) {
            float colourised = luminance <= 0.5f
                    ? target[channel] * luminance * 2f
                    : target[channel] + (255 - target[channel]) * (luminance - 0.5f) * 2f;
            mixed[channel] = Math.round(channels[channel] + (Math.min(255f, colourised) - channels[channel]) * weight);
        }
        return alpha << 24 | mixed[0] << 16 | mixed[1] << 8 | mixed[2];
    }

    private static float[] hueSaturation(int[] channels) {
        float red = channels[0] / 255f;
        float green = channels[1] / 255f;
        float blue = channels[2] / 255f;
        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float delta = max - min;
        if (delta == 0f) {
            return new float[]{0f, 0f};
        }
        float lightness = (max + min) / 2f;
        float saturation = lightness <= 0.5f ? delta / (max + min) : delta / (2f - max - min);
        float hue;
        if (max == red) {
            hue = ((green - blue) / delta) % 6f;
        } else if (max == green) {
            hue = (blue - red) / delta + 2f;
        } else {
            hue = (red - green) / delta + 4f;
        }
        hue *= 60f;
        return new float[]{hue < 0f ? hue + 360f : hue, saturation};
    }

    private static List<Map<String, String>> readCsv(File csvFile) throws IOException {
        List<List<String>> records = parseCsv(Files.readString(csvFile.toPath(), StandardCharsets.UTF_8));
        List<Map<String, String>> rows = new ArrayList<>();
        if (records.isEmpty()) {
            return rows;
        }
        List<String> header = records.get(0);
        for (List<String> record : records.subList(1, records.size())) {
            Map<String, String> row = new LinkedHashMap<>();
            for (int column = 0; column < header.size() && column < record.size(); column++) {
                row.put(header.get(column).trim(), record.get(column).trim());
            }
            if (!row.getOrDefault("id", "").isEmpty() && !row.get("id").startsWith("#")) {
                rows.add(row);
            }
        }
        return rows;
    }

    private static List<List<String>> parseCsv(String text) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (quoted) {
                if (character == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (character == '"') {
                    quoted = false;
                } else {
                    cell.append(character);
                }
            } else if (character == '"') {
                quoted = true;
            } else if (character == ',') {
                record.add(cell.toString());
                cell.setLength(0);
            } else if (character == '\n') {
                record.add(cell.toString());
                cell.setLength(0);
                records.add(record);
                record = new ArrayList<>();
            } else if (character != '\r') {
                cell.append(character);
            }
        }
        if (cell.length() > 0 || !record.isEmpty()) {
            record.add(cell.toString());
            records.add(record);
        }
        return records;
    }
}
