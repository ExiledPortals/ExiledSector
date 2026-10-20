import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DoctrineArchiveIcons {

    private static final String DEFAULT_GAME_CORE = "E:/Dev/Starsector/starsector-core";
    private static final String SOURCE_ICON = "graphics/icons/cargo/hyperspace_data.png";
    private static final String DEFAULT_CSV = "data/config/exiledSector/doctrine_archives.csv";
    private static final String DEFAULT_OUTPUT_DIR = "graphics/icons/socketables";
    private static final String FRAME_NAME = "exiledSector_doctrine_archive_frame.png";
    private static final String STILL_PREFIX = "exiledSector_doctrine_archive_";
    private static final String EMBLEM_PREFIX = "exiledSector_doctrine_emblem_";
    private static final int EMBLEM_SIZE = 64;
    private static final int CORNER_SAMPLE = 6;
    private static final float KEY_FROM = 0.14f;
    private static final float KEY_TO = 0.32f;
    private static final float EMBLEM_FLOOR = 0.12f;
    private static final float BRIGHTEST_PERCENTILE = 0.95f;
    private static final float EMBLEM_LIFT = 0.45f;
    private static final Pattern COMMENT_LINE = Pattern.compile("(?m)^\\s*#.*$");
    static final float[][] SCREEN_CORNERS = {{13.5f, 17.5f}, {63.8f, 17.5f}, {70.3f, 55.8f}, {19f, 59.6f}};
    private static final int SCREEN_FIRST_ROW = 17;
    private static final int[] SCREEN_LEFT = {13, 13, 13, 13, 13, 13, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 15, 15, 15, 15, 15, 15, 15,
            16, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 18, 18, 18, 18, 18, 18, 18, 18};
    private static final int[] SCREEN_RIGHT = {64, 64, 64, 64, 64, 65, 65, 65, 65, 65, 66, 66, 66, 66, 67, 67, 67, 67, 67, 67, 68, 68, 68,
            68, 68, 69, 69, 69, 69, 69, 69, 70, 70, 70, 70, 70, 71, 71, 71, 60, 49, 33, 22};
    private static final float DESIGN_SIZE = 80f;
    private static final Color SCREEN_FILL = new Color(10, 12, 17);
    private static final float FRAME_CONTRAST = 1.12f;
    private static final float FRAME_TINT = 0.4f;
    private static final int ROWS = 9;
    private static final float FIRST_ROW = 0.24f;
    private static final float ROW_PITCH = 0.085f;
    private static final float ROW_THICKNESS = 0.032f;
    private static final float CREST_CENTRE_U = 0.5f;
    private static final float CREST_CENTRE_V = 0.56f;
    private static final float CREST_HEIGHT = 0.6f;
    private static final Pattern COLOR_ARRAY = Pattern.compile("\"%s\"\\s*:\\s*\\[\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)");
    private static final Pattern CREST = Pattern.compile("\"crest\"\\s*:\\s*\"([^\"]+)\"");

    private DoctrineArchiveIcons() {
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> options = parseOptions(args);
        File gameCore = new File(options.getOrDefault("game", DEFAULT_GAME_CORE));
        File csvFile = new File(options.getOrDefault("csv", DEFAULT_CSV));
        File outputDir = new File(options.getOrDefault("out", DEFAULT_OUTPUT_DIR));
        boolean overwrite = options.containsKey("overwrite");
        BufferedImage source = ImageIO.read(new File(gameCore, SOURCE_ICON));
        if (source == null) {
            throw new IOException("Not a readable image: " + new File(gameCore, SOURCE_ICON));
        }
        outputDir.mkdirs();
        BufferedImage frame = neutralFrame(source);
        write(frame, new File(outputDir, FRAME_NAME), overwrite);
        for (Map<String, String> row : readCsv(csvFile)) {
            String id = row.get("id");
            String factionId = row.getOrDefault("faction", "");
            String factionText = factionFileText(new File(gameCore, "data/world/factions"), factionId);
            if (factionText == null) {
                System.out.println("Skipped " + id + ": no vanilla faction file for \"" + factionId + "\"");
                continue;
            }
            Color color = factionColor(factionText);
            Matcher crestMatch = CREST.matcher(factionText);
            BufferedImage crest = crestMatch.find() ? ImageIO.read(new File(gameCore, crestMatch.group(1))) : null;
            if (color == null || crest == null) {
                System.out.println("Skipped " + id + ": " + factionId + " has no colour or crest");
                continue;
            }
            BufferedImage emblem = emblem(crest);
            write(emblem, new File(outputDir, EMBLEM_PREFIX + factionId + ".png"), overwrite);
            String stillName = STILL_PREFIX + id.substring("doctrine_archive_".length()) + ".png";
            write(still(frame, emblem, color, id.hashCode()), new File(outputDir, stillName), overwrite);
        }
    }

    private static String usage() {
        return "java tools/DoctrineArchiveIcons.java [--game <starsector-core>] [--csv <file>] [--out <dir>] [--overwrite]\n"
                + "Bakes the neutral doctrine archive frame from vanilla hyperspace_data.png, plus one still per doctrine_archives.csv row\n"
                + "tinted to its faction's UI colour with its crest on the screen. Defaults: game " + DEFAULT_GAME_CORE + ", csv " + DEFAULT_CSV
                + ", output " + DEFAULT_OUTPUT_DIR + ".";
    }

    private static void write(BufferedImage image, File file, boolean overwrite) throws IOException {
        if (file.exists() && !overwrite) {
            throw new IOException(file + " already exists; pass --overwrite to replace it");
        }
        ImageIO.write(image, "png", file);
        System.out.println("Wrote " + file);
    }

    private static String factionFileText(File factionsDir, String factionId) throws IOException {
        File[] files = factionsDir.listFiles((dir, name) -> name.endsWith(".faction"));
        if (factionId.isEmpty() || files == null) {
            return null;
        }
        Pattern idPattern = Pattern.compile("\"?id\"?\\s*:\\s*\"" + Pattern.quote(factionId) + "\"");
        for (File file : files) {
            String text = COMMENT_LINE.matcher(Files.readString(file.toPath(), StandardCharsets.UTF_8)).replaceAll("");
            if (idPattern.matcher(text).find()) {
                return text;
            }
        }
        return null;
    }

    static Color factionColor(String factionText) {
        for (String key : new String[]{"baseUIColor", "color"}) {
            Matcher match = Pattern.compile(String.format(COLOR_ARRAY.pattern(), key)).matcher(factionText);
            if (match.find()) {
                return new Color(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2)), Integer.parseInt(match.group(3)));
            }
        }
        return null;
    }

    static BufferedImage neutralFrame(BufferedImage source) {
        int size = source.getWidth();
        BufferedImage out = new BufferedImage(size, source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < size; x++) {
                int argb = source.getRGB(x, y);
                int alpha = argb >>> 24;
                float luminance = (((argb >> 16) & 255) * 0.3f + ((argb >> 8) & 255) * 0.59f + (argb & 255) * 0.11f) / 255f;
                int grey = Math.round(Math.max(0f, Math.min(1f, (luminance - 0.5f) * FRAME_CONTRAST + 0.5f)) * 255f);
                out.setRGB(x, y, alpha << 24 | grey << 16 | grey << 8 | grey);
            }
        }
        Graphics2D graphics = out.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(SCREEN_FILL);
        graphics.fill(screenOutline(size / DESIGN_SIZE));
        graphics.dispose();
        return out;
    }

    private static Path2D screenOutline(float scale) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(SCREEN_LEFT[0] * scale, SCREEN_FIRST_ROW * scale);
        for (int row = 0; row < SCREEN_RIGHT.length; row++) {
            path.lineTo(SCREEN_RIGHT[row] * scale, (SCREEN_FIRST_ROW + row) * scale);
            path.lineTo(SCREEN_RIGHT[row] * scale, (SCREEN_FIRST_ROW + row + 1) * scale);
        }
        for (int row = SCREEN_LEFT.length - 1; row >= 0; row--) {
            path.lineTo(SCREEN_LEFT[row] * scale, (SCREEN_FIRST_ROW + row + 1) * scale);
            path.lineTo(SCREEN_LEFT[row] * scale, (SCREEN_FIRST_ROW + row) * scale);
        }
        path.closePath();
        return path;
    }

    static BufferedImage emblem(BufferedImage crest) {
        float[] background = cornerColour(crest);
        BufferedImage out = new BufferedImage(EMBLEM_SIZE, EMBLEM_SIZE, BufferedImage.TYPE_INT_ARGB);
        float step = (float) crest.getWidth() / EMBLEM_SIZE;
        float[] keys = new float[EMBLEM_SIZE * EMBLEM_SIZE];
        float[] luminances = new float[keys.length];
        List<Float> opaqueLuminances = new ArrayList<>();
        for (int y = 0; y < EMBLEM_SIZE; y++) {
            for (int x = 0; x < EMBLEM_SIZE; x++) {
                float[] pixel = averageColour(crest, Math.round(x * step), Math.round(y * step), Math.max(1, Math.round(step)));
                float distance = (float) Math.sqrt(square(pixel[0] - background[0]) + square(pixel[1] - background[1])
                        + square(pixel[2] - background[2])) / (float) Math.sqrt(3.0);
                float keyed = Math.max(0f, Math.min(1f, (distance - KEY_FROM) / (KEY_TO - KEY_FROM)));
                int index = y * EMBLEM_SIZE + x;
                keys[index] = keyed * keyed * (3f - 2f * keyed);
                luminances[index] = pixel[0] * 0.3f + pixel[1] * 0.59f + pixel[2] * 0.11f;
                if (keys[index] > 0.5f) {
                    opaqueLuminances.add(luminances[index]);
                }
            }
        }
        opaqueLuminances.sort(null);
        float brightest = opaqueLuminances.isEmpty() ? 1f
                : Math.max(0.05f, opaqueLuminances.get(Math.round((opaqueLuminances.size() - 1) * BRIGHTEST_PERCENTILE)));
        for (int index = 0; index < keys.length; index++) {
            float normalised = Math.min(1f, luminances[index] / brightest);
            int level = Math.round((EMBLEM_FLOOR + (1f - EMBLEM_FLOOR) * normalised) * 255f);
            int alpha = Math.round(keys[index] * 255f);
            out.setRGB(index % EMBLEM_SIZE, index / EMBLEM_SIZE, alpha << 24 | level << 16 | level << 8 | level);
        }
        return out;
    }

    private static float[] cornerColour(BufferedImage image) {
        float[] sum = new float[3];
        int[][] corners = {{0, 0}, {image.getWidth() - CORNER_SAMPLE, 0}, {0, image.getHeight() - CORNER_SAMPLE},
                {image.getWidth() - CORNER_SAMPLE, image.getHeight() - CORNER_SAMPLE}};
        for (int[] corner : corners) {
            float[] colour = averageColour(image, corner[0], corner[1], CORNER_SAMPLE);
            for (int channel = 0; channel < 3; channel++) {
                sum[channel] += colour[channel] / corners.length;
            }
        }
        return sum;
    }

    private static float[] averageColour(BufferedImage image, int left, int top, int span) {
        float[] sum = new float[3];
        int count = 0;
        for (int y = top; y < top + span && y < image.getHeight(); y++) {
            for (int x = left; x < left + span && x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                sum[0] += ((argb >> 16) & 255) / 255f;
                sum[1] += ((argb >> 8) & 255) / 255f;
                sum[2] += (argb & 255) / 255f;
                count++;
            }
        }
        for (int channel = 0; channel < 3; channel++) {
            sum[channel] /= Math.max(1, count);
        }
        return sum;
    }

    private static float square(float value) {
        return value * value;
    }

    static BufferedImage still(BufferedImage frame, BufferedImage emblem, Color color, int seed) {
        int size = frame.getWidth();
        float scale = size / DESIGN_SIZE;
        BufferedImage out = new BufferedImage(size, frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float tintRed = 1f + (color.getRed() / 255f - 1f) * FRAME_TINT;
        float tintGreen = 1f + (color.getGreen() / 255f - 1f) * FRAME_TINT;
        float tintBlue = 1f + (color.getBlue() / 255f - 1f) * FRAME_TINT;
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < size; x++) {
                int argb = frame.getRGB(x, y);
                int red = Math.round(((argb >> 16) & 255) * tintRed);
                int green = Math.round(((argb >> 8) & 255) * tintGreen);
                int blue = Math.round((argb & 255) * tintBlue);
                out.setRGB(x, y, (argb & 0xFF000000) | red << 16 | green << 8 | blue);
            }
        }
        Graphics2D graphics = out.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Random random = new Random(seed);
        graphics.setColor(withAlpha(color, 0.85f));
        float glyphU = 0.05f;
        while (glyphU < 0.5f) {
            float glyphWidth = 0.03f + random.nextFloat() * 0.05f;
            graphics.fill(screenQuad(glyphU, 0.075f, glyphU + glyphWidth, 0.125f, scale));
            glyphU += glyphWidth + 0.02f;
        }
        graphics.fill(screenQuad(0.88f, 0.07f, 0.93f, 0.13f, scale));
        graphics.setColor(withAlpha(color, 0.5f));
        for (int row = 0; row < ROWS; row++) {
            float rowCentre = FIRST_ROW + row * ROW_PITCH;
            float wordU = 0.05f;
            float rowEnd = 0.6f + random.nextFloat() * 0.35f;
            while (wordU < rowEnd) {
                float wordWidth = 0.04f + random.nextFloat() * 0.12f;
                graphics.fill(screenQuad(wordU, rowCentre - ROW_THICKNESS / 2f, Math.min(rowEnd, wordU + wordWidth), rowCentre + ROW_THICKNESS / 2f,
                        scale));
                wordU += wordWidth + 0.025f;
            }
        }
        graphics.dispose();
        drawEmblem(out, emblem, color, scale);
        return out;
    }

    private static void drawEmblem(BufferedImage out, BufferedImage crest, Color color, float scale) {
        float[] tint = {lift(color.getRed()), lift(color.getGreen()), lift(color.getBlue())};
        float screenHeight = (SCREEN_CORNERS[3][1] + SCREEN_CORNERS[2][1] - SCREEN_CORNERS[0][1] - SCREEN_CORNERS[1][1]) / 2f;
        float screenWidth = (SCREEN_CORNERS[1][0] + SCREEN_CORNERS[2][0] - SCREEN_CORNERS[0][0] - SCREEN_CORNERS[3][0]) / 2f;
        float halfV = CREST_HEIGHT / 2f;
        float halfU = halfV * screenHeight / screenWidth * crest.getWidth() / crest.getHeight();
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                float[] uv = inverse((x + 0.5f) / scale, (y + 0.5f) / scale);
                float crestX = (uv[0] - (CREST_CENTRE_U - halfU)) / (2f * halfU);
                float crestY = (uv[1] - (CREST_CENTRE_V - halfV)) / (2f * halfV);
                float shade = 1f - Math.min(1f, (float) Math.hypot(crestX - 0.5f, crestY - 0.5f) / 0.7f);
                if (shade > 0f) {
                    out.setRGB(x, y, darken(out.getRGB(x, y), shade * 0.75f));
                }
                if (crestX < 0f || crestX >= 1f || crestY < 0f || crestY >= 1f) {
                    continue;
                }
                int sample = sample(crest, crestX, crestY);
                float level = (sample & 255) / 255f;
                int light = Math.round(tint[0] * level) << 16 | Math.round(tint[1] * level) << 8 | Math.round(tint[2] * level);
                out.setRGB(x, y, addLight(out.getRGB(x, y), light));
            }
        }
    }

    private static float lift(int channel) {
        return channel + (255 - channel) * EMBLEM_LIFT;
    }

    private static int sample(BufferedImage image, float u, float v) {
        float sourceX = u * image.getWidth() - 0.5f;
        float sourceY = v * image.getHeight() - 0.5f;
        float sum = 0f;
        int span = Math.max(1, image.getWidth() / 24);
        int count = 0;
        for (int dy = -span; dy <= span; dy++) {
            for (int dx = -span; dx <= span; dx++) {
                int px = Math.max(0, Math.min(image.getWidth() - 1, Math.round(sourceX) + dx));
                int py = Math.max(0, Math.min(image.getHeight() - 1, Math.round(sourceY) + dy));
                int argb = image.getRGB(px, py);
                sum += (argb >>> 24) / 255f * (argb & 255);
                count++;
            }
        }
        return Math.round(sum / count);
    }

    private static int addLight(int argb, int light) {
        int red = Math.min(255, ((argb >> 16) & 255) + ((light >> 16) & 255));
        int green = Math.min(255, ((argb >> 8) & 255) + ((light >> 8) & 255));
        int blue = Math.min(255, (argb & 255) + (light & 255));
        return (argb & 0xFF000000) | red << 16 | green << 8 | blue;
    }

    private static int darken(int argb, float amount) {
        float keep = 1f - amount;
        int red = Math.round(((argb >> 16) & 255) * keep);
        int green = Math.round(((argb >> 8) & 255) * keep);
        int blue = Math.round((argb & 255) * keep);
        return (argb & 0xFF000000) | red << 16 | green << 8 | blue;
    }

    private static Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(alpha * 255f));
    }

    static float[] screenPoint(float u, float v) {
        float topX = SCREEN_CORNERS[0][0] + (SCREEN_CORNERS[1][0] - SCREEN_CORNERS[0][0]) * u;
        float topY = SCREEN_CORNERS[0][1] + (SCREEN_CORNERS[1][1] - SCREEN_CORNERS[0][1]) * u;
        float bottomX = SCREEN_CORNERS[3][0] + (SCREEN_CORNERS[2][0] - SCREEN_CORNERS[3][0]) * u;
        float bottomY = SCREEN_CORNERS[3][1] + (SCREEN_CORNERS[2][1] - SCREEN_CORNERS[3][1]) * u;
        return new float[]{topX + (bottomX - topX) * v, topY + (bottomY - topY) * v};
    }

    private static float[] inverse(float x, float y) {
        float u = 0.5f;
        float v = 0.5f;
        for (int step = 0; step < 8; step++) {
            float[] point = screenPoint(u, v);
            float[] alongU = screenPoint(u + 0.001f, v);
            float[] alongV = screenPoint(u, v + 0.001f);
            float a = (alongU[0] - point[0]) / 0.001f;
            float b = (alongV[0] - point[0]) / 0.001f;
            float c = (alongU[1] - point[1]) / 0.001f;
            float d = (alongV[1] - point[1]) / 0.001f;
            float determinant = a * d - b * c;
            float errorX = x - point[0];
            float errorY = y - point[1];
            u += (d * errorX - b * errorY) / determinant;
            v += (a * errorY - c * errorX) / determinant;
        }
        return new float[]{u, v};
    }

    private static Path2D screenQuad(float fromU, float fromV, float toU, float toV, float scale) {
        float[][] corners = {screenPoint(fromU, fromV), screenPoint(toU, fromV), screenPoint(toU, toV), screenPoint(fromU, toV)};
        Path2D.Float path = new Path2D.Float();
        path.moveTo(corners[0][0] * scale, corners[0][1] * scale);
        for (int corner = 1; corner < 4; corner++) {
            path.lineTo(corners[corner][0] * scale, corners[corner][1] * scale);
        }
        path.closePath();
        return path;
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
